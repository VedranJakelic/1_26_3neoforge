package net.beamex.shamaschizm.event.client;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.world.dungeon.GardenZoneRegistration;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = Shamaschizm.MOD_ID, value = Dist.CLIENT)
public final class GardenZoneClientRegistration {
    private GardenZoneClientRegistration() {}

    @SubscribeEvent
    public static void registerRenderer(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(GardenZoneRegistration.GARDEN_ZONE, NoopRenderer::new);
    }
}
