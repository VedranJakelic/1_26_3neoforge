package net.beamex.shamaschizm.homunculus.client;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.homunculus.HomunculusRegistration;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
@EventBusSubscriber(modid=Shamaschizm.MOD_ID,value=Dist.CLIENT)
public final class HomunculusClient {
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){event.registerEntityRenderer(HomunculusRegistration.HOMUNCULUS,HomunculusRenderer::new);}
    @SubscribeEvent public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event){
        event.registerLayerDefinition(HomunculusModel.NORMAL,()->HomunculusModel.layer(false));
        event.registerLayerDefinition(HomunculusModel.SLIM,()->HomunculusModel.layer(true));
    }
}
