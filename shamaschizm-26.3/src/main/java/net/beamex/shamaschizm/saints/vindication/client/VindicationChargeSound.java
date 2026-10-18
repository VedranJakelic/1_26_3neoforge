package net.beamex.shamaschizm.saints.vindication.client;
import net.beamex.shamaschizm.saints.vindication.VindicationEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.*;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
public final class VindicationChargeSound extends AbstractTickableSoundInstance {
 private final VindicationEntity boss;
 public VindicationChargeSound(VindicationEntity boss){super(SoundEvents.GUARDIAN_ATTACK,SoundSource.HOSTILE,SoundInstance.createUnseededRandom());this.boss=boss;looping=true;delay=0;volume=.01F;pitch=.7F;x=boss.getX();y=boss.getY()+.5;z=boss.getZ();}
 @Override public boolean canPlaySound(){return !boss.isSilent();}
 @Override public void tick(){
  if(!boss.isAlive()||boss.isRemoved()||boss.level()!=Minecraft.getInstance().level||(!boss.isCharging()&&boss.beamTicks()==0)){stop();return;}
  x=boss.getX();y=boss.getY()+.5;z=boss.getZ();float charge=boss.chargeProgress();volume=Math.max(.01F,charge*charge);pitch=.7F+.5F*charge;
 }
}
