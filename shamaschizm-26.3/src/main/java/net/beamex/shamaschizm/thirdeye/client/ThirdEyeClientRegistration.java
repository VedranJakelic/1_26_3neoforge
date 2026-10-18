package net.beamex.shamaschizm.thirdeye.client;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.thirdeye.ThirdEyeRegistration;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = Shamaschizm.MOD_ID, value = Dist.CLIENT)
public final class ThirdEyeClientRegistration {
    private ThirdEyeClientRegistration() {}

    @SubscribeEvent
    public static void registerRenderer(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                ThirdEyeRegistration.BLOCK_ENTITY, ThirdEyeRenderer::new);
    }
}
