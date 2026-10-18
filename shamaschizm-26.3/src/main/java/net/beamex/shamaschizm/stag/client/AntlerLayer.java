package net.beamex.shamaschizm.stag.client;
import com.mojang.blaze3d.vertex.PoseStack;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Unit;
public final class AntlerLayer<S extends LivingEntityRenderState,M extends EntityModel<S>> extends RenderLayer<S,M>{
 private final Model<Unit> antlers;
 public AntlerLayer(RenderLayerParent<S,M> parent,ModelPart root){super(parent);antlers=new Model<Unit>(root,RenderTypes::entityCutout){};}
 @Override public void submit(PoseStack poses,SubmitNodeCollector collector,int light,S state,float yaw,float pitch){
  if(!((AntlerState)state).shamaschizm$hasAntlers()||state.isInvisible||!(getParentModel() instanceof HeadedModel head))return;
  poses.pushPose();getParentModel().root().translateAndRotate(poses);head.translateToHead(poses);
  collector.submitModel(antlers,Unit.INSTANCE,poses,RenderTypes.entityCutout(Shamaschizm.id("textures/entity/stag1.png")),light,OverlayTexture.NO_OVERLAY,-1,null,state.outlineColor);
  poses.popPose();
 }
}
