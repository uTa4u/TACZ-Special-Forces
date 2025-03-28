package su.uTa4u.specialforces.capabilities.observation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class Observation implements IObservation {
    private static final Set<Block> BLOCKS_UNDER_OBSERVATION = new HashSet<>();
    private static final Set<EntityType<?>> ENTITIES_UNDER_OBSERVATION = new HashSet<>();
//    protected static final int MISSION_COOLDOWN = 24000 * 2;
    static final int MISSION_COOLDOWN = 1200;

    private Map<Block, List<BlockPos>> observedBlocks = new HashMap<>();
    private Map<EntityType<?>, List<UUID>> observedEntities = new HashMap<>();
    private int lastMissionTick = 0;

    Observation() {
        BLOCKS_UNDER_OBSERVATION.forEach( block -> observedBlocks.put(block, new ArrayList<>()));
        ENTITIES_UNDER_OBSERVATION.forEach( entityType -> observedEntities.put(entityType, new ArrayList<>()));
    }

    @Override
    public void setLastMissionTick(int tick) {
        this.lastMissionTick = tick;
    }

    @Override
    public int getLastMissionTick() {
        return this.lastMissionTick;
    }

    @Override
    public void observe(Block block, BlockPos pos) {
        if (!this.observedBlocks.get(block).contains(pos)) {
            this.observedBlocks.get(block).add(pos);
        }
    }

    @Override
    public void observe(EntityType<?> entityType, UUID uuid) {
        if (!this.observedEntities.get(entityType).contains(uuid)) {
            this.observedEntities.get(entityType).add(uuid);
        }
    }

    @Override
    public void clear() {
        this.observedBlocks.clear();
        this.observedEntities.clear();
    }

    @Override
    public Map<Block, List<BlockPos>> getObservedBlocks() {
        return this.observedBlocks;
    }

    @Override
    public Map<EntityType<?>, List<UUID>> getObservedEntities() {
        return this.observedEntities;
    }

    @Override
    public void copy(IObservation other) {
        this.lastMissionTick = other.getLastMissionTick();
        this.observedBlocks = other.getObservedBlocks();
        this.observedEntities = other.getObservedEntities();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();

        nbt.putInt("lastMissionTick", this.lastMissionTick);

        CompoundTag blocksTag = new CompoundTag();
        this.observedBlocks.forEach( (block, list) -> {
            ListTag blockTag = new ListTag();
            for (BlockPos pos : list) {
                blockTag.add(NbtUtils.writeBlockPos(pos));
            }
            blocksTag.put(getBlockName(block), blockTag);
        });
        nbt.put("blocks", blocksTag);

        CompoundTag entitiesTag = new CompoundTag();
        this.observedEntities.forEach( (entityType, list) -> {
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

        this.lastMissionTick = nbt.getInt("lastMissionTick");

        CompoundTag blocksTag = (CompoundTag) nbt.get("blocks");
        if (blocksTag != null) {
            for (Block block : BLOCKS_UNDER_OBSERVATION) {
                Tag tag = blocksTag.get(getBlockName(block));
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
            for (EntityType<?> entityType : ENTITIES_UNDER_OBSERVATION) {
                Tag tag = entitiesTag.get(getEntityTypeName(entityType));
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
        return BLOCKS_UNDER_OBSERVATION.contains(block);
    }

    public static boolean isObserved(BlockState blockState) {
        return isObserved(blockState.getBlock());
    }

    public static boolean isObserved(EntityType<?> entityType) {
        return ENTITIES_UNDER_OBSERVATION.contains(entityType);
    }

    private static String getBlockName(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).getPath();
    }

    private static String getEntityTypeName(EntityType<?> entityType) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entityType).getPath();
    }

    static {
        BLOCKS_UNDER_OBSERVATION.add(Blocks.CHEST);
        BLOCKS_UNDER_OBSERVATION.add(Blocks.FURNACE);

        ENTITIES_UNDER_OBSERVATION.add(EntityType.VILLAGER);
        ENTITIES_UNDER_OBSERVATION.add(EntityType.COW);
    }
}
