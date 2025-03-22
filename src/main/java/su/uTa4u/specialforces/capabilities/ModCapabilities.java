package su.uTa4u.specialforces.capabilities;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import su.uTa4u.specialforces.capabilities.observation.IObservation;

public class ModCapabilities {

    public static final Capability<IObservation> PLAYER_OBSERVATION = CapabilityManager.get(new CapabilityToken<>() {});
}
