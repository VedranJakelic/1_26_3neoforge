package net.beamex.shamaschizm.event.client;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.world.dungeon.MossRoomZoneRegistration;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = Shamaschizm.MOD_ID, value = Dist.CLIENT)
public final class MossRoomZoneClientRegistration {
    private MossRoomZoneClientRegistration() {}

    @SubscribeEvent
    public static void registerRenderer(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MossRoomZoneRegistration.MOSS_ROOM_ZONE, NoopRenderer::new);
    }
}
