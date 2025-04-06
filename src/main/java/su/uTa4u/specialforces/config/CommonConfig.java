package su.uTa4u.specialforces.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.registries.ForgeRegistries;
import su.uTa4u.specialforces.Mission;
import su.uTa4u.specialforces.Specialty;
import su.uTa4u.specialforces.Util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CommonConfig {

    public static final ForgeConfigSpec SPEC = init();

    public static ForgeConfigSpec.IntValue OBSERVATION_TICK_COOLDOWN;
    public static ForgeConfigSpec.IntValue OBSERVATION_SQUAD_COUNT;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> OBSERVATION_BLOCK_TARGETS;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> OBSERVATION_ENTITY_TARGETS;

    public static ForgeConfigSpec.IntValue GUN_ATTACK_COOLDOWN;

    public static ForgeConfigSpec.DoubleValue SWAT_ENTITY_EFFECTIVE_RANGE_MULT;
    public static ForgeConfigSpec.IntValue SWAT_ENTITY_SQUAD_SUMMON_COOLDOWN;
    public static ForgeConfigSpec.IntValue SWAT_ENTITY_DEAD_BODY_LIFESPAN;

    public static Map<Mission, ForgeConfigSpec.ConfigValue<List<? extends String>>> MISSION_PARTICIPANTS;

    public static Map<Specialty, Map<Attribute, ForgeConfigSpec.DoubleValue>> SPECIALTY_ATTRIBUTES;

    private static ForgeConfigSpec init() {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("Observation");

        OBSERVATION_TICK_COOLDOWN = builder
                .comment("Time between selecting missions and spawning mission commanders,")
                .comment("in ticks (20 ticks = 1 second)")
                .comment("Default: 24000")
                .defineInRange("tickCooldown", 24000, 1, Integer.MAX_VALUE);
        OBSERVATION_SQUAD_COUNT = builder
                .comment("How many squads (mission commanders) can exist at the same time.")
                .comment("I wouldn't recommend setting this all the way to the max...")
                .comment("Default: 3")
                .defineInRange("squadCount", 3, 1, 128);
        OBSERVATION_BLOCK_TARGETS = builder
                .comment("List of blocks that swat will take note of when player interacts with them.")
                .comment("Will be used in missions such as raid")
                .comment("Example: `minecraft:chest`, `minecraft:furnace`")
                .comment("Default: []")
                .defineListAllowEmpty("blockTargets", List.of(), (id) -> ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse((String) id)) != null);
        OBSERVATION_ENTITY_TARGETS = builder
                .comment("List of entity types that swat will take note of when player interacts with them.")
                .comment("Will be used in missions such as rescue")
                .comment("Example: `minecraft:villager`, `minecraft:cow`")
                .comment("Default: []")
                .defineListAllowEmpty("entityTargets", List.of(), (id) -> ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.parse((String) id)) != null);

        builder.pop();

        builder.push("GunAttack");

        GUN_ATTACK_COOLDOWN = builder
                .comment("Time between gun attacks,")
                .comment("in ticks (20 ticks = 1 second)")
                .comment("Default: 40")
                .defineInRange("cooldown", 40, 1, Integer.MAX_VALUE);

        builder.pop();

        builder.push("SwatEntity");

        SWAT_ENTITY_EFFECTIVE_RANGE_MULT = builder
                .comment("Multiplier for entities weapon's effective range.")
                .comment("Default: 2.0")
                .defineInRange("effectiveRangeMult", 2.0, 0.01, 10.0);
        SWAT_ENTITY_SQUAD_SUMMON_COOLDOWN = builder
                .comment("Time between squad summonings by commander,")
                .comment("in ticks (20 ticks = 1 second)")
                .comment("Default: 6000")
                .defineInRange("squadSummonCooldown", 6000, 1, Integer.MAX_VALUE);
        SWAT_ENTITY_DEAD_BODY_LIFESPAN = builder
                .comment("Time after which dead body will despawn,")
                .comment("in ticks (20 ticks = 1 second)")
                .comment("Default: 6000")
                .defineInRange("deadBodyLifespan", 6000, 1, Integer.MAX_VALUE);

        builder.pop();


        builder.push("Mission");

        MISSION_PARTICIPANTS = new HashMap<>();
        initMission(builder, Mission.SCOUTING, List.of("medic", "scout", "scout", "scout", "scout", "engineer", "spy"));
        initMission(builder, Mission.RESCUE, List.of("medic", "assaulter", "assaulter", "assaulter", "bulldozer"));
        initMission(builder, Mission.RAID, List.of("medic", "assaulter", "assaulter", "assaulter", "assaulter", "bulldozer", "grenadier", "grenadier"));
        initMission(builder, Mission.SIEGE, List.of("medic", "sniper", "sniper", "grenadier", "grenadier", "grenadier", "bulldozer"));
        initMission(builder, Mission.ARREST, List.of("medic", "assaulter", "assaulter", "assaulter", "bulldozer", "bulldozer", "grenadier"));
        initMission(builder, Mission.SABOTAGE, List.of("medic", "spy", "spy", "spy", "engineer", "engineer"));

        builder.pop();


        builder.push("Specialty");

        SPECIALTY_ATTRIBUTES = new HashMap<>();
        initSpecialty(builder, Specialty.COMMANDER,
                90,
                Attributes.FOLLOW_RANGE.getDefaultValue(),
                0.15,
                0.3,
                Attributes.ATTACK_DAMAGE.getDefaultValue(),
                Attributes.ATTACK_KNOCKBACK.getDefaultValue(),
                Attributes.ATTACK_SPEED.getDefaultValue(),
                Attributes.ARMOR.getDefaultValue(),
                Attributes.ARMOR_TOUGHNESS.getDefaultValue()
        );
        initSpecialty(builder, Specialty.ASSAULTER,
                100,
                Attributes.FOLLOW_RANGE.getDefaultValue(),
                0.15,
                0.3,
                Attributes.ATTACK_DAMAGE.getDefaultValue(),
                Attributes.ATTACK_KNOCKBACK.getDefaultValue(),
                Attributes.ATTACK_SPEED.getDefaultValue(),
                Attributes.ARMOR.getDefaultValue(),
                Attributes.ARMOR_TOUGHNESS.getDefaultValue()
        );
        initSpecialty(builder, Specialty.GRENADIER,
                80,
                Attributes.FOLLOW_RANGE.getDefaultValue(),
                0.15,
                0.3,
                Attributes.ATTACK_DAMAGE.getDefaultValue(),
                Attributes.ATTACK_KNOCKBACK.getDefaultValue(),
                Attributes.ATTACK_SPEED.getDefaultValue(),
                Attributes.ARMOR.getDefaultValue(),
                Attributes.ARMOR_TOUGHNESS.getDefaultValue()
        );
        initSpecialty(builder, Specialty.BULLDOZER,
                150,
                Attributes.FOLLOW_RANGE.getDefaultValue(),
                0.5,
                0.15,
                Attributes.ATTACK_DAMAGE.getDefaultValue(),
                Attributes.ATTACK_KNOCKBACK.getDefaultValue(),
                Attributes.ATTACK_SPEED.getDefaultValue(),
                Attributes.ARMOR.getDefaultValue(),
                Attributes.ARMOR_TOUGHNESS.getDefaultValue()
        );
        initSpecialty(builder, Specialty.ENGINEER,
                70,
                Attributes.FOLLOW_RANGE.getDefaultValue(),
                0.15,
                0.3,
                Attributes.ATTACK_DAMAGE.getDefaultValue(),
                Attributes.ATTACK_KNOCKBACK.getDefaultValue(),
                Attributes.ATTACK_SPEED.getDefaultValue(),
                Attributes.ARMOR.getDefaultValue(),
                Attributes.ARMOR_TOUGHNESS.getDefaultValue()
        );
        initSpecialty(builder, Specialty.SNIPER,
                50,
                Attributes.FOLLOW_RANGE.getDefaultValue(),
                0.15,
                0.3,
                Attributes.ATTACK_DAMAGE.getDefaultValue(),
                Attributes.ATTACK_KNOCKBACK.getDefaultValue(),
                Attributes.ATTACK_SPEED.getDefaultValue(),
                Attributes.ARMOR.getDefaultValue(),
                Attributes.ARMOR_TOUGHNESS.getDefaultValue()
        );
        initSpecialty(builder, Specialty.MEDIC,
                70,
                Attributes.FOLLOW_RANGE.getDefaultValue(),
                0.15,
                0.3,
                Attributes.ATTACK_DAMAGE.getDefaultValue(),
                Attributes.ATTACK_KNOCKBACK.getDefaultValue(),
                Attributes.ATTACK_SPEED.getDefaultValue(),
                Attributes.ARMOR.getDefaultValue(),
                Attributes.ARMOR_TOUGHNESS.getDefaultValue()
        );
        initSpecialty(builder, Specialty.SCOUT,
                60,
                Attributes.FOLLOW_RANGE.getDefaultValue(),
                0.05,
                0.5,
                Attributes.ATTACK_DAMAGE.getDefaultValue(),
                Attributes.ATTACK_KNOCKBACK.getDefaultValue(),
                Attributes.ATTACK_SPEED.getDefaultValue(),
                Attributes.ARMOR.getDefaultValue(),
                Attributes.ARMOR_TOUGHNESS.getDefaultValue()
        );
        initSpecialty(builder, Specialty.SPY,
                60,
                Attributes.FOLLOW_RANGE.getDefaultValue(),
                0.15,
                0.3,
                Attributes.ATTACK_DAMAGE.getDefaultValue(),
                Attributes.ATTACK_KNOCKBACK.getDefaultValue(),
                Attributes.ATTACK_SPEED.getDefaultValue(),
                Attributes.ARMOR.getDefaultValue(),
                Attributes.ARMOR_TOUGHNESS.getDefaultValue()
        );

        builder.pop();


        return builder.build();
    }

    private static void initMission(ForgeConfigSpec.Builder builder, Mission mission, List<String> participants) {
        builder.push(Util.capitalizeFirstLetter(mission.getName()));

        MISSION_PARTICIPANTS.put(mission, builder
                .comment("Allowed values: `commander`, `assaulter`, `grenadier`, `bulldozer`, `engineer`, `sniper`, `medic`, `scout`, `spy`")
                .comment("Default: " + participants)
                .defineList("Participants", participants, (name) -> Specialty.byName((String) name) != null)
        );

        builder.pop();
    }

    private static void initSpecialty(ForgeConfigSpec.Builder builder, Specialty spec, double maxHealth, double followRange, double knockbackResistance, double movementSpeed, double attackDamage, double attackKnockback, double attackSpeed, double armor, double armorToughness) {
        builder.push(Util.capitalizeFirstLetter(spec.getName()));

        Map<Attribute, ForgeConfigSpec.DoubleValue> attributes = new HashMap<>();
        // Min and Max values taken from net.minecraft.world.entity.ai.attributes.Attributes
        attributes.put(Attributes.MAX_HEALTH, builder
                .comment("Default: " + maxHealth)
                .defineInRange("maxHealth", maxHealth, 1.0, 1024.0));
        attributes.put(Attributes.FOLLOW_RANGE, builder
                .comment("Default: " + followRange)
                .defineInRange("followRange", followRange, 0.0, 2048.0));
        attributes.put(Attributes.KNOCKBACK_RESISTANCE, builder
                .comment("Default: " + knockbackResistance)
                .defineInRange("knockbackResistance", knockbackResistance, 0.0, 1.0));
        attributes.put(Attributes.MOVEMENT_SPEED, builder
                .comment("Default: " + movementSpeed)
                .defineInRange("movementSpeed", movementSpeed, 0.0, 1024.0));
        attributes.put(Attributes.ATTACK_DAMAGE, builder
                .comment("Default: " + attackDamage)
                .defineInRange("attackDamage", attackDamage, 0.0, 2048.0));
        attributes.put(Attributes.ATTACK_KNOCKBACK, builder
                .comment("Default: " + attackKnockback)
                .defineInRange("attackKnockback", attackKnockback, 0.0, 5.0));
        attributes.put(Attributes.ATTACK_SPEED, builder
                .comment("Default: " + attackSpeed)
                .defineInRange("attackSpeed", attackSpeed, 0.0, 1024.0));
        attributes.put(Attributes.ARMOR, builder
                .comment("Default: " + armor)
                .defineInRange("armor", armor, 0.0, 30.0));
        attributes.put(Attributes.ARMOR_TOUGHNESS, builder
                .comment("Default: " + armorToughness)
                .defineInRange("armorToughness", armorToughness, 0.0, 20.0));
        SPECIALTY_ATTRIBUTES.put(spec, attributes);

        builder.pop();
    }

    private CommonConfig() {
    }
}
