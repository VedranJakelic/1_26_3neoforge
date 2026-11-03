package net.beamex.shamaschizm.livingsmoke;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/** One independently falling, bouncing and tumbling pearl. No drops or AI. */
public final class FallingPearl extends Entity {
    public static final int LIFETIME = 90;

    public FallingPearl(EntityType<? extends FallingPearl> type, Level level) {
        super(type, level);
        setNoGravity(true); // We integrate small-object gravity ourselves below.
    }
    @Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {}
    @Override public boolean hurtServer(ServerLevel server, DamageSource source, float amount) { return false; }
    @Override protected void addAdditionalSaveData(ValueOutput out) { out.putInt("PearlAge", tickCount); }
    @Override protected void readAdditionalSaveData(ValueInput in) {
        tickCount = Math.clamp(in.getIntOr("PearlAge", 0), 0, LIFETIME);
    }
    @Override public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel)) return;
        if (tickCount >= LIFETIME) { discard(); return; }

        Vec3 initial = getDeltaMovement();
        Vec3 motion = new Vec3(initial.x * .985,
                Math.max(-.55, initial.y - .035), initial.z * .985);
        Vec3 before = position();
        move(MoverType.SELF, motion);
        Vec3 moved = position().subtract(before);
        boolean xHit = Math.abs(motion.x - moved.x) > 1.0E-5;
        boolean yHit = Math.abs(motion.y - moved.y) > 1.0E-5;
        boolean zHit = Math.abs(motion.z - moved.z) > 1.0E-5;

        double vx = xHit ? -motion.x * .30 : motion.x;
        double vz = zHit ? -motion.z * .30 : motion.z;
        double vy = yHit ? (motion.y < -.05 ? -motion.y * .42 : 0) : motion.y;
        if (onGround() || (yHit && motion.y < 0)) {
            vx *= .78;
            vz *= .78;
            if (Math.abs(vy) < .045) vy = 0;
        }
        if (Math.abs(vx) < .001) vx = 0;
        if (Math.abs(vz) < .001) vz = 0;
        setDeltaMovement(vx, vy, vz);

        // Angular motion slows as the pearl comes to rest.
        float angular = (float)Math.min(1, Math.sqrt(vx*vx + vy*vy + vz*vz) * 5);
        setYRot(getYRot() + 11F * angular);
        setXRot(getXRot() + 8F * angular);
    }
}
