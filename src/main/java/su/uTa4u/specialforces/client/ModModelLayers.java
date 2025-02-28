package su.uTa4u.specialforces.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import su.uTa4u.specialforces.Util;

@OnlyIn(Dist.CLIENT)
public class ModModelLayers {
    public static final ModelLayerLocation TEST_MODEL = new ModelLayerLocation(Util.getResource("test_layer"), "main");
}
