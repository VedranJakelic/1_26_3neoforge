package net.beamex.shamaschizm.saints.vindication.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.beamex.shamaschizm.saints.vindication.TemplarProjectile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;

public final class TemplarSpearRenderer extends EntityRenderer<TemplarProjectile,TemplarSpearRenderer.State>{
    public TemplarSpearRenderer(EntityRendererProvider.Context context){super(context);}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(TemplarProjectile entity,State state,float partial){
        super.extractRenderState(entity,state,partial);state.yaw=entity.getYRot(partial);state.pitch=entity.getXRot(partial);
        Minecraft.getInstance().getItemModelResolver().updateForNonLiving(state.item,entity.displayedItem(),ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,entity);
    }
    @Override public void submit(State state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){
        super.submit(state,pose,collector,camera);pose.pushPose();
        // Arrow yaw is measured from +Z. Align the model's (-X,+Y) tip with +Z,
        // then apply the projectile's pitch and yaw; undo held-item transforms.
        pose.rotate(Axis.YP.rotationDegrees(state.yaw));
        pose.rotate(Axis.XP.rotationDegrees(-state.pitch));
        pose.rotate(Axis.XP.rotationDegrees(90));
        pose.rotate(Axis.ZP.rotationDegrees(-45));
        pose.rotate(Axis.ZP.rotationDegrees(40));
        pose.rotate(Axis.YP.rotationDegrees(-270));
        pose.rotate(Axis.XP.rotationDegrees(-5));
        pose.translate(0,-2.0/16,-2.0/16);
        state.item.submit(pose,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,state.outlineColor);pose.popPose();
    }
    public static final class State extends EntityRenderState{public final ItemStackRenderState item=new ItemStackRenderState();public float yaw,pitch;}
}
