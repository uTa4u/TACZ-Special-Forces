package su.uTa4u.specialforces.capabilities.observation;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@AutoRegisterCapability
public interface IObservation extends INBTSerializable<CompoundTag> {

    void copy(IObservation other);

    Map<Block, List<BlockPos>> getObservedBlocks();

    Map<EntityType<?>, List<UUID>> getObservedEntities();

    void observe(Block block, BlockPos pos);

    void observe(EntityType<?> entityType, UUID uuid);

    void clear();

    void setLastMissionTick(int tick);

    int getLastMissionTick();
}
