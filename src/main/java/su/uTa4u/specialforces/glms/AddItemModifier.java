package su.uTa4u.specialforces.glms;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Supplier;

public class AddItemModifier extends LootModifier {
    public static final Supplier<Codec<AddItemModifier>> CODEC = Suppliers.memoize(() ->
            RecordCodecBuilder.create((inst) ->
                    LootModifier.codecStart(inst).and(
                            Entry.CODEC.listOf().fieldOf("entries").forGetter(m -> m.entries)
                    ).apply(inst, AddItemModifier::new)
            )
    );

    private final List<Entry> entries;

    public AddItemModifier(LootItemCondition[] conditions, List<Entry> entries) {
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
            generatedLoot.add(new ItemStack(entry.item, entry.count, entry.nbt));
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
                                CompoundTag.CODEC.optionalFieldOf("nbt", new CompoundTag()).forGetter(m -> m.nbt),
                                Codec.floatRange(0.0f, 1.0f).optionalFieldOf("chance", 1.0f).forGetter(m -> m.chance)
                        ).apply(inst, Entry::new)
                );

        private final Item item;
        private final int count;
        private final CompoundTag nbt;
        private final float chance;

        public Entry(Item item, int count, CompoundTag nbt, float chance) {
            this.item = item;
            this.count = count;
            this.nbt = nbt;
            this.chance = chance;
        }
    }
}
