package su.uTa4u.specialforces;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;
import su.uTa4u.specialforces.config.CommonConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum Mission {
    // Observe player's actions
    SCOUTING("scouting"),
    // Rescue entities observed during Scouting
    RESCUE("rescue"),
    // Take blocks or items from containers observed during Scouting
    RAID("raid"),
    // Build defensive positions around the player's base and kill on sight
    SIEGE("siege"),
    // Target player's respawn point
    ARREST("arrest"),
    // Destroy redstone contraptions, rails, place mines etc.
    SABOTAGE("sabotage");

    private static final Mission[] VALUES = values();

    private static final Map<String, Mission> MISSION_BY_NAME = new HashMap<>();

    private final String name;
    private final MutableComponent message;
    private List<Specialty> participants;

    Mission(String name) {
        this.name = name;
        this.message = Component.translatable("mission.taczsf.msg." + name);
    }

    public String getName() {
        return this.name;
    }

    public MutableComponent getMessage() {
        return this.message;
    }

    public List<Specialty> getParticipants() {
        return this.participants;
    }

    @Nullable
    public static Mission byName(String name) {
        return MISSION_BY_NAME.get(name);
    }

    public static void loadParticipantsFromConfig() {
        for (Mission mission : VALUES) {
            mission.participants = CommonConfig.MISSION_PARTICIPANTS.get(mission).get().stream().map(Specialty::byName).toList();
        }
    }

    static {
        for (Mission mission : VALUES) {
            MISSION_BY_NAME.put(mission.name, mission);
        }
    }
}
