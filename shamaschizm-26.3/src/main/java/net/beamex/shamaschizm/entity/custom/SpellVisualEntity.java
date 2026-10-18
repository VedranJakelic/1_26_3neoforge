package net.beamex.shamaschizm.entity.custom;

import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Four-second visual spell plane which follows a coffin skeleton's head. */
public final class SpellVisualEntity extends PathfinderMob {
    private final AnimationState rotateAnimation = new AnimationState();
    private @Nullable UUID ownerId;
    private int remainingTicks = 80;

    public SpellVisualEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.setPermanentlyInvulnerable(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    @Override protected void registerGoals() {}

    public void bindTo(SkeletonVariantEntity owner) {
        this.ownerId = owner.getUUID();
        this.remainingTicks = 80;
        this.snapTo(owner.getX(), owner.getY() + 2.55D, owner.getZ(), owner.getYRot(), 0.0F);
    }

    public AnimationState getRotateAnimation() { return this.rotateAnimation; }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(Vec3.ZERO);
        if (this.level().isClientSide()) {
            if (!this.rotateAnimation.isStarted()) this.rotateAnimation.start(this.tickCount);
            return;
        }
        if (!(this.level() instanceof ServerLevel level) || this.ownerId == null || --this.remainingTicks <= 0) {
            this.discard();
            return;
        }
        Entity owner = level.getEntity(this.ownerId);
        if (!(owner instanceof SkeletonVariantEntity skeleton) || !skeleton.isAlive()
                || !skeleton.isCoffinSummoning()) {
            this.discard();
            return;
        }
        this.snapTo(skeleton.getX(), skeleton.getY() + 2.55D, skeleton.getZ(),
                skeleton.getYRot(), 0.0F);
    }

    @Override public boolean hurtServer(ServerLevel level, DamageSource source, float amount) { return false; }
    @Override public boolean isPickable() { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public void push(Entity entity) {}
    @Override protected void pushEntities() {}
    @Override public boolean removeWhenFarAway(double distanceSquared) { return false; }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (this.ownerId != null) output.putString("SpellOwner", this.ownerId.toString());
        output.putInt("SpellTicks", this.remainingTicks);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.ownerId = input.getString("SpellOwner").map(value -> {
            try { return UUID.fromString(value); }
            catch (IllegalArgumentException ignored) { return null; }
        }).orElse(null);
        this.remainingTicks = input.getIntOr("SpellTicks", 80);
        this.setNoGravity(true);
        this.setPermanentlyInvulnerable(true);
    }
}
