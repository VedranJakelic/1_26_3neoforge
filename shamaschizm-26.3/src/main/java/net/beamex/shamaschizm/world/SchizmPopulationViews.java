package net.beamex.shamaschizm.world;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.beamex.shamaschizm.astral.AstralProjection;
import net.beamex.shamaschizm.localportal.DoorwayData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/** Cache the same two nearby portal destinations used by portal entity streaming. */
public final class SchizmPopulationViews {
    private record Views(long tick, List<Vec3> exits) {}
    private static final Map<ServerLevel, Views> CACHE = new WeakHashMap<>();
    private SchizmPopulationViews() {}
    public static boolean nearPortal(Mob mob) {
        if (!(mob.level() instanceof ServerLevel level) || !level.dimension().equals(Schizm.KEY)) return false;
        Views views = CACHE.get(level);
        long now = level.getGameTime();
        if (views == null || views.tick() != now) {
            var exits = new ArrayList<Vec3>();
            var data = DoorwayData.get(level);
            var doors = data.all();
            for (var player : level.players()) {
                if (!player.isAlive() || player.isSpectator()) continue;
                Vec3 eye = AstralProjection.viewpoint(player), pos = AstralProjection.trackingPosition(player);
                var sources = doors.stream().filter(d -> d.enabled() && d.distance(eye) > -.5
                        && d.origin().distanceToSqr(pos) < 24 * 24)
                        .sorted(Comparator.comparingDouble(d -> d.origin().distanceToSqr(pos))).limit(2).toList();
                for (var source : sources) {
                    var target = data.get(source.target());
                    if (target != null && target.enabled() && target.target().equals(source.name())) exits.add(target.origin());
                }
            }
            views = new Views(now, List.copyOf(exits));
            CACHE.put(level, views);
        }
        for (Vec3 exit : views.exits()) if (mob.position().distanceToSqr(exit) <= 32 * 32) return true;
        return false;
    }
}
