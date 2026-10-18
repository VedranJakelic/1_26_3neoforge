package net.beamex.shamaschizm.corpse.client;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.corpse.CorpseSkeletonRegistration;
import net.beamex.shamaschizm.world.Schizm;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = Shamaschizm.MOD_ID, value = Dist.CLIENT)
public final class CorpseSkeletonClient {
    private static Integer previousRenderDistance;
    private CorpseSkeletonClient() {}

    @SubscribeEvent
    public static void registerRenderer(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(CorpseSkeletonRegistration.TYPE, CorpseSkeletonRenderer::new);
    }

    @SubscribeEvent
    public static void capSchizmRenderDistance(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        boolean inSchizm = mc.player != null && mc.level != null
                && mc.level.dimension().equals(Schizm.KEY);
        int current = mc.options.renderDistance().get();
        if (inSchizm) {
            if (current > 8) {
                if (previousRenderDistance == null) previousRenderDistance = current;
                mc.options.renderDistance().set(8);
            }
        } else if (previousRenderDistance != null) {
            mc.options.renderDistance().set(previousRenderDistance);
            previousRenderDistance = null;
        }
    }
}
