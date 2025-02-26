package su.uTa4u.specialforces.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import su.uTa4u.specialforces.SpecialForces;

@OnlyIn(Dist.CLIENT)
public class ModModelLayers {
    public static final ModelLayerLocation TEST_MODEL = new ModelLayerLocation(new ResourceLocation(SpecialForces.MODID, "test_layer"), "main");
}
