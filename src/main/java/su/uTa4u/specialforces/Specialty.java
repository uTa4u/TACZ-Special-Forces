package su.uTa4u.specialforces;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Random;

public enum Specialty {
    COMMANDER("commander", Util.getResource("textures/entity/commander.png"), AttributeSupplier.builder().add(Attributes.MAX_HEALTH, 90 ).build()),
    ASSAULTER("assaulter", Util.getResource("textures/entity/assaulter.png"), AttributeSupplier.builder().add(Attributes.MAX_HEALTH, 100).build()),
    GRENADIER("grenadier", Util.getResource("textures/entity/grenadier.png"), AttributeSupplier.builder().add(Attributes.MAX_HEALTH, 80 ).build()),
    BULLDOZER("bulldozer", Util.getResource("textures/entity/bulldozer.png"), AttributeSupplier.builder().add(Attributes.MAX_HEALTH, 150).build()),
    ENGINEER ("engineer",  Util.getResource("textures/entity/engineer.png"),  AttributeSupplier.builder().add(Attributes.MAX_HEALTH, 70 ).build()),
    SNIPER   ("sniper",    Util.getResource("textures/entity/sniper.png"),    AttributeSupplier.builder().add(Attributes.MAX_HEALTH, 50 ).build()),
    MEDIC    ("medic",     Util.getResource("textures/entity/medic.png"),     AttributeSupplier.builder().add(Attributes.MAX_HEALTH, 70 ).build()),
    SCOUT    ("scout",     Util.getResource("textures/entity/scout.png"),     AttributeSupplier.builder().add(Attributes.MAX_HEALTH, 60 ).build()),
    SPY      ("spy",       Util.getResource("textures/entity/spy.png"),       AttributeSupplier.builder().add(Attributes.MAX_HEALTH, 60 ).build());

    private static final Random RNG = new Random();
    private static final Specialty[] VALUES = values();
    private static final int SIZE = VALUES.length;

    private final String name;
    private final ResourceLocation skin;
    private final AttributeSupplier attributes;

    Specialty(String name, ResourceLocation skin, AttributeSupplier supplier) {
        this.name = name;
        this.skin = skin;
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

    public AttributeSupplier getAttributes() {
        return this.attributes;
    }
}
