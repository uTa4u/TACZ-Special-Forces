package su.uTa4u.specialforces;

import com.tacz.guns.GunMod;
import net.minecraft.resources.ResourceLocation;

public abstract class Util {

    public static ResourceLocation getResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(SpecialForces.MOD_ID, path);
    }

    public static ResourceLocation getTaczResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, path);
    }

}
