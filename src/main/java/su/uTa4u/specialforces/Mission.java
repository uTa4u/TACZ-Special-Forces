package su.uTa4u.specialforces;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum Mission {
    // Observe player's actions
    SCOUTING("scouting", List.of(
            Specialty.MEDIC,
            Specialty.SCOUT,
            Specialty.SCOUT,
            Specialty.SCOUT,
            Specialty.SCOUT,
            Specialty.ENGINEER,
            Specialty.SPY
    )),
    // Rescue entities observed during Scouting
    RESCUE("rescue", List.of(
            Specialty.MEDIC,
            Specialty.ASSAULTER,
            Specialty.ASSAULTER,
            Specialty.ASSAULTER,
            Specialty.BULLDOZER
    )),
    // Take blocks or items from containers observed during Scouting
    RAID("raid", List.of(
            Specialty.MEDIC,
            Specialty.ASSAULTER,
            Specialty.ASSAULTER,
            Specialty.ASSAULTER,
            Specialty.ASSAULTER,
            Specialty.BULLDOZER,
            Specialty.GRENADIER,
            Specialty.GRENADIER
    )),
    // Build defensive positions around the player's base and kill on sight
    SIEGE("siege", List.of(
            Specialty.MEDIC,
            Specialty.SNIPER,
            Specialty.SNIPER,
            Specialty.GRENADIER,
            Specialty.GRENADIER,
            Specialty.GRENADIER,
            Specialty.BULLDOZER
    )),
    // Target player's respawn point
    ARREST("arrest", List.of(
            Specialty.MEDIC,
            Specialty.ASSAULTER,
            Specialty.ASSAULTER,
            Specialty.ASSAULTER,
            Specialty.BULLDOZER,
            Specialty.BULLDOZER,
            Specialty.GRENADIER
    )),
    // Destroy redstone contraptions, rails, place mines etc.
    SABOTAGE("sabotage", List.of(
            Specialty.MEDIC,
            Specialty.SPY,
            Specialty.SPY,
            Specialty.SPY,
            Specialty.ENGINEER,
            Specialty.ENGINEER
    ));

    private static final Mission[] VALUES = values();

    private static final Map<String, Mission> MISSION_BY_NAME = new HashMap<>();

    private final String name;
    private final MutableComponent message;
    private final List<Specialty> participants;

    Mission(String name, List<Specialty> participants) {
        this.name = name;
        this.message = Component.translatable("mission.taczsf.msg." + name);
        this.participants = participants;
    }

    public String getName() {
        return this.name;
    }

    public MutableComponent getMessage() {
        return this.message;
    }

    public List<Specialty> getParticipants() {
        return new ArrayList<>(this.participants);
    }

    @Nullable
    public static Mission byName(String name) {
        return MISSION_BY_NAME.get(name);
    }

    static {
        for (Mission mission : VALUES) {
            MISSION_BY_NAME.put(mission.name, mission);
        }
    }
}
