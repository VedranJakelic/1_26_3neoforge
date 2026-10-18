package net.beamex.shamaschizm.saints.audio;
import java.util.*;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.*;
import net.minecraft.sounds.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
@EventBusSubscriber(modid=Shamaschizm.MOD_ID,value=Dist.CLIENT)
public final class ArenaMusicClient {
 private static Object world;
 private static final Map<UUID,BattleTrack> tracks=new HashMap<>();
 private static final Map<UUID,Integer> leases=new HashMap<>();
 private static int clock;
 public static boolean isBattleSound(SoundInstance s){return s instanceof BattleTrack;}
 @SubscribeEvent public static void tick(ClientTickEvent.Post e){
  var mc=Minecraft.getInstance();
  if(mc.level!=world){for(var t:tracks.values())mc.getSoundManager().stop(t);tracks.clear();leases.clear();ArenaAudio.MUSIC.clear();ArenaAudio.clientGain=1;world=mc.level;}
  if(mc.level==null || mc.isPaused())return;
  clock++;
  ArenaAudio.Music m;
  while((m=ArenaAudio.MUSIC.poll())!=null){
   BattleTrack t=tracks.get(m.encounter());
   if(m.active()){
    leases.put(m.encounter(),clock+60);
    if(t==null){t=new BattleTrack(m.cue(),m.centre());tracks.put(m.encounter(),t);mc.getSoundManager().play(t);}
   }else{leases.remove(m.encounter());if(t!=null)t.fading=true;}
  }
  for(var it=tracks.entrySet().iterator();it.hasNext();){
   var entry=it.next();var t=entry.getValue();
   if(clock>leases.getOrDefault(entry.getKey(),-1))t.fading=true;
   if(t.isStopped()){mc.getSoundManager().stop(t);it.remove();leases.remove(entry.getKey());}
   else if(!t.fading && ++t.lifetime>40 && !mc.getSoundManager().isActive(t)){
    // After the opening recording finishes, loop the song without the cue silence.
    BattleTrack repeat=new BattleTrack(false,t.centre);entry.setValue(repeat);mc.getSoundManager().play(repeat);
   }
  }
 }
 public static final class BattleTrack extends AbstractTickableSoundInstance {
  boolean fading;int fadeTicks,lifetime;
  final net.minecraft.world.phys.Vec3 centre;
  BattleTrack(boolean cue,net.minecraft.world.phys.Vec3 centre){
   super(SoundEvent.createVariableRangeEvent(Shamaschizm.id(cue?"saints_battle_cue":"gallop_of_the_damned")),SoundSource.MUSIC,SoundInstance.createUnseededRandom());
   this.centre=centre;
   relative=true;attenuation=Attenuation.NONE;volume=.75F;looping=!cue;delay=0;
  }
  @Override public boolean canStartSilent(){return true;}
  @Override public float getVolume(){
   var player=Minecraft.getInstance().player;
   return player!=null && MusicRange.audible(player.position().distanceToSqr(centre),MusicRange.ARENA_RADIUS)?super.getVolume():0;
  }
  @Override public void tick(){if(fading){volume=.75F*Math.max(0,1-++fadeTicks/80F);if(fadeTicks>=80)stop();}}
 }
}
