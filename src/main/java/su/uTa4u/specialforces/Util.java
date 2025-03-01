package su.uTa4u.specialforces;

import net.minecraft.resources.ResourceLocation;

public abstract class Util {

    public static ResourceLocation getResource(String path) {
        return new ResourceLocation(SpecialForces.MOD_ID, path);
    }

}
