package su.uTa4u.specialforces.entities;

import net.minecraft.network.syncher.EntityDataSerializer;
import su.uTa4u.specialforces.Specialty;
import net.minecraft.network.syncher.EntityDataSerializers;

public class ModEntityDataSerializers {
    public static final EntityDataSerializer<Specialty> SPECIAL_FORCE_SPECIALTY;

    static {
        SPECIAL_FORCE_SPECIALTY = EntityDataSerializer.simpleEnum(Specialty.class);
        EntityDataSerializers.registerSerializer(SPECIAL_FORCE_SPECIALTY);
    }
}
