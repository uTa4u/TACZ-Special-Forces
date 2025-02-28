package su.uTa4u.specialforces;

import net.minecraft.resources.ResourceLocation;

public abstract class util {

    public static ResourceLocation getResource(String path) {
        return new ResourceLocation(SpecialForces.MODID, path);
    }
}
