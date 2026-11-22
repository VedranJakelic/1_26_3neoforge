package net.beamex.shamaschizm.event;

import net.beamex.shamaschizm.eclipse.Eclipses;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.world.Schizm;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobDespawnEvent;
import net.neoforged.neoforge.event.entity.living.SpawnClusterSizeEvent;

@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class SchizmPopulationEvents {
    private SchizmPopulationEvents() {}
    @SubscribeEvent
    public static void clusters(SpawnClusterSizeEvent event) {
        Mob mob = event.getEntity();
        if (!Eclipses.habitat(mob.level(), mob.blockPosition())) return;
        var type = mob.getType();
        if (type == EntityTypes.ZOMBIE || type == EntityTypes.HUSK || type == EntityTypes.SKELETON
                || type == EntityTypes.WITHER_SKELETON || type == EntityTypes.SPIDER) {
            event.setSize(Math.max(event.getSize(), 8));
        }
    }
    @SubscribeEvent
    public static void despawn(MobDespawnEvent event) {
        Mob mob = event.getEntity();
        if (!(mob.level() instanceof ServerLevel level) || !level.dimension().equals(Schizm.KEY)
                || event.getResult() != MobDespawnEvent.Result.DEFAULT
                || !mob.isAlive() || mob.isPersistenceRequired() || mob.requiresCustomPersistence()
                || mob.hasCustomName() || mob.isLeashed() || mob.isPassenger() || mob.isVehicle()
                || mob instanceof TamableAnimal tame && tame.isTame()) return;
        // Remote portal views must not lose visible mobs to physical-distance cleanup.
        if (net.beamex.shamaschizm.world.SchizmPopulationViews.nearPortal(mob)) {
            event.setResult(MobDespawnEvent.Result.DENY);
            return;
        }
        if (!(mob instanceof net.beamex.shamaschizm.entity.custom.RoachEntity
                || mob instanceof net.beamex.shamaschizm.earthworm.EarthwormEntity)
                || (level.getGameTime() + mob.getId()) % 20 != 0) return;
        double nearest = Double.POSITIVE_INFINITY;
        for (var player : level.players()) {
            if (player.isAlive() && !player.isSpectator()) nearest = Math.min(nearest, player.distanceToSqr(mob));
        }
        if (!mob.removeWhenFarAway(nearest)) return;
        // Far cleanup is immediate; midrange cleanup needs inactivity and a staggered roll.
        if (nearest > 96 * 96 || nearest > 48 * 48 && mob.getNoActionTime() > 400
                && mob.getRandom().nextInt(10) == 0) event.setResult(MobDespawnEvent.Result.ALLOW);
    }
}
