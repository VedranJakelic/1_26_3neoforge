package net.beamex.shamaschizm.slab;

import java.util.Comparator;
import java.util.List;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Server-side attraction; no packet or client polling is required. */
@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class SlabUndeadAttraction {
    private static final double RANGE = 30.0D;

    private SlabUndeadAttraction() {}

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || player.tickCount % 10 != 0
                || player.isCreative() || player.isSpectator()
                || !carriesSlab(player)) return;

        AABB area = player.getBoundingBox().inflate(RANGE);
        ServerLevel level = (ServerLevel) player.level();
        List<Mob> undead = level.getEntitiesOfClass(Mob.class, area,
                mob -> mob.isAlive() && (mob instanceof Zombie || mob instanceof AbstractSkeleton));
        for (Mob mob : undead) {
            Player heldCarrier = nearestCarrier(mob, true);
            if (heldCarrier != null) {
                // A visible held slab always wins, including over an existing target.
                if (mob.getTarget() != heldCarrier) mob.setTarget(heldCarrier);
                mob.getNavigation().moveTo(heldCarrier, 1.15D);
            } else if (mob.getTarget() == null || !mob.getTarget().isAlive()) {
                // An inventory-carried slab makes its owner discoverable, but does
                // not pull an undead mob away from an existing valid combat target.
                Player carrier = nearestCarrier(mob, false);
                if (carrier != null) {
                    mob.setTarget(carrier);
                    mob.getNavigation().moveTo(carrier, 1.0D);
                }
            }
        }
    }

    private static Player nearestCarrier(Mob mob, boolean mustHold) {
        return mob.level().players().stream()
                .filter(player -> player.isAlive() && !player.isCreative() && !player.isSpectator())
                .filter(player -> mob.distanceToSqr(player) <= RANGE * RANGE)
                .filter(player -> mustHold ? holdsSlab(player) : carriesSlab(player))
                .min(Comparator.comparingDouble(mob::distanceToSqr))
                .orElse(null);
    }

    public static boolean holdsSlab(Player player) {
        return player.getMainHandItem().is(SlabRegistration.SLAB)
                || player.getOffhandItem().is(SlabRegistration.SLAB);
    }

    public static boolean carriesSlab(Player player) {
        return player.getInventory().contains(stack -> stack.is(SlabRegistration.SLAB));
    }
}
