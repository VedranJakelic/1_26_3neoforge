package net.beamex.shamaschizm.maiden.client;
import net.beamex.shamaschizm.maiden.IronMaiden;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
/** Vanilla picking excludes one's mount. Send the normal attack packet so captives can hit the interior. */
@EventBusSubscriber(modid="shamaschizm",value=Dist.CLIENT)
public final class MaidenInput {
 @SubscribeEvent public static void attack(InputEvent.InteractionKeyMappingTriggered e){
  var mc=Minecraft.getInstance();
  if(!e.isAttack()||mc.player==null||mc.gameMode==null||!(mc.player.getVehicle() instanceof IronMaiden m)||!m.holds(mc.player))return;
  e.setCanceled(true);e.setSwingHand(false);mc.gameMode.attack(mc.player,m);mc.player.swingAndResetAttackStrength(InteractionHand.MAIN_HAND,net.minecraft.world.item.component.SwingAnimation.DEFAULT,true);
 }
}
