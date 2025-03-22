package su.uTa4u.specialforces.client.models;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import su.uTa4u.specialforces.entities.SwatEntity;

@OnlyIn(Dist.CLIENT)
public class SwatModel extends HumanoidModel<SwatEntity> {
    public SwatModel(ModelPart root) {
        super(root);
    }

    @Override
    public void setupAnim(SwatEntity entity, float limbSpeed, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSpeed, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
//        this.root().getAllParts().forEach(ModelPart::resetPose);
        // FIXME: switch on entity.getSpecialty() to handle different animations
//        this.animate(((SwatEntity) entity).idleAnimationState, SpecialForceAnimation.IDLE, ageInTicks, 1.0f);
    }

    public static LayerDefinition createBodyLayer() {
        return LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f), 64, 64);
    }
}