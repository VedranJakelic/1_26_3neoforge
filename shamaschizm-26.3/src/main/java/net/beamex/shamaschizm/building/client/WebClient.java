package net.beamex.shamaschizm.building.client;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.building.BuildingRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

@EventBusSubscriber(modid=Shamaschizm.MOD_ID,value=Dist.CLIENT)
public final class WebClient {
    private static boolean hadWorld;
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){
        event.registerBlockEntityRenderer(BuildingRegistration.WEB_BLOCK_ENTITY,WebBlockRenderer::new);
        // Compatibility display only, while an old saved entity is being converted into blocks.
        event.registerEntityRenderer(BuildingRegistration.WEB_ENTITY,SpanningWebRenderer::new);
    }
    @SubscribeEvent public static void reload(AddClientReloadListenersEvent event){
        event.addListener(Shamaschizm.id("spanning_web_textures"),(ResourceManagerReloadListener)manager->WebTextures.clear());
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        boolean world=Minecraft.getInstance().level!=null;
        if(hadWorld&&!world)WebTextures.clear();hadWorld=world;
    }
    private WebClient(){}
}
