package su.uTa4u.specialforces.client.renderers;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import su.uTa4u.specialforces.client.ModModelLayers;
import su.uTa4u.specialforces.client.models.TestModel;
import su.uTa4u.specialforces.entities.TestEntity;

@OnlyIn(Dist.CLIENT)
public class TestRenderer extends MobRenderer<TestEntity, TestModel<TestEntity>> {

    public TestRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new TestModel<>(ctx.bakeLayer(ModModelLayers.TEST_MODEL)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(TestEntity entity) {
        return entity.getSpecialty().getSkin();
    }
}
