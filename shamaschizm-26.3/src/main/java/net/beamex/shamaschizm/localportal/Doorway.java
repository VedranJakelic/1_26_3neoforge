package net.beamex.shamaschizm.localportal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.phys.Vec3;

/** Upright, rigid doorway. Origin is the bottom centre; normal points into the approach room. */
public record Doorway(String name, Vec3 origin, Facing facing, double width, double height,
                      String target, boolean enabled) {
    public static final int VIEW_DEPTH = 15;
    public static final int MAX_DOORWAYS = 128;
    public static final Codec<Doorway> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(Doorway::name),
            Vec3.CODEC.fieldOf("origin").forGetter(Doorway::origin),
            Codec.STRING.xmap(Facing::valueOf, Facing::name).fieldOf("facing").forGetter(Doorway::facing),
            Codec.doubleRange(1, 8).fieldOf("width").forGetter(Doorway::width),
            Codec.doubleRange(2, 8).fieldOf("height").forGetter(Doorway::height),
            Codec.STRING.optionalFieldOf("target", "").forGetter(Doorway::target),
            Codec.BOOL.optionalFieldOf("enabled", false).forGetter(Doorway::enabled)
    ).apply(i, Doorway::new));

    public enum Facing {
        NORTH(0, -1), EAST(1, 0), SOUTH(0, 1), WEST(-1, 0);
        public final Vec3 normal, right;
        Facing(int x, int z) { normal = new Vec3(x, 0, z); right = new Vec3(z, 0, -x); }
    }

    /** Snap X/Z to the containing block's centre and the bottom to that block's base. */
    public Doorway blockAligned() {
        Vec3 aligned = new Vec3(Math.floor(origin.x) + 0.5, Math.floor(origin.y), Math.floor(origin.z) + 0.5);
        return aligned.equals(origin) ? this : new Doorway(name, aligned, facing, width, height, target, enabled);
    }

    public Doorway link(String destination, boolean active) {
        return new Doorway(name, origin, facing, width, height, destination, active);
    }

    public double distance(Vec3 point) { return point.subtract(origin).dot(facing.normal); }

    public Vec3 local(Vec3 point) {
        Vec3 d = point.subtract(origin);
        return new Vec3(d.dot(facing.right), d.y, d.dot(facing.normal));
    }

    public Vec3 world(double x, double y, double z) {
        return origin.add(facing.right.scale(x)).add(0, y, 0).add(facing.normal.scale(z));
    }

    /** 180 degrees in doorway-local space: entering A emerges from the front of B. */
    public Vec3 transformVector(Doorway destination, Vec3 vector) {
        return destination.facing.right.scale(-vector.dot(facing.right))
                .add(0, vector.y, 0)
                .add(destination.facing.normal.scale(-vector.dot(facing.normal)));
    }

    public Vec3 transformPoint(Doorway destination, Vec3 point) {
        return destination.origin.add(transformVector(destination, point.subtract(origin)));
    }

    /** Swept crossing, with enough aperture clearance for the complete entity. */
    public boolean crossed(Vec3 previous, Vec3 current, double entityWidth, double entityHeight) {
        double a = distance(previous), b = distance(current);
        if (a <= 0 || b > 0 || a - b < 1.0e-8) return false;
        Vec3 intersection = local(previous.lerp(current, a / (a - b)));
        return Math.abs(intersection.x) + entityWidth / 2 <= width / 2
                && intersection.y >= -0.02 && intersection.y + entityHeight <= height + 0.02;
    }
}
