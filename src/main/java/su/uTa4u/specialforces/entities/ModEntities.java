package su.uTa4u.specialforces.entities;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import su.uTa4u.specialforces.SpecialForces;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES;

    public static final RegistryObject<EntityType<SwatEntity>> SWAT_ENTITY;

    static {
        ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, SpecialForces.MOD_ID);

        SWAT_ENTITY = ENTITY_TYPES.register("swat_entity",
                () -> EntityType.Builder.of(SwatEntity::new, MobCategory.CREATURE)
                        .sized(0.6f, 1.8f)
                        .build("swat_entity"));
    }

}
