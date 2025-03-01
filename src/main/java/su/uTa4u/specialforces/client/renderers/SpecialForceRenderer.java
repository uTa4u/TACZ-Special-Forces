package su.uTa4u.specialforces.client.renderers;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import su.uTa4u.specialforces.client.ModModelLayers;
import su.uTa4u.specialforces.client.models.SwatModel;
import su.uTa4u.specialforces.entities.SwatEntity;

@OnlyIn(Dist.CLIENT)
public class SpecialForceRenderer extends MobRenderer<SwatEntity, SwatModel<SwatEntity>> {

    public SpecialForceRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new SwatModel<>(ctx.bakeLayer(ModModelLayers.TEST_MODEL)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(SwatEntity entity) {
        return entity.getSpecialty().getSkin();
    }
}
