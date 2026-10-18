package net.beamex.shamaschizm.mixin;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.slab.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Retains the real frame/name but displays the slab sheet at vanilla map size. */
@Mixin(ItemFrameRenderer.class)
public abstract class SlabFrameRendererMixin {
 @Shadow @Final private BlockModelResolver blockModelResolver;
 @Shadow public abstract Vec3 getRenderOffset(ItemFrameRenderState state);
 @Inject(method="extractRenderState(Lnet/minecraft/world/entity/decoration/ItemFrame;Lnet/minecraft/client/renderer/entity/state/ItemFrameRenderState;F)V",at=@At("RETURN"))
 private void shamaschizm$extract(ItemFrame entity,ItemFrameRenderState state,float partialTicks,CallbackInfo ci){
  var stack=entity.getItem();
  Identifier texture=stack.is(SlabRegistration.SLAB)?Shamaschizm.id(SlabAppearance.isSecond(stack)?"textures/item/slab02.png":"textures/item/slab01.png"):null;
  ((SlabFrameState)state).shamaschizm$slabTexture(texture);
  if(texture!=null){state.item.clear();state.mapId=null;
   if(!state.isInvisible)blockModelResolver.updateForItemFrame(state.frameModel,state.isGlowFrame,true);
  }
 }
 @Inject(method="submit(Lnet/minecraft/client/renderer/entity/state/ItemFrameRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",at=@At("RETURN"))
 private void shamaschizm$sheet(ItemFrameRenderState state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera,CallbackInfo ci){
  Identifier texture=((SlabFrameState)state).shamaschizm$slabTexture();if(texture==null)return;
  pose.pushPose();Direction direction=state.direction;Vec3 offset=getRenderOffset(state);
  pose.translate(-offset.x(),-offset.y(),-offset.z());
  pose.translate(direction.getStepX()*.46875,direction.getStepY()*.46875,direction.getStepZ()*.46875);
  pose.rotate(Axis.XP.rotationDegrees(direction.getAxis().isHorizontal()?0:-90*direction.getAxisDirection().getStep()));
  pose.rotate(Axis.YP.rotationDegrees(direction.getAxis().isHorizontal()?180-direction.toYRot():180));
  pose.translate(0,0,state.isInvisible?.5:.4375);
  pose.rotate(Axis.ZP.rotationDegrees((state.rotation%4)*90+180));
  pose.scale(1F/128,1F/128,1F/128);pose.translate(-64,-64,-1);
  int light=state.isGlowFrame?15728850:state.lightCoords;
  collector.submitCustomGeometry(pose,RenderTypes.text(texture),(matrix,buffer)->{
   buffer.addVertex(matrix,0,128,0).setColor(-1).setUv(0,1).setLight(light);
   buffer.addVertex(matrix,128,128,0).setColor(-1).setUv(1,1).setLight(light);
   buffer.addVertex(matrix,128,0,0).setColor(-1).setUv(1,0).setLight(light);
   buffer.addVertex(matrix,0,0,0).setColor(-1).setUv(0,0).setLight(light);
  });pose.popPose();
 }
}
