package su.uTa4u.specialforces.items;

import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import su.uTa4u.specialforces.SpecialForces;
import su.uTa4u.specialforces.entities.ModEntities;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS;

    public static final RegistryObject<Item> SWAT_SPAWN_EGG;

    static {
        ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SpecialForces.MOD_ID);

        SWAT_SPAWN_EGG = ITEMS.register("swat_spawn_egg", () -> new ForgeSpawnEggItem(ModEntities.SWAT_ENTITY, 0x131313, 0x78866b, new Item.Properties()));
    }

}
