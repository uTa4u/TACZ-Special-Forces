package su.uTa4u.specialforces;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public enum Specialty {
    COMMANDER("commander", Util.getResource("textures/entity/commander.png"), 0.3f, AttributeSupplier.builder().add(Attributes.MAX_HEALTH, 90).add(Attributes.FOLLOW_RANGE, 16).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.KNOCKBACK_RESISTANCE, 0.15f).build()),
    ASSAULTER("assaulter", Util.getResource("textures/entity/assaulter.png"), 0.3f, AttributeSupplier.builder().add(Attributes.MAX_HEALTH, 100).add(Attributes.FOLLOW_RANGE, 16).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.KNOCKBACK_RESISTANCE, 0.15f).build()),
    GRENADIER("grenadier", Util.getResource("textures/entity/grenadier.png"), 0.3f, AttributeSupplier.builder().add(Attributes.MAX_HEALTH, 80).add(Attributes.FOLLOW_RANGE, 16).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.KNOCKBACK_RESISTANCE, 0.15f).build()),
    BULLDOZER("bulldozer", Util.getResource("textures/entity/bulldozer.png"), 0.3f, AttributeSupplier.builder().add(Attributes.MAX_HEALTH, 150).add(Attributes.FOLLOW_RANGE, 16).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.KNOCKBACK_RESISTANCE, 0.15f).build()),
    ENGINEER ("engineer",  Util.getResource("textures/entity/engineer.png"),  0.3f, AttributeSupplier.builder().add(Attributes.MAX_HEALTH, 70).add(Attributes.FOLLOW_RANGE, 16).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.KNOCKBACK_RESISTANCE, 0.15f).build()),
    SNIPER   ("sniper",    Util.getResource("textures/entity/sniper.png"),    1.0f, AttributeSupplier.builder().add(Attributes.MAX_HEALTH, 50).add(Attributes.FOLLOW_RANGE, 16).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.KNOCKBACK_RESISTANCE, 0.15f).build()),
    MEDIC    ("medic",     Util.getResource("textures/entity/medic.png"),     0.3f, AttributeSupplier.builder().add(Attributes.MAX_HEALTH, 70).add(Attributes.FOLLOW_RANGE, 16).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.KNOCKBACK_RESISTANCE, 0.15f).build()),
    SCOUT    ("scout",     Util.getResource("textures/entity/scout.png"),     0.3f, AttributeSupplier.builder().add(Attributes.MAX_HEALTH, 60).add(Attributes.FOLLOW_RANGE, 16).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.KNOCKBACK_RESISTANCE, 0.15f).build()),
    SPY      ("spy",       Util.getResource("textures/entity/spy.png"),       0.3f, AttributeSupplier.builder().add(Attributes.MAX_HEALTH, 60).add(Attributes.FOLLOW_RANGE, 16).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.KNOCKBACK_RESISTANCE, 0.15f).build());

    private static final Random RNG = new Random();
    private static final Specialty[] VALUES = values();
    private static final int SIZE = VALUES.length;

    public final static Map<Specialty, Component> TYPE_NAME_BY_SPECIALTY = new HashMap<>();

    private final String name;
    private final ResourceLocation skin;
    private final float headAimChance;
    private final AttributeSupplier attributes;

    Specialty(String name, ResourceLocation skin, float headAimChance, AttributeSupplier supplier) {
        this.name = name;
        this.skin = skin;
        this.headAimChance = headAimChance;
        // TODO: factor out common attributes to be set here
        this.attributes = supplier;
    }

    public static Specialty getRandomSpecialty() {
        return VALUES[RNG.nextInt(SIZE)];
    }

    // TODO: make into a hashmap
    @Nullable
    public static Specialty getByName(String name) {
        return switch (name) {
            case ("commander") -> Specialty.COMMANDER;
            case ("assaulter") -> Specialty.ASSAULTER;
            case ("grenadier") -> Specialty.GRENADIER;
            case ("bulldozer") -> Specialty.BULLDOZER;
            case ("engineer") -> Specialty.ENGINEER;
            case ("sniper") -> Specialty.SNIPER;
            case ("medic") -> Specialty.MEDIC;
            case ("scout") -> Specialty.SCOUT;
            case ("spy") -> Specialty.SPY;
            default -> null;
        };
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
            TYPE_NAME_BY_SPECIALTY.put(specialty, Component.translatable("entity." + SpecialForces.MOD_ID + "." + specialty.getName()));
        }
    }
}
