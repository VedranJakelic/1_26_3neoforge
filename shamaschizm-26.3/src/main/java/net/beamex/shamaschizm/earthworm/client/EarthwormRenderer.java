package net.beamex.shamaschizm.earthworm.client;
import com.mojang.blaze3d.vertex.PoseStack;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.earthworm.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

/** Independent world-space segments: vanilla body/head rotations and walking bob never apply. */
public final class EarthwormRenderer extends EntityRenderer<EarthwormEntity,EarthwormRenderer.State> {
 public EarthwormRenderer(EntityRendererProvider.Context context){super(context);shadowRadius=0;}
 public static final class State extends EntityRenderState {
  public final Vector3f[] centers=new Vector3f[9],back=new Vector3f[9];
  public final float[] stretch=new float[9];
 }
 @Override public State createRenderState(){return new State();}
 @Override public void extractRenderState(EarthwormEntity e,State s,float partial){
  super.extractRenderState(e,s,partial);
  float time=e.animationTime(partial);
  for(int i=0;i<9;i++){s.centers[i]=e.part(i,partial);s.stretch[i]=WormMotion.stretch(i,time);}
  for(int i=0;i<9;i++){
   Vector3f t=new Vector3f(s.centers[Math.min(8,i+1)]).sub(s.centers[Math.max(0,i-1)]);
   if(t.lengthSquared()<1e-8)t.set(0,0,1);else t.normalize();s.back[i]=t;
  }
 }
 @Override protected AABB getBoundingBoxForCulling(EarthwormEntity e,float partial){return e.getBoundingBox().inflate(1.2);}
 @Override public void submit(State s,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){
  super.submit(s,pose,collector,camera);
  collector.submitCustomGeometry(pose,RenderTypes.entityCutout(Shamaschizm.id("textures/entity/earthworm.png")),(matrix,buffer)->{
   for(float[] v:WormMesh.VERTICES){
    int i=(int)v[0];Vector3f b=s.back[i],c=s.centers[i];
    Vector3f right=new Vector3f(b.z,0,-b.x);if(right.lengthSquared()<1e-8)right.set(1,0,0);else right.normalize();
    Vector3f up=new Vector3f(b).cross(right).normalize();
    float x=v[1],y=v[2],z=v[3]*s.stretch[i];
    float px=c.x+right.x*x+up.x*y+b.x*z,py=c.y+right.y*x+up.y*y+b.y*z+.001F,pz=c.z+right.z*x+up.z*y+b.z*z;
    float nx=right.x*v[6]+up.x*v[7]+b.x*v[8],ny=right.y*v[6]+up.y*v[7]+b.y*v[8],nz=right.z*v[6]+up.z*v[7]+b.z*v[8];
    buffer.addVertex(matrix,px,py,pz).setColor(-1).setUv(v[4],v[5]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(s.lightCoords).setNormal(matrix,nx,ny,nz);
   }
  });
 }
}
