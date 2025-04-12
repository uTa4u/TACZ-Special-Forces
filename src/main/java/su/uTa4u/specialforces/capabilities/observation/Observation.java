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
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.registries.ForgeRegistries;
import su.uTa4u.specialforces.Mission;
import su.uTa4u.specialforces.config.CommonConfig;
import su.uTa4u.specialforces.entities.SwatEntity;

import java.util.*;

public class Observation implements IObservation {
    private static final String NBT_KEY_SWAT_MISSION = "SwatMission";
    private static final String NBT_KEY_COMMANDERS = "Commanders";
    private static final String NBT_KEY_BLOCKS = "Blocks";
    private static final String NBT_KEY_ENTITIES = "Entities";
    private static final String NBT_KEY_TICK_TIMER = "TickTimer";

    private static final Set<Block> OBSERVATION_BLOCK_TARGETS = new HashSet<>();
    private static final Set<EntityType<? extends Entity>> OBSERVATION_ENTITY_TARGETS = new HashSet<>();

    private Map<Block, List<BlockPos>> observedBlocks = new HashMap<>();
    private Map<EntityType<? extends Entity>, List<UUID>> observedEntities = new HashMap<>();
    private int tickTimer = 0;
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
    public int getTickTimer() {
        return this.tickTimer;
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
            this.validateObserved();
        }

        this.tickTimer += 1;
        if (this.tickTimer >= CommonConfig.OBSERVATION_TICK_COOLDOWN.get()) {
            this.tickTimer = 0;

            ServerLevel serverLevel = (ServerLevel) player.level();
            ServerPlayer serverPlayer = (ServerPlayer) player;

            // Clear invalid swat commanders
            this.commanders.removeIf((uuid) -> isCommanderInvalid(uuid, serverLevel));

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
                SwatEntity commander = SwatEntity.commander(serverLevel, this.swatMission);

                // Temporarily set commander's position to player to be able to get a random pos
                commander.setPos(player.position());
                Vec3 pos = LandRandomPos.getPos(commander, 64, 15);
                if (pos == null) continue;

                commander.setPos(pos);
                commander.setTarget(player);
                ForgeEventFactory.onFinalizeSpawn(commander, serverLevel, serverLevel.getCurrentDifficultyAt(commander.getOnPos()), MobSpawnType.MOB_SUMMONED, null, null);
                if (serverLevel.addFreshEntity(commander)) {
                    this.commanders.add(commander.getUUID());
                    commander.summonSquadNextTick();
                }
            }
        }
    }

    private void validateObserved() {
        for (Block block : this.observedBlocks.keySet()) {
            if (!isObservationTarget(block)) {
                this.observedBlocks.remove(block);
            }
        }
        for (EntityType<? extends Entity> entityType : this.observedEntities.keySet()) {
            if (!isObservationTarget(entityType)) {
                this.observedEntities.remove(entityType);
            }
        }
    }

    @Override
    public void copy(IObservation other) {
        this.tickTimer = other.getTickTimer();
        this.commanders = other.getCommanders();
        this.observedBlocks = other.getObservedBlocks();
        this.observedEntities = other.getObservedEntities();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();

        nbt.putInt(NBT_KEY_TICK_TIMER, this.tickTimer);

        if (this.swatMission != null) {
            nbt.putString(NBT_KEY_SWAT_MISSION, this.swatMission.getName());
        }

        ListTag spawnedUUIDsTag = new ListTag();
        this.commanders.forEach(uuid -> spawnedUUIDsTag.add(NbtUtils.createUUID(uuid)));
        nbt.put(NBT_KEY_COMMANDERS, spawnedUUIDsTag);

        CompoundTag blocksTag = new CompoundTag();
        this.observedBlocks.forEach((block, list) -> {
            ListTag blockTag = new ListTag();
            for (BlockPos pos : list) {
                blockTag.add(NbtUtils.writeBlockPos(pos));
            }
            blocksTag.put(getBlockName(block), blockTag);
        });
        nbt.put(NBT_KEY_BLOCKS, blocksTag);

        CompoundTag entitiesTag = new CompoundTag();
        this.observedEntities.forEach((entityType, list) -> {
            ListTag entityTypeTag = new ListTag();
            for (UUID uuid : list) {
                entityTypeTag.add(NbtUtils.createUUID(uuid));
            }
            entitiesTag.put(getEntityTypeName(entityType), entityTypeTag);
        });
        nbt.put(NBT_KEY_ENTITIES, entitiesTag);

        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {

        this.tickTimer = nbt.getInt(NBT_KEY_TICK_TIMER);

        if (nbt.contains(NBT_KEY_SWAT_MISSION)) {
            Mission mission = Mission.byName(nbt.getString(NBT_KEY_SWAT_MISSION));
            if (mission != null) this.swatMission = mission;
        }

        Tag tag = nbt.get(NBT_KEY_COMMANDERS);
        if (tag instanceof ListTag commandersTag && !commandersTag.isEmpty()) {
            for (Tag t : commandersTag) {
                this.commanders.add(NbtUtils.loadUUID(t));
            }
        }

        CompoundTag blocksTag = (CompoundTag) nbt.get(NBT_KEY_BLOCKS);
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

        CompoundTag entitiesTag = (CompoundTag) nbt.get(NBT_KEY_ENTITIES);
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

    public static boolean isObservationTarget(Block block) {
        return OBSERVATION_BLOCK_TARGETS.contains(block);
    }

    public static boolean isObservationTarget(EntityType<?> entityType) {
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

    private static boolean isCommanderInvalid(UUID uuid, ServerLevel serverLevel) {
        return !(serverLevel.getEntity(uuid) instanceof SwatEntity swat)
                || swat.isRemoved()
                || swat.getState() == SwatEntity.STATE_DEAD
                || !swat.hasMission()
                || !swat.hasSquad();
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
