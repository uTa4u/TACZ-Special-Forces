package su.uTa4u.specialforces.glms;

import com.google.common.base.Suppliers;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.List;
import java.util.function.Supplier;

public class AddItemsModifier extends LootModifier {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Supplier<Codec<AddItemsModifier>> CODEC = Suppliers.memoize(() ->
            RecordCodecBuilder.create((inst) ->
                    LootModifier.codecStart(inst).and(
                            Entry.CODEC.listOf().fieldOf("entries").forGetter(m -> m.entries)
                    ).apply(inst, AddItemsModifier::new)
            )
    );

    private final List<Entry> entries;

    public AddItemsModifier(LootItemCondition[] conditions, List<Entry> entries) {
        super(conditions);
        this.entries = entries;
    }

    @NotNull
    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext lootContext) {
        for (LootItemCondition condition : this.conditions) {
            if (!condition.test(lootContext)) {
                return generatedLoot;
            }
        }

        for (Entry entry : this.entries) {
            ItemStack toAdd = new ItemStack(entry.item, entry.count);
            CompoundTag nbt;
            try {
                nbt = TagParser.parseTag(entry.nbt);
            } catch (CommandSyntaxException e) {
                nbt = null;
                LOGGER.error(e.getMessage());
            }
            toAdd.setTag(nbt);
            generatedLoot.add(toAdd);
        }

        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }

    public static class Entry {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create((inst) ->
                        inst.group(
                                ForgeRegistries.ITEMS.getCodec().fieldOf("item").forGetter(m -> m.item),
                                Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(m -> m.count),
                                Codec.STRING.optionalFieldOf("nbt", "{}").forGetter(m -> m.nbt),
                                Codec.floatRange(0.0f, 1.0f).optionalFieldOf("chance", 1.0f).forGetter(m -> m.chance)
                        ).apply(inst, Entry::new)
                );

        private final Item item;
        private final int count;
        private final String nbt;
        private final float chance;

        public Entry(Item item, int count, String nbt, float chance) {
            this.item = item;
            this.count = count;
            this.nbt = nbt;
            this.chance = chance;
        }
    }
}
