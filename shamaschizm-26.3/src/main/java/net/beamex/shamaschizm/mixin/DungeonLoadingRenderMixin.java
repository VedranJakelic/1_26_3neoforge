package net.beamex.shamaschizm.mixin;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import net.beamex.shamaschizm.loading.client.DungeonLoadingClient;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GameRenderer.class)
public abstract class DungeonLoadingRenderMixin {
    @Shadow @Final private RenderTarget mainRenderTarget;
    @Shadow @Final private CrossFrameResourcePool resourcePool;
    @Inject(method="render",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/resource/CrossFrameResourcePool;endFrame()V"))
    private void shamaschizm$loading(CallbackInfo ci){DungeonLoadingClient.render(mainRenderTarget,resourcePool);}
}
