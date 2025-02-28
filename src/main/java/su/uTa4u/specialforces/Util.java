package su.uTa4u.specialforces;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.*;

public abstract class Util {

    private static final Attribute[] ATTRIBUTES = new Attribute[]{
            Attributes.MAX_HEALTH,
            Attributes.FOLLOW_RANGE,
            Attributes.KNOCKBACK_RESISTANCE,
            Attributes.MOVEMENT_SPEED,
            Attributes.FLYING_SPEED,
            Attributes.ATTACK_DAMAGE,
            Attributes.ATTACK_KNOCKBACK,
            Attributes.ATTACK_SPEED,
            Attributes.ARMOR,
            Attributes.ARMOR_TOUGHNESS,
    };
    private static final int ATTRIBUTES_SIZE = ATTRIBUTES.length;

    public static ResourceLocation getResource(String path) {
        return new ResourceLocation(SpecialForces.MOD_ID, path);
    }

    public static void copyAttributes(AttributeMap dest, AttributeSupplier src) {
        for (int i = 0; i < ATTRIBUTES_SIZE; i++) {
            AttributeInstance attr = dest.getInstance(ATTRIBUTES[i]);
            if (attr != null && src.hasAttribute(ATTRIBUTES[i])) {
                attr.setBaseValue(src.getBaseValue(ATTRIBUTES[i]));
            }
        }
    }
}
