package su.uTa4u.specialforces.entities;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import su.uTa4u.specialforces.SpecialForces;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES;

    public static final RegistryObject<EntityType<TestEntity>> TEST_ENTITY;

    static {
        ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, SpecialForces.MOD_ID);

        TEST_ENTITY = registerEntityType("test_entity", TestEntity::new);
    }

    // TODO: change size and MobCategory?
    private static <T extends Entity> RegistryObject<EntityType<T>> registerEntityType(String name, EntityType.EntityFactory<T> factory) {
        return ENTITY_TYPES.register(name, () -> EntityType.Builder.of(factory, MobCategory.CREATURE).sized(1, 2).build(name));
    }
}
