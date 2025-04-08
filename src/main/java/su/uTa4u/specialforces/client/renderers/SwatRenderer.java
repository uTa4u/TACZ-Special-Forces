package su.uTa4u.specialforces.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import su.uTa4u.specialforces.client.ModModelLayers;
import su.uTa4u.specialforces.client.models.SwatModel;
import su.uTa4u.specialforces.entities.SwatEntity;

@OnlyIn(Dist.CLIENT)
public class SwatRenderer extends HumanoidMobRenderer<SwatEntity, SwatModel> {
    public SwatRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new SwatModel(ctx.bakeLayer(ModModelLayers.SWAT)), 0.5f);
        this.addLayer(new HumanoidArmorLayer<>(this, new SwatModel(ctx.bakeLayer(ModModelLayers.SWAT_INNER_ARMOR)), new SwatModel(ctx.bakeLayer(ModModelLayers.SWAT_OUTER_ARMOR)), ctx.getModelManager()));
    }

    @NotNull
    @Override
    public ResourceLocation getTextureLocation(SwatEntity entity) {
        return entity.getSpecialty().getSkin();
    }

    @Override
    protected void setupRotations(SwatEntity entity, @NotNull PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTicks) {
        // Adapted from PlayerRenderer and LivingEntityRenderer
        float f = entity.getSwimAmount(partialTicks);
        float f3;
        float f2;
        if (f > 0.0f) {
            super.setupRotations(entity, poseStack, ageInTicks, rotationYaw, partialTicks);
            f3 = !entity.isInWater() && !entity.isInFluidType((fluidType, height) -> entity.canSwimInFluidType(fluidType)) ? -90.0F : -90.0F - entity.getXRot();
            f2 = Mth.lerp(f, 0.0f, f3);
            poseStack.mulPose(Axis.XP.rotationDegrees(f2));
            if (entity.isVisuallySwimming()) {
                poseStack.translate(0.0f, -1.0f, 0.3f);
            }
        } else if (entity.getState() == SwatEntity.STATE_DOWN) {
            poseStack.mulPose(Axis.YN.rotationDegrees(rotationYaw + 180.0f));
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));
            poseStack.translate(0.0f, -1.0f, 0.0f);
        } else if (entity.getState() == SwatEntity.STATE_DEAD) {
            poseStack.mulPose(Axis.YN.rotationDegrees(rotationYaw + 180.0f));
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));
            poseStack.translate(0.0f, -1.0f, 0.0f);
        } else {
            super.setupRotations(entity, poseStack, ageInTicks, rotationYaw, partialTicks);
        }
    }
}
