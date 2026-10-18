package net.beamex.shamaschizm.entity.custom;

import net.beamex.shamaschizm.entity.CiglunRegistration;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** A persistent, stationary sentry which becomes hostile when a nearby player provokes it. */
public final class CiglunEntity extends PathfinderMob {
    public static final int IDLE = 0;
    public static final int TURNING = 1;
    public static final int ATTACKING = 2;
    public static final int COOLDOWN = 3;

    private static final int TURN_LENGTH = 115;       // turnsaround2: 5.75 seconds
    private static final int OPEN_LENGTH = 120;       // open: 6 seconds
    private static final int COOLDOWN_LENGTH = 60;
    private static final int LASER_START = 45;
    private static final int LASER_DAMAGE_TICK = 55;
    private static final int LASER_END = 70;
    public static final double PROVOCATION_RANGE = 32.0D;
    private static final double MAX_TARGET_DISTANCE_SQR = PROVOCATION_RANGE * PROVOCATION_RANGE;

    private static final EntityDataAccessor<Integer> ACTION =
            SynchedEntityData.defineId(CiglunEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ACTION_TICKS =
            SynchedEntityData.defineId(CiglunEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LASER_TARGET =
            SynchedEntityData.defineId(CiglunEntity.class, EntityDataSerializers.INT);

    private final AnimationState clientTurningAnimation = new AnimationState();
    private final AnimationState clientOpenAnimation = new AnimationState();
    private int clientAnimationAction = Integer.MIN_VALUE;

    public CiglunEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.setPersistenceRequired();
        this.setYRot(180.0F);
        this.yBodyRot = 180.0F;
        this.yHeadRot = 180.0F;
        this.xpReward = 15;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 80.0D)
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 6.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    @Override
    protected void registerGoals() {
        // Deliberately empty: this mob never walks and never selects an unprovoked target.
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder data) {
        super.defineSynchedData(data);
        data.define(ACTION, IDLE);
        data.define(ACTION_TICKS, 0);
        data.define(LASER_TARGET, 0);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                                   EntitySpawnReason reason, @Nullable SpawnGroupData groupData) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, groupData);
        this.setYRot(180.0F);
        this.yBodyRot = 180.0F;
        this.yHeadRot = 180.0F;
        return result;
    }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(Vec3.ZERO);
        this.fallDistance = 0.0F;
        if (this.level().isClientSide()) {
            this.updateClientAnimationState();
            return;
        }

        LivingEntity target = this.getTarget();
        if (!isValidTarget(target)) {
            becomePassive();
            return;
        }

        int action = getAction();
        int ticks = getActionTicks() + 1;
        this.entityData.set(ACTION_TICKS, ticks);

        if (action == TURNING) {
            this.entityData.set(LASER_TARGET, 0);
            // The visible turn happens through the middle portion of turnsaround2.
            if (ticks >= 35 && ticks <= 85) faceTarget(target, 7.0F, 180.0F);
            if (ticks >= TURN_LENGTH) {
                // turnsaround2 ends with the model rotated 180 degrees internally.
                // Removing that animation and this offset on the same tick keeps
                // the visible front aimed at the player without a visual reversal.
                faceTarget(target, 360.0F, 0.0F);
                beginAction(ATTACKING);
            }
        } else if (action == ATTACKING) {
            faceTarget(target, 4.0F, 0.0F);
            boolean clearShot = this.distanceToSqr(target) <= MAX_TARGET_DISTANCE_SQR
                    && this.hasLineOfSight(target);
            if (ticks == LASER_START && clearShot) {
                this.playSound(CiglunRegistration.CIGLUN_LASER, 2.0F, 1.0F);
            }
            this.entityData.set(LASER_TARGET,
                    clearShot && ticks >= LASER_START && ticks <= LASER_END ? target.getId() : 0);
            if (ticks == LASER_DAMAGE_TICK && clearShot && this.level() instanceof ServerLevel serverLevel) {
                target.igniteForSeconds(5.0F);
                target.hurtServer(serverLevel, this.damageSources().indirectMagic(this, this), 8.0F);
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                        18, 0.25D, 0.35D, 0.25D, 0.08D);
            }
            if (ticks >= OPEN_LENGTH) beginAction(COOLDOWN);
        } else if (action == COOLDOWN && ticks >= COOLDOWN_LENGTH) {
            beginAction(ATTACKING);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof Player player && player.isAlive() && !player.isSpectator()) {
            this.provoke(player);
        }

        boolean struckWithPickaxe = source.getDirectEntity() instanceof Player player
                && player.getMainHandItem().is(ItemTags.PICKAXES);
        if (struckWithPickaxe) {
            amount *= 2.0F;
        } else if (isShelled()) {
            amount = Math.min(0.5F, amount * 0.10F);
        }
        return super.hurtServer(level, source, amount);
    }

    /**
     * Starts or redirects the existing attack sequence toward an offending
     * player. Repeated offenses do not restart an animation already in progress.
     */
    public void provoke(Player player) {
        if (!player.isAlive()
                || player.isSpectator()
                || player.isCreative()
                || player.level() != this.level()
                || this.distanceToSqr(player) > MAX_TARGET_DISTANCE_SQR) return;

        this.setTarget(player);
        if (getAction() == IDLE) beginAction(TURNING);
    }

    private boolean isShelled() {
        return getAction() != ATTACKING || getActionTicks() < 20 || getActionTicks() > 90;
    }

    private boolean isValidTarget(@Nullable LivingEntity target) {
        return target instanceof Player player
                && player.isAlive()
                && !player.isSpectator()
                && !player.isCreative()
                && player.level() == this.level()
                && this.distanceToSqr(player) <= MAX_TARGET_DISTANCE_SQR;
    }

    private void faceTarget(LivingEntity target, float maximumStep, float animationOffset) {
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        float wanted = (float)(Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F + animationOffset;
        float facing = Mth.approachDegrees(this.getYRot(), wanted, maximumStep);
        this.setYRot(facing);
        this.yBodyRot = facing;
        this.yHeadRot = facing;
    }

    private void beginAction(int action) {
        this.entityData.set(ACTION, action);
        this.entityData.set(ACTION_TICKS, 0);
        this.entityData.set(LASER_TARGET, 0);
        if (action == TURNING) {
            this.playSound(CiglunRegistration.CIGLUN_3, 1.0F, 1.0F);
            this.playSound(CiglunRegistration.CIGLUN_4, 1.0F, 1.0F);
        } else if (action == ATTACKING) {
            this.playSound(CiglunRegistration.CIGLUN_4, 1.2F, 1.0F);
        }
    }

    private void becomePassive() {
        this.setTarget(null);
        if (getAction() != IDLE) beginAction(IDLE);
    }

    public int getAction() {
        return this.entityData.get(ACTION);
    }

    public int getActionTicks() {
        return this.entityData.get(ACTION_TICKS);
    }

    private void updateClientAnimationState() {
        int action = this.getAction();
        if (action == this.clientAnimationAction) return;

        this.clientTurningAnimation.stop();
        this.clientOpenAnimation.stop();
        int authoritativeStartTick = this.tickCount - this.getActionTicks();
        if (action == TURNING) {
            this.clientTurningAnimation.start(authoritativeStartTick);
        } else if (action == ATTACKING) {
            this.clientOpenAnimation.start(authoritativeStartTick);
        }
        this.clientAnimationAction = action;
    }

    public AnimationState getClientTurningAnimation() {
        return this.clientTurningAnimation;
    }

    public AnimationState getClientOpenAnimation() {
        return this.clientOpenAnimation;
    }

    public Vec3 getCorePosition(float partialTick) {
        Vec3 position = this.getPosition(partialTick);
        return position.add(0.0D, 1.5D, 0.0D);
    }

    public @Nullable LivingEntity getLaserTarget() {
        int id = this.entityData.get(LASER_TARGET);
        return id == 0 ? null : this.level().getEntity(id) instanceof LivingEntity living ? living : null;
    }

    @Override
    protected AABB makeBoundingBox(Vec3 position) {
        EntityDimensions dimensions = this.getDimensions(this.getPose());
        return dimensions.makeBoundingBox(position).move(0.0D, 1.0D, 0.0D);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity entity) {
        // Stationary by design.
    }

    @Override
    public void push(double x, double y, double z) {
        // Stationary by design.
    }

    @Override
    public boolean removeWhenFarAway(double distanceSquared) {
        return false;
    }
}
