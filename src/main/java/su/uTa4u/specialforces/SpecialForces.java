package su.uTa4u.specialforces;

import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.world.item.Items;
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
import su.uTa4u.specialforces.client.renderers.SpecialForceRenderer;
import su.uTa4u.specialforces.entities.ModEntities;
import su.uTa4u.specialforces.entities.SwatEntity;
import su.uTa4u.specialforces.items.ModItems;

@Mod(SpecialForces.MOD_ID)
public class SpecialForces
{
    public static final String MOD_ID = "taczsf";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SpecialForces(FMLJavaModLoadingContext context)
    {
        IEventBus eventBus = context.getModEventBus();

        ModItems.ITEMS.register(eventBus);
        ModEntities.ENTITY_TYPES.register(eventBus);
    }

    public static void onCreativeModeTansContent(BuildCreativeModeTabContentsEvent event) {
        // yes, this is how I find the creative tab I need
        if (event.getTab().getIconItem().is(Items.PIG_SPAWN_EGG)) {
            event.accept(ModItems.SWAT_SPAWN_EGG);
        }
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModEventBusClientEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            EntityRenderers.register(ModEntities.TEST_ENTITY.get(), SpecialForceRenderer::new);
        }

        @SubscribeEvent
        public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(ModModelLayers.TEST_MODEL, SwatModel::createBodyLayer);
        }
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEventBusEvents {
        @SubscribeEvent
        public static void onEntityAttributeCreationEvent(EntityAttributeCreationEvent event) {
            event.put(ModEntities.TEST_ENTITY.get(), SwatEntity.createDefaultAttributes().build());
        }
    }
}
