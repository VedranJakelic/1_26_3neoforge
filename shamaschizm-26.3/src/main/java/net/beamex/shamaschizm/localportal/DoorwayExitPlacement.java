package net.beamex.shamaschizm.localportal;

import java.util.List;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Geometry shared by exit placement and headless regression checks. */
public final class DoorwayExitPlacement {
    public static final double EPSILON = 1.0e-5;
    private DoorwayExitPlacement() {}

    public static Vec3 anchor(Doorway source, Doorway target, Vec3 previous, Vec3 current, double width) {
        double a = source.distance(previous), b = source.distance(current);
        Vec3 crossing = previous.lerp(current, a / (a - b));
        return source.transformPoint(target, crossing).add(target.facing().normal.scale(width / 2 + EPSILON));
    }

    /** Try the exact crossing first, then slide at most 0.35 blocks inward.
     * Never search beyond the aperture, through its side wall, or across a blocked room.
     */
    public static List<Vec3> cornerCandidates(Doorway target, Vec3 exact, double width) {
        var result = new java.util.ArrayList<Vec3>();
        result.add(exact);
        Vec3 local = target.local(exact);
        double limit = Math.min(0.35, Math.abs(local.x));
        double edge = target.width() / 2 - width / 2 - EPSILON;
        for (int i = 1; i <= 14; i++) {
            double shift = Math.min(limit, i * 0.025);
            double x = local.x - Math.copySign(shift, local.x);
            if (Math.abs(x) <= edge) result.add(target.world(x, local.y, local.z));
            if (shift >= limit) break;
        }
        return List.copyOf(result);
    }

    /** Exact collision-shape top, rather than rounding to a whole block or ignoring floors. */
    public static double floorLift(AABB box, List<AABB> obstacles, double stepHeight) {
        double lift = 0;
        AABB probe = box.deflate(EPSILON);
        for (AABB obstacle : obstacles) {
            if (!probe.intersects(obstacle)) continue;
            double rise = obstacle.maxY - box.minY;
            if (rise > stepHeight + EPSILON) return Double.NaN;
            lift = Math.max(lift, rise + EPSILON);
        }
        return lift;
    }
}
