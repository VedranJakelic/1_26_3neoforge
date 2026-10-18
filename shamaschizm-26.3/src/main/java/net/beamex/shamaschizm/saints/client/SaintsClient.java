package net.beamex.shamaschizm.saints.client;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.saints.CatacombSaints;
import net.beamex.shamaschizm.saints.CatacombEncounterEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.TippableArrowRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
@EventBusSubscriber(modid=Shamaschizm.MOD_ID,value=Dist.CLIENT)
public final class SaintsClient {
    @SubscribeEvent public static void hud(net.neoforged.neoforge.client.event.RegisterGuiLayersEvent event){
        event.registerAboveAll(Shamaschizm.id("saints_boss_bars"),SaintsHud::render);
        event.wrapLayer(net.neoforged.neoforge.client.gui.VanillaGuiLayers.BOSS_OVERLAY,original->(graphics,delta)->{
            boolean shifted=SaintsHud.active()!=null;
            if(shifted){graphics.pose().pushMatrix();graphics.pose().translate(0,66);}
            original.render(graphics,delta);
            if(shifted)graphics.pose().popMatrix();
        });
    }
    @SubscribeEvent public static void specialModels(net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent event){
        event.register(Shamaschizm.id("coffin_lid"),CoffinLidSpecialRenderer.Unbaked.CODEC);
    }
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){
        event.registerEntityRenderer(CatacombSaints.COFFIN2,Coffin2Renderer::new);
        event.registerEntityRenderer(CatacombSaints.ARROW,TippableArrowRenderer::new);
        event.registerEntityRenderer(CatacombSaints.THROWN_SPEAR,SaintSpearRenderer::new);
        event.registerEntityRenderer(CatacombSaints.ENCOUNTER,Invisible::new);
    }
    @SubscribeEvent public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event){event.registerLayerDefinition(Coffin2Model.LAYER,Coffin2Model::layer);}
    private static final class Invisible extends EntityRenderer<CatacombEncounterEntity,EntityRenderState> {
        Invisible(EntityRendererProvider.Context context){super(context);}
        @Override public EntityRenderState createRenderState(){return new EntityRenderState();}
    }
}
