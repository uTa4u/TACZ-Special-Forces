package su.uTa4u.specialforces.glms;

import com.mojang.serialization.Codec;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import su.uTa4u.specialforces.SpecialForces;

public class ModLootModifiers {
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIER_SERIALIZERS;

    public static final RegistryObject<Codec<? extends IGlobalLootModifier>> ADD_ITEMS;
    static {
        LOOT_MODIFIER_SERIALIZERS = DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, SpecialForces.MOD_ID);

        ADD_ITEMS = LOOT_MODIFIER_SERIALIZERS.register("add_item", AddItemModifier.CODEC);
    }
}
