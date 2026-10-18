package net.beamex.shamaschizm.saints.vindication;
import net.beamex.shamaschizm.Shamaschizm;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
/** Protect the old skeleton even from damage that normally bypasses its invulnerable flag. */
@EventBusSubscriber(modid=Shamaschizm.MOD_ID)
public final class SaintConversionProtection {
 @SubscribeEvent(priority=EventPriority.HIGHEST)
 public static void damage(LivingIncomingDamageEvent event){
  if(event.getEntity().getPersistentData().getBooleanOr("SaintsConverting",false))event.setCanceled(true);
 }
}
