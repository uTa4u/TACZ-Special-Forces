package su.uTa4u.specialforces.capabilities.observation;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.registries.ForgeRegistries;
import su.uTa4u.specialforces.Mission;
import su.uTa4u.specialforces.Specialty;
import su.uTa4u.specialforces.config.CommonConfig;
import su.uTa4u.specialforces.entities.SwatEntity;

import java.util.*;

public class Observation implements IObservation {
    private static final Set<Block> OBSERVATION_BLOCK_TARGETS = new HashSet<>();
    private static final Set<EntityType<? extends Entity>> OBSERVATION_ENTITY_TARGETS = new HashSet<>();

    private Map<Block, List<BlockPos>> observedBlocks = new HashMap<>();
    private Map<EntityType<? extends Entity>, List<UUID>> observedEntities = new HashMap<>();
    private int lastTick = 0;
    private List<UUID> commanders = new ArrayList<>();
    private Mission swatMission = null;

    Observation() {
    }

    @Override
    public void observe(Block block, BlockPos pos) {
        if (this.swatMission == Mission.SCOUTING &&
                !this.observedBlocks.computeIfAbsent(block, k -> new ArrayList<>()).contains(pos)) {
            this.observedBlocks.get(block).add(pos);
        }
    }

    @Override
    public void observe(EntityType<? extends Entity> entityType, UUID uuid) {
        if (this.swatMission == Mission.SCOUTING &&
                !this.observedEntities.computeIfAbsent(entityType, k -> new ArrayList<>()).contains(uuid)) {
            this.observedEntities.get(entityType).add(uuid);
        }
    }

    @Override
    public int getLastTick() {
        return this.lastTick;
    }

    @Override
    public List<UUID> getCommanders() {
        return this.commanders;
    }

    @Override
    public Map<Block, List<BlockPos>> getObservedBlocks() {
        return this.observedBlocks;
    }

    @Override
    public Map<EntityType<? extends Entity>, List<UUID>> getObservedEntities() {
        return this.observedEntities;
    }

    @Override
    public void tick(Player player) {
        // Remove invalid observed entries.
        // They might appear after observation targets were changed in config.
        if (player.tickCount % 20 == 0) {
            for (Block block : this.observedBlocks.keySet()) {
                if (!OBSERVATION_BLOCK_TARGETS.contains(block)) {
                    this.observedBlocks.remove(block);
                }
            }
            for (EntityType<? extends Entity> entityType : this.observedEntities.keySet()) {
                if (!OBSERVATION_ENTITY_TARGETS.contains(entityType)) {
                    this.observedEntities.remove(entityType);
                }
            }
        }

        if (player.tickCount - this.lastTick >= CommonConfig.OBSERVATION_TICK_COOLDOWN.get()) {
            this.lastTick = player.tickCount;

            ServerLevel serverLevel = (ServerLevel) player.level();
            ServerPlayer serverPlayer = (ServerPlayer) player;

            // Clear removed (null) or dead swat entities
            this.commanders.removeIf(uuid -> !(serverLevel.getEntity(uuid) instanceof SwatEntity swat) || swat.isRemoved() || swat.getState() == SwatEntity.STATE_DEAD);

            // If all commanders are dead, current mission is failed
            if (this.commanders.isEmpty()) {
                this.swatMission = null;
            }

            // Select a new Mission if previous one was failed
            if (this.swatMission == null) {
                List<Mission> possibleMissions = new ArrayList<>();
                if (!this.anyObservedBlocks()) {
                    possibleMissions.add(Mission.RAID);
                }
                if (!this.anyObservedEntities()) {
                    possibleMissions.add(Mission.RESCUE);
                }
                if (serverLevel.dimension() == serverPlayer.getRespawnDimension() && serverPlayer.getRespawnPosition() != null) {
                    possibleMissions.add(Mission.ARREST);
                }
                possibleMissions.add(Mission.SCOUTING);
                possibleMissions.add(Mission.SIEGE);
                possibleMissions.add(Mission.SABOTAGE);
                this.swatMission = possibleMissions.get(player.getRandom().nextInt(possibleMissions.size()));

                // Notify the player that new swat mission is about to start
                serverPlayer.sendSystemMessage(this.swatMission.getMessage().append(", ").append(serverPlayer.getDisplayName()));
            }

            // Spawn Mission Commander
            if (this.swatMission == null) return;
            if (this.commanders.size() >= CommonConfig.OBSERVATION_SQUAD_COUNT.get()) return;

            for (int i = 0; i < CommonConfig.OBSERVATION_SQUAD_COUNT.get(); ++i) {
                SwatEntity commander = SwatEntity.create(serverLevel, Specialty.COMMANDER, this.swatMission);

                // Temporarily set commander's position to player to be able to get a random pos
                commander.setPos(player.position());
                Vec3 pos = LandRandomPos.getPos(commander, 127, 15);
                if (pos == null) continue;

                commander.setPos(pos);
                commander.setTarget(player);
                ForgeEventFactory.onFinalizeSpawn(commander, serverLevel, serverLevel.getCurrentDifficultyAt(commander.getOnPos()), MobSpawnType.MOB_SUMMONED, null, null);
                if (serverLevel.addFreshEntity(commander)) {
                    this.commanders.add(commander.getUUID());
                }
            }
        }
    }

    private void validateObserved() {
        for (Block block : this.observedBlocks.keySet()) {
            if (!OBSERVATION_BLOCK_TARGETS.contains(block)) {
                this.observedBlocks.remove(block);
            }
        }
        for (EntityType<? extends Entity> entityType : this.observedEntities.keySet()) {
            if (!OBSERVATION_ENTITY_TARGETS.contains(entityType)) {
                this.observedEntities.remove(entityType);
            }
        }
    }

    @Override
    public void copy(IObservation other) {
        this.lastTick = other.getLastTick();
        this.commanders = other.getCommanders();
        this.observedBlocks = other.getObservedBlocks();
        this.observedEntities = other.getObservedEntities();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();

        if (this.swatMission != null) {
            nbt.putString("swatMission", this.swatMission.getName());
        }

        ListTag spawnedUUIDsTag = new ListTag();
        this.commanders.forEach(uuid -> spawnedUUIDsTag.add(NbtUtils.createUUID(uuid)));
        nbt.put("commanders", spawnedUUIDsTag);

        CompoundTag blocksTag = new CompoundTag();
        this.observedBlocks.forEach((block, list) -> {
            ListTag blockTag = new ListTag();
            for (BlockPos pos : list) {
                blockTag.add(NbtUtils.writeBlockPos(pos));
            }
            blocksTag.put(getBlockName(block), blockTag);
        });
        nbt.put("blocks", blocksTag);

        CompoundTag entitiesTag = new CompoundTag();
        this.observedEntities.forEach((entityType, list) -> {
            ListTag entityTypeTag = new ListTag();
            for (UUID uuid : list) {
                entityTypeTag.add(NbtUtils.createUUID(uuid));
            }
            entitiesTag.put(getEntityTypeName(entityType), entityTypeTag);
        });
        nbt.put("entities", entitiesTag);

        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {

        if (nbt.contains("swatMission")) {
            Mission mission = Mission.byName(nbt.getString("swatMission"));
            if (mission != null) this.swatMission = mission;
        }

        Tag tag = nbt.get("commanders");
        if (tag instanceof ListTag commandersTag && !commandersTag.isEmpty()) {
            for (Tag t : commandersTag) {
                this.commanders.add(NbtUtils.loadUUID(t));
            }
        }

        CompoundTag blocksTag = (CompoundTag) nbt.get("blocks");
        if (blocksTag != null) {
            for (Block block : OBSERVATION_BLOCK_TARGETS) {
                tag = blocksTag.get(getBlockName(block));
                if (!(tag instanceof ListTag blockTag) || blockTag.isEmpty()) continue;
                List<BlockPos> list = new ArrayList<>();
                for (Tag t : blockTag) {
                    list.add(NbtUtils.readBlockPos((CompoundTag) t));
                }
                this.observedBlocks.put(block, list);
            }
        }

        CompoundTag entitiesTag = (CompoundTag) nbt.get("entities");
        if (entitiesTag != null) {
            for (EntityType<?> entityType : OBSERVATION_ENTITY_TARGETS) {
                tag = entitiesTag.get(getEntityTypeName(entityType));
                if (!(tag instanceof ListTag entityTypeTag) || entityTypeTag.isEmpty()) continue;
                List<UUID> list = new ArrayList<>();
                for (Tag t : entityTypeTag) {
                    list.add(NbtUtils.loadUUID(t));
                }
                this.observedEntities.put(entityType, list);
            }
        }
    }

    public static boolean isObserved(Block block) {
        return OBSERVATION_BLOCK_TARGETS.contains(block);
    }

    public static boolean isObserved(BlockState blockState) {
        return isObserved(blockState.getBlock());
    }

    public static boolean isObserved(EntityType<?> entityType) {
        return OBSERVATION_ENTITY_TARGETS.contains(entityType);
    }

    private boolean anyObservedBlocks() {
        for (Block block : OBSERVATION_BLOCK_TARGETS) {
            List<BlockPos> list = this.observedBlocks.get(block);
            if (list != null && !list.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean anyObservedEntities() {
        for (EntityType<? extends Entity> entityType : OBSERVATION_ENTITY_TARGETS) {
            List<UUID> list = this.observedEntities.get(entityType);
            if (list != null && !list.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static String getBlockName(Block block) {
        return Objects.requireNonNull(ForgeRegistries.BLOCKS.getKey(block)).getPath();
    }

    private static String getEntityTypeName(EntityType<?> entityType) {
        return Objects.requireNonNull(ForgeRegistries.ENTITY_TYPES.getKey(entityType)).getPath();
    }

    public static void loadTargetsFromConfig() {
        OBSERVATION_BLOCK_TARGETS.clear();
        CommonConfig.OBSERVATION_BLOCK_TARGETS.get().stream()
                .map((id) -> ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse(id)))
                .forEach(OBSERVATION_BLOCK_TARGETS::add);

        OBSERVATION_ENTITY_TARGETS.clear();
        CommonConfig.OBSERVATION_ENTITY_TARGETS.get().stream()
                .map((id) -> ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.parse(id)))
                .forEach(OBSERVATION_ENTITY_TARGETS::add);
    }
}
