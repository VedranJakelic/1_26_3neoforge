package net.beamex.shamaschizm.saints.vindication.client;
import com.mojang.blaze3d.vertex.PoseStack;
import net.beamex.shamaschizm.saints.vindication.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Quaternionf;
public final class SaintBoneRenderer extends EntityRenderer<SaintBoneEntity,SaintBoneRenderer.State>{
 private final VindicationModel model;
 public SaintBoneRenderer(EntityRendererProvider.Context c){super(c);model=new VindicationModel(c.bakeLayer(VindicationModel.LAYER));}
 @Override public State createRenderState(){return new State();}
 @Override protected AABB getBoundingBoxForCulling(SaintBoneEntity e, float partialTicks){return e.getBoundingBox().inflate(e.bossEntityId()>=0?160:1);}
 @Override public void extractRenderState(SaintBoneEntity e,State s,float partial){
  super.extractRenderState(e,s,partial);s.part=e.part();s.role=e.role();float height=e.part()==0?.25F:.075F;s.emissive=false;
  var a=e.angles();Quaternionf loose=new Quaternionf().rotationXYZ(a.x(),a.y(),a.z());
  s.transform=new Matrix4f().translation(0,height,0).rotate(loose).scale(1,-1,1);
  var level=Minecraft.getInstance().level;
  if(level!=null && level.getEntity(e.bossEntityId()) instanceof VindicationEntity boss){
   float age=boss.visualPhaseAge(partial);
   float blend=Math.max(0,Math.min(1,age/40F));blend=blend*blend*(3-2*blend);
   if(blend<=0)return;
   var relative=boss.getPosition(partial).add(0,.5,0).subtract(e.getPosition(partial));
   Matrix4f target=SaintBoneModels.target(model,e.slot(),e.part(),age);
   Vector3f point=target.getTranslation(new Vector3f()).add((float)relative.x,(float)relative.y,(float)relative.z);
   Vector3f scale=target.getScale(new Vector3f());
   Quaternionf orientation=target.scale(1,-1,1).getUnnormalizedRotation(new Quaternionf()).normalize();
   point.lerp(new Vector3f(0,height,0),1-blend);loose.slerp(orientation,blend);
   float size=1+(scale.x-1)*blend;
   s.transform=new Matrix4f().translation(point).rotate(loose).scale(size,-size,size);s.emissive=age>=40;
  }
 }
 @Override public void submit(State s,PoseStack p,SubmitNodeCollector c,CameraRenderState camera){
  super.submit(s,p,c,camera);SaintBoneModels.piece(model,s.part,s.role,s.transform,p,c,s.emissive?0x00F000F0:s.lightCoords,-1,s.emissive);
 }
 public static final class State extends EntityRenderState{public int part,role;public boolean emissive;public Matrix4f transform=new Matrix4f();}
}
