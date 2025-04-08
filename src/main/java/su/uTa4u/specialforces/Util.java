package su.uTa4u.specialforces;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.item.GunTabType;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public abstract class Util {

    public static ResourceLocation getResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(SpecialForces.MOD_ID, path);
    }

    public static ResourceLocation getTaczResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, path);
    }

    public static String capitalizeFirstLetter(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    public static String getGunTabTypeName(GunTabType type) {
        return type.name().toLowerCase(Locale.US);
    }

    // Adapted from LootTable#shuffleAndSplitItems
    public static void shuffleAndSplitItems(ObjectArrayList<ItemStack> items, int emptySlotCount, RandomSource random) {
        List<ItemStack> list = new ArrayList<>();
        Iterator<ItemStack> iterator = items.iterator();

        ItemStack itemstack;
        while (iterator.hasNext()) {
            itemstack = iterator.next();
            if (itemstack.isEmpty()) {
                iterator.remove();
            } else if (itemstack.getCount() > 1) {
                list.add(itemstack);
                iterator.remove();
            }
        }

        while (emptySlotCount - items.size() - list.size() > 0 && !list.isEmpty()) {
            itemstack = list.remove(Mth.nextInt(random, 0, list.size() - 1));
            int i = Mth.nextInt(random, 1, itemstack.getCount() / 2);
            ItemStack itemstack1 = itemstack.split(i);
            if (itemstack.getCount() > 1 && random.nextBoolean()) {
                list.add(itemstack);
            } else {
                items.add(itemstack);
            }

            if (itemstack1.getCount() > 1 && random.nextBoolean()) {
                list.add(itemstack1);
            } else {
                items.add(itemstack1);
            }
        }

        items.addAll(list);
        net.minecraft.Util.shuffle(items, random);
    }

}
