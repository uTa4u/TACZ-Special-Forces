package su.uTa4u.specialforces;

import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import su.uTa4u.specialforces.client.ModModelLayers;
import su.uTa4u.specialforces.client.models.SwatModel;
import su.uTa4u.specialforces.client.renderers.SwatRenderer;
import su.uTa4u.specialforces.entities.ModEntities;
import su.uTa4u.specialforces.entities.SwatEntity;
import su.uTa4u.specialforces.items.ModItems;

@Mod(SpecialForces.MOD_ID)
public class SpecialForces {
    public static final String MOD_ID = "taczsf";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SpecialForces(FMLJavaModLoadingContext context) {
        IEventBus eventBus = context.getModEventBus();

        ModItems.ITEMS.register(eventBus);
        ModEntities.ENTITY_TYPES.register(eventBus);
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModEventBusClientEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            EntityRenderers.register(ModEntities.SWAT_ENTITY.get(), SwatRenderer::new);
        }

        @SubscribeEvent
        public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(ModModelLayers.SWAT, SwatModel::createBodyLayer);
        }

        @SubscribeEvent
        public static void onCreativeModeTabContent(BuildCreativeModeTabContentsEvent event) {
            if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
                event.accept(ModItems.SWAT_SPAWN_EGG);
            }
        }

        @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
        public static class ModEventBusEvents {
            @SubscribeEvent
            public static void onEntityAttributeCreationEvent(EntityAttributeCreationEvent event) {
                event.put(ModEntities.SWAT_ENTITY.get(), SwatEntity.createDefaultAttributes().build());
            }

        }
    }
}
