package net.beamex.shamaschizm.mixin;

import net.beamex.shamaschizm.event.client.PortalAtmosphereClient;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Darkens the actual per-frame sky render state without changing server light levels. */
@Mixin(SkyRenderer.class)
public abstract class PortalSkyDarknessMixin {
    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void shamaschizm$darkenPortalSky(ClientLevel level, float partialTicks,
            Camera camera, SkyRenderState state, CallbackInfo callback) {
        float proximity = PortalAtmosphereClient.proximity();
        if (proximity > 0.0F) {
            state.skyColor = ARGB.scaleRGB(state.skyColor, 1.0F - 0.85F * proximity);
        }
    }
}
