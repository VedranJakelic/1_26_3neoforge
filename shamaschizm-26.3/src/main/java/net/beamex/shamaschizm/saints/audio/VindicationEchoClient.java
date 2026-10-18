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
public final class VindicationEchoClient {
 private record Reflection(int due,VindicationEcho.Echo echo,float gain){}
 private static final List<Reflection> reflections=new ArrayList<>();
 private static Object world;private static int clock;
 @SubscribeEvent public static void tick(ClientTickEvent.Post e){
  var mc=Minecraft.getInstance();
  if(mc.level!=world){reflections.clear();VindicationEcho.PENDING.clear();world=mc.level;}
  if(mc.level==null || mc.isPaused())return;
  clock++;VindicationEcho.Echo p;
  while((p=VindicationEcho.PENDING.poll())!=null){reflections.add(new Reflection(clock+3,p,.18F));reflections.add(new Reflection(clock+7,p,.07F));}
  for(var it=reflections.iterator();it.hasNext();){var r=it.next();if(clock>=r.due){mc.getSoundManager().play(new ReflectionSound(r.echo,r.gain));it.remove();}}
 }
 private static final class ReflectionSound extends AbstractSoundInstance {
  ReflectionSound(VindicationEcho.Echo e,float gain){
   super(SoundEvent.createVariableRangeEvent(e.sound()),SoundSource.HOSTILE,SoundInstance.createUnseededRandom());
   x=e.pos().x;y=e.pos().y;z=e.pos().z;volume=Math.min(1.5F,e.volume())*gain;pitch=e.pitch();
  }
 }
}
