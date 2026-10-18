package net.beamex.shamaschizm.illusionist.client;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.illusionist.Illusionists;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
@EventBusSubscriber(modid=Shamaschizm.MOD_ID,value=Dist.CLIENT)
public final class IllusionistClient {
 @SubscribeEvent public static void renderer(EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(Illusionists.TYPE,IllusionistRenderer::new);e.registerEntityRenderer(Illusionists.LEGACY_TYPE,IllusionistRenderer::new);}
}
