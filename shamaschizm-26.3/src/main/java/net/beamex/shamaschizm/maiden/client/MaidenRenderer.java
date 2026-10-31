package net.beamex.shamaschizm.maiden.client;
import com.mojang.blaze3d.vertex.PoseStack;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.maiden.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.joml.*;
@EventBusSubscriber(modid="shamaschizm",value=Dist.CLIENT)
public final class MaidenRenderer extends EntityRenderer<IronMaiden,MaidenRenderer.State>{
 public static final class State extends EntityRenderState{Matrix4f[] pose;boolean skeleton;int overlay;}
 public MaidenRenderer(EntityRendererProvider.Context c){super(c);shadowRadius=.5F;}
 @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(MaidenRegistration.TYPE,MaidenRenderer::new);e.registerEntityRenderer(MaidenRegistration.BONE,MaidenBoneRenderer::new);}
 @Override public State createRenderState(){return new State();}
 @Override protected AABB getBoundingBoxForCulling(IronMaiden e,float p){return e.getBoundingBox().inflate(1.2);}
 @Override public void extractRenderState(IronMaiden e,State s,float p){super.extractRenderState(e,s,p);s.pose=MaidenMotion.matrices(net.minecraft.util.Mth.rotLerp(p,e.yRotO,e.getYRot()),e.pose(p));s.skeleton=e.skeletonInside();s.overlay=OverlayTexture.pack(0.0F,e.hurtTime>0||e.deathTime>0);}
 @Override public void submit(State s,PoseStack p,SubmitNodeCollector c,CameraRenderState camera){
  super.submit(s,p,c,camera);
  c.submitCustomGeometry(p,RenderTypes.entityCutout(Shamaschizm.id("textures/entity/iron_maiden.png")),(pose,vertices)->{
   Matrix3f[] normals=new Matrix3f[5];for(int i=0;i<5;i++)normals[i]=s.pose[i].normal(new Matrix3f());
   for(var q:MaidenGeometry.QUADS){var n=normals[q.part()].transform(new Vector3f(q.normal())).normalize();for(var v:q.vertices()){var at=s.pose[q.part()].transformPosition(new Vector3f(v[0],v[1],v[2]));vertices.addVertex(pose,at.x,at.y,at.z).setColor(-1).setUv(v[3],v[4]).setOverlay(s.overlay).setLight(s.lightCoords).setNormal(pose,n.x,n.y,n.z);}}
  });
  if(s.skeleton)for(int i=0;i<MaidenBones.PARTS.length;i++)BoneMesh.submit(i,s.pose[0],false,p,c,s.lightCoords);
 }
}
