package net.beamex.shamaschizm.mixin;

import net.beamex.shamaschizm.client.ClientTrippyEffects;
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
public abstract class TrippingSoundEngineMixin {
    @Unique private float shamaschizm$previousGain = 1F;
    @Shadow public abstract void refreshCategoryVolume(SoundSource source);
    @Inject(method = "calculateVolume(FLnet/minecraft/sounds/SoundSource;)F", at = @At("RETURN"), cancellable = true)
    private void shamaschizm$fade(float volume, SoundSource source, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(cir.getReturnValue() * ClientTrippyEffects.soundGain());
    }
    @Inject(method = "tick", at = @At("HEAD"))
    private void shamaschizm$refresh(boolean paused, CallbackInfo ci) {
        float gain = ClientTrippyEffects.soundGain();
        if (Math.abs(gain-shamaschizm$previousGain) > 0.0001F) {
            shamaschizm$previousGain = gain;
            refreshCategoryVolume(SoundSource.MASTER);
        }
    }
}
