package net.beamex.shamaschizm.mixin;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import net.beamex.shamaschizm.client.ClientTrippyEffects;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GameRenderer.class)
public abstract class TrippingGameRendererMixin {
    @Shadow @Final private RenderTarget mainRenderTarget;
    @Shadow @Final private CrossFrameResourcePool resourcePool;
    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;blitEntityOutline()V", shift = At.Shift.AFTER))
    private void shamaschizm$trip(CallbackInfo ci) {
        ClientTrippyEffects.render(mainRenderTarget, resourcePool);
        net.beamex.shamaschizm.client.CentipedeBiteBlur.render(mainRenderTarget, resourcePool);
        net.beamex.shamaschizm.wisdom.client.WisdomLens.render(mainRenderTarget, resourcePool);
    }
    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/render/GuiRenderer;render()V", shift = At.Shift.AFTER))
    private void shamaschizm$fractals(CallbackInfo ci) {
        ClientTrippyEffects.renderOverlay(mainRenderTarget, resourcePool);
    }
}
