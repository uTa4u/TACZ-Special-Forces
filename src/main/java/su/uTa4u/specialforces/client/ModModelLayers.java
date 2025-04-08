package su.uTa4u.specialforces.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import su.uTa4u.specialforces.Util;

@OnlyIn(Dist.CLIENT)
public class ModModelLayers {
    public static final ModelLayerLocation SWAT = new ModelLayerLocation(Util.getResource("swat"), "main");
    public static final ModelLayerLocation SWAT_INNER_ARMOR = new ModelLayerLocation(Util.getResource("swat"), "inner_armor");
    public static final ModelLayerLocation SWAT_OUTER_ARMOR = new ModelLayerLocation(Util.getResource("swat"), "outer_armor");
}
