package net.beamex.shamaschizm.mixin;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.saints.audio.BoundedDiscSound;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(SimpleSoundInstance.class)
public abstract class GallopJukeboxSoundMixin {
 @Inject(method="forJukeboxSong",at=@At("HEAD"),cancellable=true)
 private static void shamaschizm$boundedDisc(SoundEvent sound,Vec3 pos,CallbackInfoReturnable<SimpleSoundInstance> cir){
  if(sound.location().equals(Shamaschizm.id("gallop_of_the_damned")))cir.setReturnValue(new BoundedDiscSound(sound,pos));
 }
}
