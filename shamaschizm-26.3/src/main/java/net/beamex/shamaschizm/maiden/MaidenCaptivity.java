package net.beamex.shamaschizm.maiden;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
/** Sneaking is not an escape; killing the mimic, logout or dimension travel safely releases the rider. */
@EventBusSubscriber(modid="shamaschizm")
public final class MaidenCaptivity {
 @SubscribeEvent public static void dismount(EntityMountEvent e){if(e.isDismounting()&&e.getEntityBeingMounted() instanceof IronMaiden m&&m.holds(e.getEntityMounting())&&e.getEntityMounting() instanceof Player p&&p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&p.level()==m.level()&&p.distanceToSqr(m)<16)e.setCanceled(true);}
 @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity().getVehicle() instanceof IronMaiden m)m.release();}
 @SubscribeEvent public static void dimension(EntityTravelToDimensionEvent e){if(!e.getDimension().equals(e.getEntity().level().dimension())&&e.getEntity().getVehicle() instanceof IronMaiden m)m.release();}
}
