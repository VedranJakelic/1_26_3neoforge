package net.beamex.shamaschizm.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Use the custom coffin skeleton's established resting transform for vanilla skeletons too. */
@Mixin(LivingEntityRenderer.class)
public abstract class CoffinSkeletonPoseMixin {
    @Inject(method="setupRotations",at=@At("HEAD"),cancellable=true)
    private void shamaschizm$coffinPose(LivingEntityRenderState state, PoseStack poses,
                                        float bodyRot, float scale, CallbackInfo ci) {
        if (!(state instanceof SkeletonRenderState) || !state.hasPose(Pose.SLEEPING)) return;
        poses.rotate(Axis.YP.rotationDegrees(bodyRot + 180.0F));
        poses.translate(1.8F, 0.0F, 0.0F);
        poses.rotate(Axis.ZP.rotationDegrees(90.0F));
        poses.rotate(Axis.YP.rotationDegrees(270.0F));
        ci.cancel();
    }
}
