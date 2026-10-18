package net.beamex.shamaschizm.earthworm.client;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.earthworm.Earthworms;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
@EventBusSubscriber(modid=Shamaschizm.MOD_ID,value=Dist.CLIENT)
public final class EarthwormClient {
 @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(Earthworms.TYPE,EarthwormRenderer::new);}
}
