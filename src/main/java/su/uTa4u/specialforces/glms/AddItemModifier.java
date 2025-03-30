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

import java.util.function.Supplier;

public class AddItemModifier extends LootModifier {
    public static final Supplier<Codec<AddItemModifier>> CODEC = Suppliers.memoize(() ->
            RecordCodecBuilder.create((inst) ->
                    LootModifier.codecStart(inst).and(
                            inst.group(
                                    ForgeRegistries.ITEMS.getCodec().fieldOf("item").forGetter(m -> m.item),
                                    Codec.INT.fieldOf("count").forGetter(m -> m.count),
                                    Codec.STRING.fieldOf("nbt").forGetter(m -> m.nbt)
                            )
                    ).apply(inst, AddItemModifier::new)
            )
    );

    private static final Logger LOGGER = LogUtils.getLogger();

    private final Item item;
    private final int count;
    private final String nbt;

    public AddItemModifier(LootItemCondition[] conditions, Item item, int count, String nbt) {
        super(conditions);
        this.item = item;
        this.count = count;
        this.nbt = nbt;
    }

    @NotNull
    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext lootContext) {
        for (LootItemCondition condition : this.conditions) {
            if (!condition.test(lootContext)) {
                return generatedLoot;
            }
        }

        CompoundTag nbt;
        try {
            nbt = TagParser.parseTag(this.nbt);
        } catch (CommandSyntaxException e) {
            nbt = null;
            LOGGER.error(e.getMessage());
        }

        generatedLoot.add(new ItemStack(this.item, this.count, nbt));

        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}
