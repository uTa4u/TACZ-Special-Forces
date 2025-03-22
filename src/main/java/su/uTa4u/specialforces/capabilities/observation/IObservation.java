package su.uTa4u.specialforces.capabilities.observation;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.UUID;

@AutoRegisterCapability
public interface IObservation extends INBTSerializable<CompoundTag> {

    void observe(Block block, BlockPos pos);

    void observe(EntityType<?> entityType, UUID uuid);

    void clear();
}
