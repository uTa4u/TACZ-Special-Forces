package su.uTa4u.specialforces.client.models;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import su.uTa4u.specialforces.entities.SwatEntity;

@OnlyIn(Dist.CLIENT)
public class SwatModel extends HumanoidModel<SwatEntity> {
    public SwatModel(ModelPart root) {
        super(root);
    }

    @Override
    public void setupAnim(@NotNull SwatEntity entity, float limbSpeed, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSpeed, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        if (entity.getState() == SwatEntity.STATE_DEAD) {
            this.rightArm.setRotation(0.0f, 0.0f, 0.0f);
            this.leftArm.setRotation(0.0f, 0.0f, 0.0f);
            this.rightLeg.setRotation(0.0f, 0.0f, 0.0f);
            this.leftLeg.setRotation(0.0f, 0.0f, 0.0f);
        } else if (entity.getState() == SwatEntity.STATE_DOWN) {
            this.rightArm.xRot += 1.0f;
            this.leftArm.xRot += 1.0f;
            this.head.xRot += 1.0f;
        }
    }

    public static LayerDefinition createBodyLayer() {
        return LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f), 64, 64);
    }
}