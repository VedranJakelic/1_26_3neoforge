package net.beamex.shamaschizm.wisdom;

import net.minecraft.world.phys.Vec3;
import java.util.Arrays;
import java.util.function.BiFunction;

/** Server-side Verlet chain. Collision projection is supplied by the owning level. */
public final class StingerRope {
    public static final int NODES = 5;
    public static final double[] LENGTHS = {.625, .5, .5, .5};
    private final Vec3[] points = new Vec3[NODES], previous = new Vec3[NODES];
    public Vec3 point(int i) { return points[i]; }
    public void reset(Vec3 root) {
        double down = 0;
        for (int i = 0; i < NODES; i++) {
            if (i > 0) down += LENGTHS[i - 1];
            points[i] = previous[i] = root.add(0, -down, 0);
        }
    }
    public void tick(Vec3 root, Vec3 attack, BiFunction<Vec3, Vec3, Vec3> collision) {
        if (points[0] == null || points[0].distanceToSqr(root) > 16) reset(root);
        Vec3[] before = Arrays.copyOf(points, NODES);
        previous[0] = points[0]; points[0] = root;
        for (int i = 1; i < NODES; i++) {
            Vec3 velocity = points[i].subtract(previous[i]).scale(.88).add(0, -.025, 0);
            if (attack != null && i == NODES - 1) {
                Vec3 pull = attack.subtract(points[i]);
                velocity = velocity.add(pull.normalize().scale(Math.min(.24, pull.length() * .3)));
            }
            previous[i] = points[i];
            points[i] = collision.apply(points[i], points[i].add(velocity));
        }
        for (int iteration = 0; iteration < 12; iteration++) {
            points[0] = root;
            for (int i = 1; i < NODES; i++) {
                Vec3 d = points[i].subtract(points[i - 1]); double distance = d.length();
                if (distance < 1e-8) d = new Vec3(0, -1, 0); else d = d.scale(1 / distance);
                Vec3 correction = d.scale(distance - LENGTHS[i - 1]);
                if (i == 1) points[i] = points[i].subtract(correction);
                else { points[i - 1] = points[i - 1].add(correction.scale(.5)); points[i] = points[i].subtract(correction.scale(.5)); }
            }
        }
        // Keep the chain on the root's side of solid surfaces, even during a thrust.
        points[0] = root;
        for (int i = 1; i < NODES; i++) {
            points[i] = collision.apply(before[i], points[i]);
            points[i] = collision.apply(points[i - 1], points[i]);
        }
    }
}
