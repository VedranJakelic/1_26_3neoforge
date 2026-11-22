package net.beamex.shamaschizm.mixin;
import net.beamex.shamaschizm.eclipse.EclipseClient;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(SoundEngine.class)
public abstract class EclipseMusicMixin {
    @Unique private boolean shamaschizm$eclipseMuted;
    @Shadow public abstract void refreshCategoryVolume(SoundSource source);
    @Inject(method="calculateVolume(FLnet/minecraft/sounds/SoundSource;)F",at=@At("RETURN"),cancellable=true)
    private void eclipseVolume(float volume, SoundSource source, CallbackInfoReturnable<Float> ci) {
        if ((source==SoundSource.MUSIC || source==SoundSource.RECORDS) && EclipseClient.muteMusic()) ci.setReturnValue(0F);
    }
    @Inject(method="tick",at=@At("HEAD"))
    private void eclipseRefresh(boolean paused, CallbackInfo ci) {
        boolean muted=EclipseClient.muteMusic();
        if (muted!=shamaschizm$eclipseMuted) {
            shamaschizm$eclipseMuted=muted;
            refreshCategoryVolume(SoundSource.MUSIC); refreshCategoryVolume(SoundSource.RECORDS);
        }
    }
}
