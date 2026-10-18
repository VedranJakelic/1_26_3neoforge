package net.beamex.shamaschizm.entity.client;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.entity.CoffinRegistration;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = Shamaschizm.MOD_ID, value = Dist.CLIENT)
public final class CoffinClientRegistration {
    private CoffinClientRegistration() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(CoffinRegistration.COFFIN, CoffinRenderer::new);
        event.registerEntityRenderer(CoffinRegistration.SPELL, SpellVisualRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(CoffinModel.LAYER_LOCATION, CoffinModel::createBodyLayer);
        event.registerLayerDefinition(SpellVisualModel.LAYER_LOCATION, SpellVisualModel::createBodyLayer);
    }
}
