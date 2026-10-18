package net.beamex.shamaschizm.saints.vindication.client;
import com.mojang.blaze3d.vertex.PoseStack;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.entity.client.SpellVisualModel;
import net.beamex.shamaschizm.saints.vindication.*;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Unit;
import net.minecraft.world.phys.AABB;
public final class VindicationRenderer extends EntityRenderer<VindicationEntity,VindicationRenderer.State>{
 private static final int FULL_BRIGHT = 0x00F000F0;
 private final VindicationModel bones;private final Model<Unit> spell;
 public VindicationRenderer(EntityRendererProvider.Context c){super(c);bones=new VindicationModel(c.bakeLayer(VindicationModel.LAYER));spell=new Model<Unit>(c.bakeLayer(SpellVisualModel.LAYER_LOCATION),RenderTypes::entityTranslucent){};}
 @Override public State createRenderState(){return new State();}
 @Override protected AABB getBoundingBoxForCulling(VindicationEntity e, float partialTicks){return e.getBoundingBox().inflate(e.beamTicks()>0?50:6);}
 @Override public void extractRenderState(VindicationEntity e,State s,float partial){super.extractRenderState(e,s,partial);s.age=e.visualPhaseAge(partial);s.outerFlash=e.outerRingFlashing();for(int i=0;i<4;i++)s.roles[i]=e.roleForSlot(i);s.boneFlash=e.boneRingFlashing();s.spellFlash=e.spellRingFlashing();s.beams=e.beamTicks()>0?java.util.List.of(e.beam(0),e.beam(1),e.beam(2)):java.util.List.of();}
 @Override public void submit(State s,PoseStack p,SubmitNodeCollector c,CameraRenderState camera){
  super.submit(s,p,c,camera);
  // Normal scene lighting through the grounded prelude and rise; emissive only once fully formed.
  float rise=Math.max(0,Math.min(1,s.age/40F));p.pushPose();p.translate(0,.5,0);
  SaintBoneModels.ring(bones,0,s.roles[0],s.age,p,c,s.boneFlash,s.lightCoords);
  if(rise>=1){
   p.pushPose();p.rotate(RingGeometry.spellRotation(Math.max(0,s.age-40)));p.scale(.7866667F,-.7866667F,.7866667F);p.translate(0,-1.5,0);
   c.submitModel(spell,Unit.INSTANCE,p,RenderTypes.entityTranslucentEmissive(Shamaschizm.id("textures/entity/spell1.png")),FULL_BRIGHT,OverlayTexture.NO_OVERLAY,s.spellFlash?0xFFFF3030:0xFF88DDFF,null,s.outlineColor);p.popPose();
   SaintBoneModels.ring(bones,1,s.roles[1],s.age,p,c,s.spellFlash,s.lightCoords);
   SaintBoneModels.ring(bones,2,s.roles[2],s.age,p,c,s.outerFlash,s.lightCoords);
   SaintBoneModels.ring(bones,3,s.roles[3],s.age,p,c,s.outerFlash,s.lightCoords);
   VindicationSphereRenderer.submit(p,c);
  }
  for(var beam:s.beams){
   if(beam.length()<1)continue;
   p.pushPose();
   net.beamex.shamaschizm.mixin.GuardianBeamInvoker.shamaschizm$beam(p,c,beam.normalize().scale(beam.length()-1),s.age,1,s.age*.5F%1);
   p.popPose();
  }
  p.popPose();
 }
 public static final class State extends EntityRenderState{public float age;public boolean boneFlash,spellFlash,outerFlash;public final int[] roles=new int[4];public java.util.List<net.minecraft.world.phys.Vec3> beams=java.util.List.of();}
}
