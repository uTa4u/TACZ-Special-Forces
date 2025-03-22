package su.uTa4u.specialforces;

import com.tacz.guns.GunMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public abstract class Util {

    public static ResourceLocation getResource(String path) {
        return new ResourceLocation(SpecialForces.MOD_ID, path);
    }

    public static ResourceLocation getTaczResource(String path) {
        return new ResourceLocation(GunMod.MOD_ID, path);
    }

    public static double getBodyY(Entity entity) {
        return (entity.getBoundingBox().minY + entity.getBoundingBox().maxY) / 2.0;
    }

}
