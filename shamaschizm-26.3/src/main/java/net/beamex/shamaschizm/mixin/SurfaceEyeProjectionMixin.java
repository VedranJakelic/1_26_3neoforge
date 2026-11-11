package net.beamex.shamaschizm.mixin;
import net.beamex.shamaschizm.client.SurfaceEyeEffects;
import net.beamex.shamaschizm.client.ClientTrippyEffects;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class SurfaceEyeProjectionMixin {
    @Shadow @Final private RenderTarget mainRenderTarget;
    @Shadow @Final private CrossFrameResourcePool resourcePool;
    // The 3D HUD can clear world depth: consume it immediately before that call.
    @Inject(method="renderLevel",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/GameRenderer;render3dHud(Lnet/minecraft/client/renderer/state/level/CameraRenderState;Lnet/minecraft/client/renderer/state/level/PlayerRenderState;Lnet/minecraft/client/renderer/state/OptionsRenderState;Z)V"))
    private void shamaschizm$surfaceEyes(CallbackInfo ci) {
        ClientTrippyEffects.renderSurfaces(mainRenderTarget,resourcePool);
    }
    @Inject(method="renderLevel",at=@At("HEAD"))
    private void shamaschizm$beginSurfaceEyes(CallbackInfo ci) { SurfaceEyeEffects.beginFrame(); }
    // Read the exact world projection after bob/hurt/nausea, returning it unchanged.
    @ModifyArg(method="renderLevel",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/ProjectionMatrixBuffer;getBuffer(Lorg/joml/Matrix4f;)Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;"),index=0)
    private Matrix4f shamaschizm$captureSurfaceProjection(Matrix4f projection) {
        SurfaceEyeEffects.captureProjection(projection); return projection;
    }
}
