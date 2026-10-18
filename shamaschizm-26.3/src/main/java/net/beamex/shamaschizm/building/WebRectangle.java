package net.beamex.shamaschizm.building;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Upright rectangle. Position is its bottom centre; yaw rotates its horizontal edge. */
public record WebRectangle(Vec3 bottom, double width, double height, double yaw) {
    public double ux() { return Math.cos(Math.toRadians(yaw)); }
    public double uz() { return Math.sin(Math.toRadians(yaw)); }
    public AABB bounds() {
        double x = Math.abs(ux()) * width / 2, z = Math.abs(uz()) * width / 2;
        return new AABB(bottom.x - x, bottom.y, bottom.z - z,
                bottom.x + x, bottom.y + height, bottom.z + z).inflate(0.025);
    }
    /** Exact segment/plane intersection, also over transparent texture pixels. */
    public Vec3 clip(Vec3 from, Vec3 to) {
        Vec3 delta = to.subtract(from), relative = from.subtract(bottom);
        double denominator = -uz() * delta.x + ux() * delta.z;
        if (Math.abs(denominator) < 1.0E-9) return null;
        double t = -(-uz() * relative.x + ux() * relative.z) / denominator;
        if (t < 0 || t > 1) return null;
        Vec3 point = from.add(delta.scale(t)), local = point.subtract(bottom);
        double horizontal = local.x * ux() + local.z * uz();
        return Math.abs(horizontal) <= width / 2 + 1.0E-6
                && local.y >= -1.0E-6 && local.y <= height + 1.0E-6 ? point : null;
    }
    /** Separating-axis test for a rectangle and an entity's axis-aligned body box. */
    public boolean intersects(AABB box) {
        if (box.maxY < bottom.y || box.minY > bottom.y + height || !bounds().intersects(box)) return false;
        double x = (box.minX + box.maxX) / 2 - bottom.x;
        double z = (box.minZ + box.maxZ) / 2 - bottom.z;
        double rx = (box.maxX - box.minX) / 2, rz = (box.maxZ - box.minZ) / 2;
        return Math.abs(x * ux() + z * uz()) <= width / 2 + rx * Math.abs(ux()) + rz * Math.abs(uz())
                && Math.abs(-x * uz() + z * ux()) <= .025 + rx * Math.abs(uz()) + rz * Math.abs(ux());
    }
}
