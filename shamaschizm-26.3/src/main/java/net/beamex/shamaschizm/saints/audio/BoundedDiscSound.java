package net.beamex.shamaschizm.saints.audio;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.*;
import net.minecraft.sounds.*;
import net.minecraft.world.phys.Vec3;
/** The jukebox stores this exact instance, so ejecting the disc still stops it. */
public final class BoundedDiscSound extends SimpleSoundInstance implements TickableSoundInstance {
 private final Object level=Minecraft.getInstance().level;
 private final Vec3 centre;
 public BoundedDiscSound(SoundEvent sound,Vec3 centre){
  super(sound.location(),SoundSource.RECORDS,.75F,1,SoundInstance.createUnseededRandom(),false,0,
   Attenuation.NONE,0,0,0,true);this.centre=centre;
 }
 @Override public float getVolume(){
  var p=Minecraft.getInstance().player;
  return p!=null && MusicRange.audible(p.position().distanceToSqr(centre),MusicRange.DISC_RADIUS)?super.getVolume():0;
 }
 @Override public boolean canStartSilent(){return true;}
 @Override public boolean isStopped(){return Minecraft.getInstance().level!=level;}
 @Override public void tick(){}
}
