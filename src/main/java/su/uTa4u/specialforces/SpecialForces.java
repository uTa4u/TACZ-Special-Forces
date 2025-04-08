package su.uTa4u.specialforces;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.geom.LayerDefinitions;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import su.uTa4u.specialforces.capabilities.observation.Observation;
import su.uTa4u.specialforces.client.ModModelLayers;
import su.uTa4u.specialforces.client.models.SwatModel;
import su.uTa4u.specialforces.client.renderers.SwatRenderer;
import su.uTa4u.specialforces.client.screens.SwatCorpseScreen;
import su.uTa4u.specialforces.config.CommonConfig;
import su.uTa4u.specialforces.entities.ModEntities;
import su.uTa4u.specialforces.entities.SwatEntity;
import su.uTa4u.specialforces.glms.ModLootModifiers;
import su.uTa4u.specialforces.items.ModItems;
import su.uTa4u.specialforces.menus.ModMenuTypes;

@Mod(SpecialForces.MOD_ID)
public class SpecialForces {
    public static final String MOD_ID = "taczsf";
    // private static final Logger LOGGER = LogUtils.getLogger();

    public SpecialForces(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();

        ModItems.ITEMS.register(modBus);
        ModEntities.ENTITY_TYPES.register(modBus);
        ModMenuTypes.MENU_TYPES.register(modBus);
        ModLootModifiers.LOOT_MODIFIER_SERIALIZERS.register(modBus);

        context.registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC);

    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModEventBusClientEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            EntityRenderers.register(ModEntities.SWAT_ENTITY.get(), SwatRenderer::new);
            MenuScreens.register(ModMenuTypes.SWAT_CORPSE.get(), SwatCorpseScreen::new);
        }

        @SubscribeEvent
        public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(ModModelLayers.SWAT, SwatModel::createBodyLayer);
            event.registerLayerDefinition(ModModelLayers.SWAT_INNER_ARMOR, () -> LayerDefinition.create(HumanoidArmorModel.createBodyLayer(LayerDefinitions.INNER_ARMOR_DEFORMATION), 64, 32));
            event.registerLayerDefinition(ModModelLayers.SWAT_OUTER_ARMOR, () -> LayerDefinition.create(HumanoidArmorModel.createBodyLayer(LayerDefinitions.OUTER_ARMOR_DEFORMATION), 64, 32));
        }

        @SubscribeEvent
        public static void onCreativeModeTabContent(BuildCreativeModeTabContentsEvent event) {
            if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
                event.accept(ModItems.SWAT_SPAWN_EGG);
            }
        }

    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEventBusEvents {
        @SubscribeEvent
        public static void onEntityAttributeCreationEvent(EntityAttributeCreationEvent event) {
            event.put(ModEntities.SWAT_ENTITY.get(), SwatEntity.createDefaultAttributes().build());
        }

        @SubscribeEvent
        public static void onModConfigLoadingEvent(ModConfigEvent.Loading event) {
            if (event.getConfig().getSpec() == CommonConfig.SPEC) {
                Specialty.loadAttributesFromConfig();
                Mission.loadParticipantsFromConfig();
                Observation.loadTargetsFromConfig();
            }
        }

        @SubscribeEvent
        public static void onModConfigReloadingEvent(ModConfigEvent.Reloading event) {
            if (event.getConfig().getSpec() == CommonConfig.SPEC) {
                Specialty.loadAttributesFromConfig();
                Mission.loadParticipantsFromConfig();
                Observation.loadTargetsFromConfig();
            }
        }
    }
}
