package su.uTa4u.specialforces;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
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

    public static final Map<Specialty, Component> TYPE_NAMES = new HashMap<>();
    public static final Map<String, Specialty> SPECIALTY_BY_NAME = new HashMap<>();

    private final String name;
    private final ResourceLocation skin;
    private final float headAimChance;
    private final AttributeMap attributes;
    private final ResourceLocation lootTable;

    Specialty(String name, float headAimChance, AttributeSupplier supplier) {
        this.name = name;
        this.skin = Util.getResource("textures/entity/" + name + ".png");
        this.headAimChance = headAimChance;
        this.attributes = initAttributeMap(supplier);
        this.lootTable = Util.getResource("spawn_inv/" + name);
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

    public AttributeMap getAttributes() {
        return this.attributes;
    }

    public ResourceLocation getLootTable() {
        return this.lootTable;
    }

    private static AttributeMap initAttributeMap(AttributeSupplier supplier) {
        AttributeMap map = new AttributeMap(supplier);

        // Initialize `Attribute` types which are set on creating `Specialty` enum members,
        // otherwise `AttributeMap#assignValues` will try to loop over empty `attributes` map
        // and SwatEntities will spawn with default attributes
        map.getInstance(Attributes.MAX_HEALTH);
        map.getInstance(Attributes.FOLLOW_RANGE);
        map.getInstance(Attributes.KNOCKBACK_RESISTANCE);
        map.getInstance(Attributes.MOVEMENT_SPEED);
        map.getInstance(Attributes.ATTACK_DAMAGE);
        map.getInstance(Attributes.ATTACK_KNOCKBACK);
        map.getInstance(Attributes.ATTACK_SPEED);
        map.getInstance(Attributes.ARMOR);
        map.getInstance(Attributes.ARMOR_TOUGHNESS);

        return map;
    }

    static {
        for (Specialty specialty : VALUES) {
            TYPE_NAMES.put(specialty, Component.translatable("entity." + SpecialForces.MOD_ID + "." + specialty.name));
            SPECIALTY_BY_NAME.put(specialty.name, specialty);
        }
    }
}
