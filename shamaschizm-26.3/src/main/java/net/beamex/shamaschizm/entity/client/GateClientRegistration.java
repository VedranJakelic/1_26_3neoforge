package net.beamex.shamaschizm.entity.client;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.entity.GateRegistration;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = Shamaschizm.MOD_ID, value = Dist.CLIENT)
public final class GateClientRegistration {
    private GateClientRegistration() {}

    @SubscribeEvent
    public static void registerRenderer(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(GateRegistration.GATE, GateRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayer(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(GateModel.LAYER_LOCATION, GateModel::createBodyLayer);
    }
}
