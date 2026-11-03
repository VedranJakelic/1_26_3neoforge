package net.beamex.shamaschizm.livingsmoke;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Three-second pursuer followed by vanilla lightning at its final position. */
public final class Kugelblitz extends Entity {
    private static final int LIFE = 60;
    public Kugelblitz(EntityType<? extends Kugelblitz> type, Level level) {
        super(type, level); setNoGravity(true);
    }
    @Override
    public boolean hurtServer(ServerLevel server, DamageSource source, float amount) {
        return false;
    }
    @Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder b) {}
    @Override protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput out) {
        out.putInt("KugelblitzLife", tickCount);
    }
    @Override protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput in) {
        tickCount = in.getIntOr("KugelblitzLife", 0);
    }
    @Override public void tick() {
        super.tick();
        if (level().isClientSide()) {
            for (int i = 0; i < 8; i++) {
                double dx = (random.nextDouble() - .5) * .65;
                double dy = (random.nextDouble() - .5) * .65;
                double dz = (random.nextDouble() - .5) * .65;
                level().addParticle(i % 3 == 0 ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.SQUID_INK,
                        getX() + dx, getY() + dy, getZ() + dz,
                        -.015 * dx, -.015 * dy, -.015 * dz);
            }
            return;
        }
        if (!(level() instanceof ServerLevel server)) return;
        if (tickCount >= LIFE) {
            var lightning = EntityTypes.LIGHTNING_BOLT.create(server, EntitySpawnReason.TRIGGERED);
            if (lightning != null) {
                lightning.setPos(getX(), getY(), getZ());
                server.addFreshEntity(lightning);
            }
            discard();
            return;
        }
        if (tickCount % 14 == 1) {
            server.playSound(null, getX(), getY(), getZ(), SoundEvents.WARDEN_HEARTBEAT,
                    SoundSource.HOSTILE, 1.6F, .55F + random.nextFloat() * .1F);
        }
        net.minecraft.server.level.ServerPlayer player = null;
        double nearest = 24 * 24;
        for (net.minecraft.server.level.ServerPlayer candidate : server.players()) {
            double distance = candidate.distanceToSqr(this);
            if (candidate.isAlive() && !candidate.isCreative() && !candidate.isSpectator() && distance < nearest) {
                nearest = distance;
                player = candidate;
            }
        }
        if (player != null) {
            Vec3 delta = player.getEyePosition().subtract(position());
            if (delta.lengthSqr() > .04) {
                Vec3 movement = delta.normalize().scale(.055);
                if (server.noCollision(this, getBoundingBox().expandTowards(movement))) {
                    setPos(getX() + movement.x, getY() + movement.y, getZ() + movement.z);
                }
            }
        }
    }
}
