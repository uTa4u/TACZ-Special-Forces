package su.uTa4u.specialforces.capabilities.observation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullConsumer;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.uTa4u.specialforces.SpecialForces;
import su.uTa4u.specialforces.Util;
import su.uTa4u.specialforces.capabilities.ModCapabilities;
import su.uTa4u.specialforces.entities.SwatEntity;

@Mod.EventBusSubscriber(modid = SpecialForces.MOD_ID)
public class ObservationManager {
    private static final ResourceLocation IDENTIFIER = Util.getResource("observation");

    @SubscribeEvent
    public static void onPlayerTickEvent(TickEvent.PlayerTickEvent event) {
        if (!(event.player.level() instanceof ServerLevel serverLevel) || event.phase != TickEvent.Phase.START) return;
        ifCapPresent(event.player, (cap) -> {
            // TODO: extract to tick method in Observation
            if (event.player.tickCount - cap.getLastSpawnTick() >= Observation.SPAWN_COOLDOWN) {
                cap.setLastSpawnTick(event.player.tickCount);
                if (cap.getSpawnCount() >= Observation.MAX_SPAWN_COUNT) return;

                SwatEntity entity = SwatEntity.forPlayer(event.player);

                entity.setPos(event.player.position());
                Vec3 pos = LandRandomPos.getPos(entity, 127, 15);
                if (pos == null) return;

                entity.setPos(pos);
                entity.setTarget(event.player);
                ForgeEventFactory.onFinalizeSpawn(entity, serverLevel, serverLevel.getCurrentDifficultyAt(entity.getOnPos()), MobSpawnType.MOB_SUMMONED, null, null);
                if (serverLevel.addFreshEntity(entity)) {
                    cap.setSpawnCount(cap.getSpawnCount() + 1);
                }

                // If no Missions is running currently:
                // Select Mission
                // Start Mission
                // If Mission is running currently:
                // Send reinforcements
                // event.player.displayClientMessage(Component.literal("Time to start a new Mission!"), false);
            }
        });
    }

    @SubscribeEvent
    public static void onRightClickBlockEvent(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide || event.getHand() != InteractionHand.MAIN_HAND) return;
        BlockPos pos = event.getPos();
        Block block = level.getBlockState(pos).getBlock();
        if (Observation.isObserved(block)) {
            ifCapPresent(event.getEntity(), (cap) -> cap.observe(block, pos));
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(PlayerEvent.Clone event) {
        if (event.getEntity().level().isClientSide) return;
        Player oldPlayer = event.getOriginal();
        oldPlayer.reviveCaps();
        ifCapPresent(event.getEntity(), (cap) -> ifCapPresent(oldPlayer, cap::copy));
    }

    @SubscribeEvent
    public static void onAttachEntityCapabilityEvent(AttachCapabilitiesEvent<Entity> event) {
        if (!(event.getObject() instanceof Player)) return;

        IObservation scoutDataCap = new Observation();
        LazyOptional<IObservation> scoutDataCapOpt = LazyOptional.of(() -> scoutDataCap);
        Capability<IObservation> capability = ModCapabilities.PLAYER_OBSERVATION;

        ICapabilityProvider provider = new ICapabilitySerializable<CompoundTag>() {

            @Override
            public CompoundTag serializeNBT() {
                return scoutDataCap.serializeNBT();
            }

            @Override
            public void deserializeNBT(CompoundTag nbt) {
                scoutDataCap.deserializeNBT(nbt);
            }

            @NotNull
            @Override
            public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction direction) {
                return cap == capability ? scoutDataCapOpt.cast() : LazyOptional.empty();
            }
        };

        event.addCapability(IDENTIFIER, provider);
    }

    public static void ifCapPresent(Player player, NonNullConsumer<? super IObservation> consumer) {
        player.getCapability(ModCapabilities.PLAYER_OBSERVATION).ifPresent(consumer);
    }

    private ObservationManager() {
    }
}
