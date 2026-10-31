package net.beamex.shamaschizm.wisdom;

import java.util.Random;
import net.minecraft.world.phys.Vec3;

/** Stable body-local anchors; animation never changes the selected surface. */
public final class WisdomAttachments {
    public record Anchor(Vec3 point, Vec3 normal, double phase, double length) {}
    private WisdomAttachments() {}
    public static int count(int seed) { return 4 + Math.floorMod(seed, 7); }
    public static Anchor[] create(int seed) {
        Random random = new Random(seed);
        Anchor[] result = new Anchor[count(seed)];
        for (int i = 0; i < result.length; i++) {
            // The main cube is [-7,7] x [38,49] x [-8,6] in the supplied model.
            double x = (random.nextDouble() * 10 - 5) / 16;
            double y = (2 + random.nextDouble() * 7) / 16;
            double z = (random.nextDouble() * 10 - 6) / 16;
            Vec3 normal;
            switch (i % 5) {
                case 0 -> { x = -7.0 / 16; normal = new Vec3(-1, 0, 0); }
                case 1 -> { x = 7.0 / 16; normal = new Vec3(1, 0, 0); }
                case 2 -> { z = -8.0 / 16; normal = new Vec3(0, 0, -1); }
                case 3 -> { z = 8.0 / 16; x *= .8; y = Math.min(y, 9.0 / 16); normal = new Vec3(0, 0, 1); }
                default -> { y = 12.0 / 16; normal = new Vec3(0, 1, 0); }
            }
            result[i] = new Anchor(new Vec3(x, y, z), normal, random.nextDouble() * Math.PI * 2, .38 + random.nextDouble() * .25);
        }
        return result;
    }
    // The supplied model faces -Z; Minecraft's yaw zero faces +Z.
    // Share this correction between the rendered brain/worms and death-release positions.
    public static Vec3 rotate(Vec3 p, float yaw) { return p.yRot((float)Math.toRadians(180 - yaw)); }
}
