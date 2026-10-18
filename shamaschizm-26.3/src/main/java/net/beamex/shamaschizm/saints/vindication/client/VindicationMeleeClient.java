package net.beamex.shamaschizm.saints.vindication.client;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.saints.vindication.VindicationMelee;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
@EventBusSubscriber(modid=Shamaschizm.MOD_ID,value=Dist.CLIENT)
public final class VindicationMeleeClient {
 @SubscribeEvent public static void swing(InputEvent.InteractionKeyMappingTriggered e){
  var mc=Minecraft.getInstance();
  if(!e.isAttack() || mc.player==null || mc.level==null)return;
  var hit=VindicationMelee.trace(mc.player);
  if(hit==null)return;
  e.setCanceled(true);e.setSwingHand(true);
  ClientPacketDistributor.sendToServer(new VindicationMelee.Swing(hit.boss().getId()));
  mc.player.resetAttackStrengthTicker();
 }
}
