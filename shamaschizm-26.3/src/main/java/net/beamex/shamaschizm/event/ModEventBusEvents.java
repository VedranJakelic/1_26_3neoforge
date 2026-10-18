package net.beamex.shamaschizm.event;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.entity.client.ShamanModel;
import net.beamex.shamaschizm.entity.client.RoachModel;
import net.beamex.shamaschizm.entity.client.BabyRoachModel;
import net.beamex.shamaschizm.entity.client.GiantCentipedeHeadModel;
import net.beamex.shamaschizm.entity.client.RegularCentipedeModel;
import net.beamex.shamaschizm.client.AncientArmorClient;
import net.beamex.shamaschizm.client.AncientArmorModel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = Shamaschizm.MOD_ID, value = Dist.CLIENT)
public final class ModEventBusEvents {
    private ModEventBusEvents() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(net.beamex.shamaschizm.entity.ModEntities.SHAMAN.get(),
                net.beamex.shamaschizm.entity.client.ShamanRenderer::new);
        event.registerEntityRenderer(net.beamex.shamaschizm.entity.ModEntities.ROACH.get(),
                net.beamex.shamaschizm.entity.client.RoachRenderer::new);
        event.registerEntityRenderer(net.beamex.shamaschizm.entity.ModEntities.BABY_ROACH.get(),
                net.beamex.shamaschizm.entity.client.BabyRoachRenderer::new);
        event.registerEntityRenderer(net.beamex.shamaschizm.entity.ModEntities.REGULAR_CENTIPEDE.get(),
                net.beamex.shamaschizm.entity.client.RegularCentipedeRenderer::new);
        event.registerEntityRenderer(net.beamex.shamaschizm.entity.ModEntities.GIANT_CENTIPEDE.get(),
                net.beamex.shamaschizm.entity.client.GiantCentipedeRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayers(final EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ShamanModel.LAYER_LOCATION, ShamanModel::createBodyLayer);
        event.registerLayerDefinition(RoachModel.LAYER_LOCATION, RoachModel::createBodyLayer);
        event.registerLayerDefinition(BabyRoachModel.LAYER_LOCATION, BabyRoachModel::createBodyLayer);
        event.registerLayerDefinition(RegularCentipedeModel.LAYER_LOCATION,
                RegularCentipedeModel::createBodyLayer);
        event.registerLayerDefinition(GiantCentipedeHeadModel.LAYER_LOCATION,
                GiantCentipedeHeadModel::createBodyLayer);
        event.registerLayerDefinition(AncientArmorModel.HELMET_LAYER, AncientArmorModel::createHelmetLayer);
        event.registerLayerDefinition(AncientArmorModel.CHESTPLATE_LAYER, AncientArmorModel::createChestplateLayer);
        event.registerLayerDefinition(AncientArmorModel.LEGGINGS_LAYER, AncientArmorModel::createLeggingsLayer);
        event.registerLayerDefinition(AncientArmorModel.BOOTS_LAYER, AncientArmorModel::createBootsLayer);
        event.registerLayerDefinition(AncientArmorModel.BABY_HELMET_LAYER, AncientArmorModel::createBabyHelmetLayer);
        event.registerLayerDefinition(AncientArmorModel.BABY_CHESTPLATE_LAYER, AncientArmorModel::createBabyChestplateLayer);
        event.registerLayerDefinition(AncientArmorModel.BABY_LEGGINGS_LAYER, AncientArmorModel::createBabyLeggingsLayer);
        event.registerLayerDefinition(AncientArmorModel.BABY_BOOTS_LAYER, AncientArmorModel::createBabyBootsLayer);
    }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        AncientArmorClient.register(event);
    }

    @SubscribeEvent
    public static void bakeAncientArmorModels(EntityRenderersEvent.AddLayers event) {
        AncientArmorClient.bakeModels(event.getEntityModels());
    }
}
