package net.beamex.shamaschizm.event;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.entity.custom.MegalithicGateEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;
/** Deliberately absent from commands, help, advancement and tooltip text. */
@EventBusSubscriber(modid=Shamaschizm.MOD_ID)
public final class HiddenGatePhrase {
 @SubscribeEvent public static void chat(ServerChatEvent event){
  if(!event.getRawText().trim().equalsIgnoreCase("aperi viam"))return;
  var player=event.getPlayer();var level=player.level();var position=player.position();
  level.getServer().execute(()->{
   if(player.isRemoved()||player.level()!=level)return;
   for(var gate:level.getEntitiesOfClass(MegalithicGateEntity.class,new net.minecraft.world.phys.AABB(position,position).inflate(5)))
    if(gate.position().distanceToSqr(position)<=25)gate.openByPhrase();
  });
 }
}
