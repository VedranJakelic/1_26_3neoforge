package net.beamex.shamaschizm.stag.client;
import java.util.ArrayList;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.stag.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
@EventBusSubscriber(modid=Shamaschizm.MOD_ID,value=Dist.CLIENT)
public final class StagClient {
 @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(StagRegistration.STAG,StagRenderer::new);}
 @SubscribeEvent public static void layers(EntityRenderersEvent.RegisterLayerDefinitions e){e.registerLayerDefinition(StagModel.LAYER,StagModel::layer);e.registerLayerDefinition(AntlerModel.LAYER,AntlerModel::layer);}
 @SuppressWarnings({"rawtypes","unchecked"})
 @SubscribeEvent public static void armor(EntityRenderersEvent.AddLayers e){
  for(var skin:e.getSkins()){
   var renderer=e.getPlayerRenderer(skin);if(renderer!=null)renderer.addLayer(new AntlerLayer(renderer,e.getEntityModels().bakeLayer(AntlerModel.LAYER)));
   var mannequin=e.getMannequinRenderer(skin);if(mannequin!=null)mannequin.addLayer(new AntlerLayer(mannequin,e.getEntityModels().bakeLayer(AntlerModel.LAYER)));
  }
  for(var type:e.getEntityTypes())if(e.getRenderer(type) instanceof LivingEntityRenderer renderer)
   renderer.addLayer(new AntlerLayer(renderer,e.getEntityModels().bakeLayer(AntlerModel.LAYER)));
 }
 @SubscribeEvent public static void sight(ClientTickEvent.Post e){
  var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null||mc.isPaused())return;
  var camera=mc.gameRenderer.mainCamera();if(!camera.isInitialized())return;
  var frustum=camera.getCullFrustum();if(frustum==null)return;
  var entries=new ArrayList<StagSight.Entry>();
  for(StagEntity stag:mc.level.getEntitiesOfClass(StagEntity.class,mc.player.getBoundingBox().inflate(160))){
   if(entries.size()>=128)break;
   // Use the normal form's bounds for a stable visibility boundary on transformation.
   entries.add(new StagSight.Entry(stag.getId(),frustum.isVisible(stag.getBoundingBox().expandTowards(0,0.6,0).inflate(0.2))));
  }
  if(!entries.isEmpty())ClientPacketDistributor.sendToServer(new StagSight.Report(entries));
 }
}
