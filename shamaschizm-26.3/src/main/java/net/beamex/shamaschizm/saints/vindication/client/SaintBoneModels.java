package net.beamex.shamaschizm.saints.vindication.client;
import com.mojang.blaze3d.vertex.PoseStack;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.saints.vindication.RingGeometry;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Uses the original eleven cubes and their animation hierarchy, preserving head UVs. */
public final class SaintBoneModels {
 private static final String[][] PATHS={
  {"head","cube_1"},{"body","bone","cube_4"},{"body","bone2","cube_6"},
  {"body","bone3","cube_8"},{"body","bone4","cube_10"},{"body","bone5","cube_12"},
  {"body","bone6","cube_14"},{"arm","left_arm","cube_17"},{"arm","right_arm","cube_19"},
  {"leg","left_leg","cube_22"},{"leg","right_leg","cube_24"}};
 private static final float[][] CENTRES={{0,-17,0},{0,-13,0},{0,0,-16},{0,0,-16},{0,0,-16},{0,0,-16},{0,-2,-9},{0,4,0},{0,4,0},{0,6,0},{0,6,0}};
 private static final ModelPart PARCHED_HEAD=parchedHead();
 private static ModelPart parchedHead(){
  var mesh=new net.minecraft.client.model.geom.builders.MeshDefinition();
  mesh.getRoot().addOrReplaceChild("head",net.minecraft.client.model.geom.builders.CubeListBuilder.create().texOffs(0,0).addBox(-4,-21,-4,8,8,8),net.minecraft.client.model.geom.PartPose.ZERO);
  return net.minecraft.client.model.geom.builders.LayerDefinition.create(mesh,64,64).bakeRoot().getChild("head");
 }
 private static final Identifier BODY=Shamaschizm.id("textures/entity/vindication.png");
 private static Identifier head(int role){return switch(role){
  case 0->Identifier.withDefaultNamespace("textures/entity/skeleton/stray.png");
  case 1->Identifier.withDefaultNamespace("textures/entity/skeleton/bogged.png");
  case 2->Identifier.withDefaultNamespace("textures/entity/skeleton/parched.png");
  default->Shamaschizm.id("textures/entity/skeleton_variant1.png");};}
 private static ModelPart leaf(VindicationModel model,int part){ModelPart node=model.root();for(String name:PATHS[part])node=node.getChild(name);return node;}
 public static Matrix4f target(VindicationModel model,int slot,int part,float age){
  float rise=Math.max(0,Math.min(1,age/40F)),spin=Math.max(0,age-40);
  model.setupAnim(slot>=2?rise*.4F:rise);
  PoseStack p=new PoseStack();
  if(slot==0){
   p.translate(0,.1854F*(1-rise),0);
   if(age<0){float settle=Math.min(1,-age/6F),t=age+40;p.rotate(com.mojang.math.Axis.YP.rotationDegrees((float)Math.sin(t*3.6F)*48*settle));p.rotate(com.mojang.math.Axis.XP.rotationDegrees((float)Math.sin(t*2.7F)*25*settle));}
   p.rotate(RingGeometry.rotation(spin));p.scale(1.2636828F,-1.2636828F,1.2636828F);
  }else if(slot==1){
   p.rotate(RingGeometry.spellRotation(spin));p.rotate(com.mojang.math.Axis.ZP.rotationDegrees(90));p.scale(.96F,-.96F,.96F);
  }else{
   if(slot==3)p.rotate(com.mojang.math.Axis.XP.rotationDegrees(180));
   p.translate(0,0,-1.3);p.scale(2.05F,-2.05F,2.05F);
  }
  p.translate(0,15.01324369/16,-.23711097/16);
  ModelPart node=model.root();node.translateAndRotate(p);
  for(String name:PATHS[part]){node=node.getChild(name);node.translateAndRotate(p);}
  float[] c=CENTRES[part];p.translate(c[0]/16,c[1]/16,c[2]/16);
  Matrix4f result=new Matrix4f(p.last().pose());
  if(slot>=2){
   // Keep the 40% joint pose, arranged as two rough, opposite half-rings.
   Vector3f position=result.getTranslation(new Vector3f());
   position.z=slot==2?Math.min(-.15F,position.z):Math.max(.15F,position.z);
   float radial=(float)Math.hypot(position.y,position.z);position.y*=2.6F/radial;position.z*=2.6F/radial;
   result.setTranslation(position);result=new Matrix4f().rotate(RingGeometry.outerRotation(spin)).mul(result);
  }
  return result;
 }
 public static void piece(VindicationModel model,int part,int role,Matrix4f transform,PoseStack p,SubmitNodeCollector out,int light,int tint,boolean emissive){
  p.pushPose();p.mulPose(transform);float[] centre=CENTRES[part];p.translate(-centre[0]/16,-centre[1]/16,-centre[2]/16);
  Identifier texture=part==0?head(role):BODY;
  out.submitModelPart(part==0&&role==2?PARCHED_HEAD:leaf(model,part),p,emissive?RenderTypes.entityTranslucentEmissive(texture):RenderTypes.entityCutout(texture),light,OverlayTexture.NO_OVERLAY,null,tint,0);
  if(part==0 && (role==0 || role==1)){
   Identifier overlay=Identifier.withDefaultNamespace("textures/entity/skeleton/"+(role==0?"stray_overlay":"bogged_overlay")+".png");
   p.translate(centre[0]/16,centre[1]/16,centre[2]/16);p.scale(1.025F,1.025F,1.025F);p.translate(-centre[0]/16,-centre[1]/16,-centre[2]/16);
   out.submitModelPart(leaf(model,part),p,emissive?RenderTypes.entityTranslucentEmissive(overlay):RenderTypes.entityCutout(overlay),light,OverlayTexture.NO_OVERLAY,null,tint,0);
  }
  p.popPose();
 }
 public static void ring(VindicationModel model,int slot,int role,float age,PoseStack p,SubmitNodeCollector out,boolean flash,int sceneLight){
  boolean emissive=age>=40;
  for(int part=0;part<11;part++)piece(model,part,role,target(model,slot,part,age),p,out,emissive?0x00F000F0:sceneLight,flash?0xFFFF3030:-1,emissive);
 }
 private SaintBoneModels(){}
}
