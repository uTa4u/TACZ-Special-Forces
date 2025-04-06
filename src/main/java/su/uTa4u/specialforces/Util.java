package su.uTa4u.specialforces;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.item.GunTabType;
import net.minecraft.resources.ResourceLocation;

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

}
