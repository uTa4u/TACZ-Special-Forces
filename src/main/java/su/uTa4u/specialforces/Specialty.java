package su.uTa4u.specialforces;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public enum Specialty {
    COMMANDER("commander", 0.3f, AttributeSupplier.builder()
            .add(Attributes.MAX_HEALTH, 90)
            .add(Attributes.FOLLOW_RANGE, 16)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.15f).build()),
    ASSAULTER("assaulter", 0.3f, AttributeSupplier.builder()
            .add(Attributes.MAX_HEALTH, 100)
            .add(Attributes.FOLLOW_RANGE, 16)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.15f).build()),
    GRENADIER("grenadier", 0.3f, AttributeSupplier.builder()
            .add(Attributes.MAX_HEALTH, 80)
            .add(Attributes.FOLLOW_RANGE, 16)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.15f).build()),
    BULLDOZER("bulldozer", 0.3f, AttributeSupplier.builder()
            .add(Attributes.MAX_HEALTH, 150)
            .add(Attributes.FOLLOW_RANGE, 16)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.15f).build()),
    ENGINEER("engineer", 0.3f, AttributeSupplier.builder()
            .add(Attributes.MAX_HEALTH, 70)
            .add(Attributes.FOLLOW_RANGE, 16)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.15f).build()),
    SNIPER("sniper", 1.0f, AttributeSupplier.builder()
            .add(Attributes.MAX_HEALTH, 50)
            .add(Attributes.FOLLOW_RANGE, 16)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.15f).build()),
    MEDIC("medic", 0.3f, AttributeSupplier.builder()
            .add(Attributes.MAX_HEALTH, 70)
            .add(Attributes.FOLLOW_RANGE, 16)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.15f).build()),
    SCOUT("scout", 0.3f, AttributeSupplier.builder()
            .add(Attributes.MAX_HEALTH, 60)
            .add(Attributes.FOLLOW_RANGE, 16)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.15f).build()),
    SPY("spy", 0.3f, AttributeSupplier.builder()
            .add(Attributes.MAX_HEALTH, 60)
            .add(Attributes.FOLLOW_RANGE, 16)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.15f).build());

    private static final Random RNG = new Random();
    private static final Specialty[] VALUES = values();
    private static final int SIZE = VALUES.length;

    public final static Map<Specialty, Component> TYPE_NAME_BY_SPECIALTY = new HashMap<>();
    public final static Map<String, Specialty> SPECIALTY_BY_NAME = new HashMap<>();

    private final String name;
    private final ResourceLocation skin;
    private final float headAimChance;
    private final AttributeSupplier attributes;

    Specialty(String name, float headAimChance, AttributeSupplier supplier) {
        this.name = name;
        this.skin = Util.getResource("textures/entity/" + name + ".png");
        this.headAimChance = headAimChance;
        this.attributes = supplier;
    }

    public static Specialty getRandomSpecialty() {
        return VALUES[RNG.nextInt(SIZE)];
    }

    public String getName() {
        return this.name;
    }

    public ResourceLocation getSkin() {
        return this.skin;
    }

    public float getHeadAimChance() {
        return this.headAimChance;
    }

    public AttributeSupplier getAttributes() {
        return this.attributes;
    }

    static {
        for (Specialty specialty : VALUES) {
            TYPE_NAME_BY_SPECIALTY.put(specialty, Component.translatable("entity." + SpecialForces.MOD_ID + "." + specialty.name));
            SPECIALTY_BY_NAME.put(specialty.name, specialty);
        }
    }
}
