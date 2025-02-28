package su.uTa4u.specialforces;

import net.minecraft.resources.ResourceLocation;

import java.util.Random;

public enum Specialty {
    COMMANDER(util.getResource("textures/entity/commander.png")),
    SNIPER   (util.getResource("textures/entity/sniper.png")),
    ASSAULTER(util.getResource("textures/entity/assaulter.png")),
    GRENADIER(util.getResource("textures/entity/grenadier.png")),
    BULLDOZER(util.getResource("textures/entity/bulldozer.png")),
    MEDIC    (util.getResource("textures/entity/medic.png")),
    SPY      (util.getResource("textures/entity/spy.png")),
    ENGINEER (util.getResource("textures/entity/engineer.png"));

    private static final Random RNG = new Random();
    private static final Specialty[] VALUES = values();
    private static final int SIZE = VALUES.length;

    private final ResourceLocation skin;

    Specialty(ResourceLocation skin) {
        this.skin = skin;
    }

    public static Specialty getRandomSpecialty() {
        return VALUES[RNG.nextInt(SIZE)];
    }

    public ResourceLocation getSkin() {
        return this.skin;
    }
}
