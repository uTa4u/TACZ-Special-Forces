package su.uTa4u.specialforces;

import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import su.uTa4u.specialforces.client.ModModelLayers;
import su.uTa4u.specialforces.client.models.TestModel;
import su.uTa4u.specialforces.client.renderers.TestRenderer;
import su.uTa4u.specialforces.entities.ModEntities;
import su.uTa4u.specialforces.entities.TestEntity;

@Mod(SpecialForces.MODID)
public class SpecialForces
{
    public static final String MODID = "taczsf";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SpecialForces(FMLJavaModLoadingContext context)
    {
        IEventBus eventBus = context.getModEventBus();

        ModEntities.ENTITY_TYPES.register(eventBus);
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModEventBusClientEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            EntityRenderers.register(ModEntities.TEST_ENTITY.get(), TestRenderer::new);
        }

        @SubscribeEvent
        public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(ModModelLayers.TEST_MODEL, TestModel::createBodyLayer);
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEventBusEvents {
        @SubscribeEvent
        public static void onEntityAttributeCreationEvent(EntityAttributeCreationEvent event) {
            event.put(ModEntities.TEST_ENTITY.get(), TestEntity.createAttributes().build());
        }
    }
}
