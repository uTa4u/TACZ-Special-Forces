package su.uTa4u.specialforces;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public abstract class ModTags {

    public static class Items {
        public static final TagKey<Item> RULE_HOTBAR = TagKey.create(Registries.ITEM, Util.getResource("inventory_rules/hotbar"));
        public static final TagKey<Item> RULE_OFFHAND = TagKey.create(Registries.ITEM, Util.getResource("inventory_rules/offhand"));
    }

}
