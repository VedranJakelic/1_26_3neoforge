package net.beamex.shamaschizm.serpent.client;
import java.io.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.serpent.SeaSerpent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

public final class SerpentRenderer extends EntityRenderer<SeaSerpent,SerpentRenderer.State> {
 private final Mesh head=load("serpent1"),bite=load("serpent1_bite"),hold=load("serpent1_biteandhold"),body=load("serpent2"),tail=load("serpent3"),lure=load("lure"),arrow=load("arrow");
 private static final Identifier[] TEX={Shamaschizm.id("textures/entity/serpent/serpent1.png"),Shamaschizm.id("textures/entity/serpent/serpent2.png"),Shamaschizm.id("textures/entity/serpent/serpent3.png")};
 public SerpentRenderer(EntityRendererProvider.Context context){super(context);shadowRadius=0;}
 public static final class State extends EntityRenderState {
  final Vector3f[] points=new Vector3f[SeaSerpent.NODES],backs=new Vector3f[SeaSerpent.NODES];
  final int[] light=new int[SeaSerpent.NODES];
  final ItemStackRenderState[] items=new ItemStackRenderState[10];
  final boolean[] arrows=new boolean[10],empty=new boolean[10];
  float biteTime,headYaw,headPitch;
  boolean holding,hurt,dead;
  State(){for(int i=0;i<10;i++)items[i]=new ItemStackRenderState();}
 }
 @Override public State createRenderState(){return new State();}
 @Override protected AABB getBoundingBoxForCulling(SeaSerpent e,float partial){return e.getBoundingBox().inflate(36);}
 @Override public void extractRenderState(SeaSerpent e,State s,float partial){
  super.extractRenderState(e,s,partial);s.biteTime=e.biteTime(partial);s.holding=e.heldId()>=0;s.dead=e.corpse();s.hurt=e.hurtTime>0;
  for(int i=0;i<SeaSerpent.NODES;i++){
   s.points[i]=e.point(i,partial);
   BlockPos p=BlockPos.containing(e.getX()+s.points[i].x,e.getY()+s.points[i].y,e.getZ()+s.points[i].z);
   s.light[i]=(e.level().getBrightness(LightLayer.BLOCK,p)<<4)|(e.level().getBrightness(LightLayer.SKY,p)<<20);
  }
  for(int i=0;i<SeaSerpent.LURE;i++){Vector3f back=new Vector3f(s.points[Math.min(SeaSerpent.LURE,i+1)]).sub(s.points[Math.max(0,i-1)]);if(back.lengthSquared()<1e-8)back.set(0,0,1);else back.normalize();s.backs[i]=back;}
  s.backs[SeaSerpent.LURE]=new Vector3f(0,0,1);
  s.headYaw=0;s.headPitch=0;
  if(!s.dead){
   var origin=e.position().add(s.points[SeaSerpent.LURE].x,s.points[SeaSerpent.LURE].y+1.75,s.points[SeaSerpent.LURE].z);
   var closest=e.level().getNearestPlayer(origin.x,origin.y,origin.z,32,false);
   if(closest!=null){var d=closest.getEyePosition().subtract(origin);s.headYaw=(float)Math.atan2(-d.x,-d.z);s.headPitch=(float)Math.max(-.6,Math.min(.6,Math.atan2(d.y,Math.sqrt(d.x*d.x+d.z*d.z))));}
  }
  for(int i=0;i<10;i++){
   var item=e.weapon(i);s.empty[i]=item.isEmpty();s.arrows[i]=item.is(Items.ARROW);
   Minecraft.getInstance().getItemModelResolver().updateForNonLiving(s.items[i],item,ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,e);
  }
 }
 @Override public void submit(State s,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){
  super.submit(s,pose,collector,camera);
  Mesh animation=s.biteTime<.05F||s.dead?head:s.holding?hold:bite;
  for(int i=0;i<SeaSerpent.LURE;i++)draw(i==0?animation:i<=SeaSerpent.MAIN_SEGMENTS?body:tail,i==0?s.biteTime:0,TEX[i==0?0:i<=SeaSerpent.MAIN_SEGMENTS?1:2],s,i,pose,collector,-1,false);
  // Drowned UV geometry, tinted dark violet/grey to sit with the serpent palette.
  draw(lure,0,Identifier.withDefaultNamespace("textures/entity/zombie/drowned.png"),s,SeaSerpent.LURE,pose,collector,0xFF786090,true);
  draw(lure,0,Identifier.withDefaultNamespace("textures/entity/zombie/drowned_outer_layer.png"),s,SeaSerpent.LURE,pose,collector,0xFF786090,true);
  for(int i=0;i<10;i++)if(!s.empty[i]){
   int segment=i+1;Vector3f p=s.points[segment];float scale=(float)SeaSerpent.SCALE[segment];
   Vector3f back=s.backs[segment],right=new Vector3f(back.z,0,-back.x);if(right.lengthSquared()<1e-8)right.set(1,0,0);else right.normalize();Vector3f up=new Vector3f(back).cross(right).normalize();
   Vector3f offset=new Vector3f(right).mul(((i%2)*2-1)*.28F*scale).add(new Vector3f(up).mul(.75F*scale));
   pose.pushPose();pose.translate(p.x+offset.x,p.y+offset.y,p.z+offset.z);
   pose.rotate(new org.joml.Quaternionf().setFromNormalized(new org.joml.Matrix3f(right,up,back)));
   pose.rotate(Axis.XP.rotationDegrees((i%3-1)*12));pose.rotate(Axis.ZP.rotationDegrees((i%2==0?1:-1)*13));
   if(s.arrows[i]){
    collector.submitCustomGeometry(pose,RenderTypes.entityCutout(Identifier.withDefaultNamespace("textures/entity/projectiles/arrow.png")),(matrix,buffer)->{
     for(float[] v:arrow.frames[0])buffer.addVertex(matrix,v[0],v[1],v[2]).setColor(-1).setUv(v[3],v[4]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(s.light[segment]).setNormal(matrix,v[5],v[6],v[7]);
    });
   }else{
    pose.translate(0,1.1,0);pose.rotate(Axis.ZP.rotationDegrees(175));pose.rotate(Axis.YP.rotationDegrees(-270));pose.rotate(Axis.XP.rotationDegrees(-5));pose.translate(0,-2F/16,-2F/16);
    s.items[i].submit(pose,collector,s.light[segment],OverlayTexture.NO_OVERLAY,s.outlineColor);
   }
   pose.popPose();
  }
 }
 private void draw(Mesh mesh,float time,Identifier texture,State s,int part,PoseStack pose,SubmitNodeCollector collector,int color,boolean humanoid){
  int frame=Math.min(mesh.frames.length-1,Math.max(0,(int)(time*30)));int next=Math.min(mesh.frames.length-1,frame+1);float blend=Math.min(1,Math.max(0,time*30-frame));
  final float[][] a=mesh.frames[frame],b=mesh.frames[next];
  collector.submitCustomGeometry(pose,RenderTypes.entityCutout(texture),(matrix,buffer)->{
   Vector3f back=s.backs[part],center=s.points[part];Vector3f right=new Vector3f(back.z,0,-back.x);if(right.lengthSquared()<1e-8)right.set(1,0,0);else right.normalize();Vector3f up=new Vector3f(back).cross(right).normalize();
   float scale=(float)SeaSerpent.SCALE[part];
   for(int k=0;k<a.length;k++){
    float[] v=a[k],w=b[k];Vector3f p=new Vector3f(v[0]+(w[0]-v[0])*blend,v[1]+(w[1]-v[1])*blend,v[2]+(w[2]-v[2])*blend);
    Vector3f n=new Vector3f(v[5]+(w[5]-v[5])*blend,v[6]+(w[6]-v[6])*blend,v[7]+(w[7]-v[7])*blend).normalize();
    if(humanoid&&v[8]>0){p.sub(0,1.5F,0).rotateX(s.headPitch).rotateY(s.headYaw).add(0,1.5F,0);n.rotateX(s.headPitch).rotateY(s.headYaw);}
    if(humanoid&&texture.getPath().contains("outer_layer"))p.sub(0,1,0).mul(1.015F).add(0,1,0);
    if(!humanoid&&part>0)p.z*=1.16F;
    p.mul(scale);float x=center.x+right.x*p.x+up.x*p.y+back.x*p.z,y=center.y+right.y*p.x+up.y*p.y+back.y*p.z,z=center.z+right.z*p.x+up.z*p.y+back.z*p.z;
    buffer.addVertex(matrix,x,y,z).setColor(s.hurt?0xFFFF8888:color).setUv(v[3],v[4]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(s.light[part]).setNormal(matrix,right.x*n.x+up.x*n.y+back.x*n.z,right.y*n.x+up.y*n.y+back.y*n.z,right.z*n.x+up.z*n.y+back.z*n.z);
   }
  });
 }
 private record Mesh(float[][][] frames){}
 private static Mesh load(String name){
  try(var in=new DataInputStream(Minecraft.getInstance().getResourceManager().getResourceOrThrow(Shamaschizm.id("serpent/"+name+".bin")).open())){
   int frames=in.readInt(),vertices=in.readInt();if(frames<1||frames>100||vertices<1||vertices>10000)throw new IOException("Invalid serpent mesh");
   float[][][] data=new float[frames][vertices][9];for(var frame:data)for(var vertex:frame)for(int i=0;i<9;i++)vertex[i]=in.readFloat();return new Mesh(data);
  }catch(IOException error){throw new IllegalStateException("Cannot load serpent mesh "+name,error);}
 }
}
