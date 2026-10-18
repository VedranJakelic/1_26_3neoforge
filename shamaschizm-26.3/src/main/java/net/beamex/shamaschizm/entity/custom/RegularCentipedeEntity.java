package net.beamex.shamaschizm.entity.custom;

import net.beamex.shamaschizm.world.Schizm;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Small, slow, silent Schizm centipede composed of five to ten visual sections. */
public final class RegularCentipedeEntity extends RoachEntity {
    private static final EntityDataAccessor<Integer> SECTION_COUNT =
            SynchedEntityData.defineId(RegularCentipedeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Direction> SUPPORT =
            SynchedEntityData.defineId(RegularCentipedeEntity.class, EntityDataSerializers.DIRECTION);
    private static final EntityDataAccessor<Boolean> CURLED =
            SynchedEntityData.defineId(RegularCentipedeEntity.class, EntityDataSerializers.BOOLEAN);
    private int curlTicks;

    public RegularCentipedeEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setSilent(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.095D)
                .add(Attributes.FOLLOW_RANGE, 14.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.10D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder data) {
        super.defineSynchedData(data);
        data.define(SECTION_COUNT, 5);
        data.define(SUPPORT, Direction.DOWN);
        data.define(CURLED, false);
    }

    public int getSectionCount() {
        return this.entityData.get(SECTION_COUNT);
    }

    private void setSectionCount(int sections) {
        this.entityData.set(SECTION_COUNT, Math.max(5, Math.min(10, sections)));
    }

    public Direction getSupportDirection() {
        return this.entityData.get(SUPPORT);
    }

    public boolean isCurled() {
        return this.entityData.get(CURLED);
    }

    @Override
    public void tick() {
        super.tick();
        this.fallDistance = 0.0D;
        if (!(this.level() instanceof ServerLevel level)) return;

        updateSupport(level);
        if (this.curlTicks > 0) {
            this.curlTicks--;
            this.entityData.set(CURLED, true);
            this.getNavigation().stop();
            this.setDeltaMovement(Vec3.ZERO);
            if (this.curlTicks == 0) this.entityData.set(CURLED, false);
            return;
        }

        // Curl only while calm and in darkness, so fleeing from light or damage
        // always wins over the cosmetic resting behavior.
        if (!this.isInWater() && this.getLastHurtByMob() == null
                && level.getMaxLocalRawBrightness(this.blockPosition()) <= 7
                && this.random.nextInt(500) == 0) {
            this.curlTicks = 60 + this.random.nextInt(81);
            this.entityData.set(CURLED, true);
            return;
        }

        applySurfaceMovement(level);
    }

    private void updateSupport(ServerLevel level) {
        Direction current = getSupportDirection();
        if (hasSurface(level, current)) return;

        Direction best = null;
        double bestDistance = Double.MAX_VALUE;
        Vec3 center = this.getBoundingBox().getCenter();
        for (Direction direction : Direction.values()) {
            BlockPos surface = this.blockPosition().relative(direction);
            if (!hasCollision(level, surface)) continue;
            double distance = center.distanceToSqr(Vec3.atCenterOf(surface));
            if (distance < bestDistance) {
                best = direction;
                bestDistance = distance;
            }
        }
        this.entityData.set(SUPPORT, best == null ? Direction.DOWN : best);
    }

    private void applySurfaceMovement(ServerLevel level) {
        Vec3 velocity = this.getDeltaMovement();
        Direction support = getSupportDirection();

        // A wall directly in its route becomes the new floor, as it does for
        // the giant centipede. Keep the correction deliberately slow.
        if (this.horizontalCollision) {
            Direction ahead = Direction.getApproximateNearest(velocity.x, 0.0D, velocity.z);
            if (ahead.getAxis().isHorizontal() && hasSurface(level, ahead)) {
                support = ahead;
                this.entityData.set(SUPPORT, ahead);
            }
        }

        if (support.getAxis().isHorizontal()) {
            // Climb or descend a wall according to the current navigation target.
            double climb = this.getNavigation().getTargetPos() != null
                    && this.getNavigation().getTargetPos().getY() < this.getY() ? -0.055D : 0.055D;
            this.setDeltaMovement(velocity.x, climb, velocity.z);
        } else if (support == Direction.UP) {
            this.setNoGravity(true);
            this.setDeltaMovement(velocity.x, 0.025D, velocity.z);
        } else {
            this.setNoGravity(false);
        }
    }

    private boolean hasSurface(ServerLevel level, Direction direction) {
        return hasCollision(level, this.blockPosition().relative(direction));
    }

    private static boolean hasCollision(ServerLevel level, BlockPos pos) {
        return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    public static boolean canRegularCentipedeSpawn(EntityType<RegularCentipedeEntity> type,
                                                   ServerLevelAccessor level,
                                                   EntitySpawnReason reason,
                                                   BlockPos pos, RandomSource random) {
        if (reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.CHUNK_GENERATION) return false;
        return level.getLevel().dimension().equals(Schizm.KEY)
                && level.getMaxLocalRawBrightness(pos) <= 7
                && Mob.checkMobSpawnRules(type, level, reason, pos, random);
    }

    @Override
    @SuppressWarnings("deprecation")
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level,
                                                   DifficultyInstance difficulty,
                                                   EntitySpawnReason reason,
                                                   @Nullable SpawnGroupData groupData) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, groupData);
        this.setSectionCount(5 + this.random.nextInt(6));
        this.setSilent(true);
        return result;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        // Regular centipedes are completely silent.
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource source) {
        return null;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return null;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("RegularCentipedeSections", this.getSectionCount());
        output.putInt("RegularCentipedeSupport", this.getSupportDirection().get3DDataValue());
        output.putInt("RegularCentipedeCurlTicks", this.curlTicks);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setSectionCount(input.getIntOr("RegularCentipedeSections", 5));
        this.entityData.set(SUPPORT, Direction.from3DDataValue(
                input.getIntOr("RegularCentipedeSupport", Direction.DOWN.get3DDataValue())));
        this.curlTicks = Math.max(0, input.getIntOr("RegularCentipedeCurlTicks", 0));
        this.entityData.set(CURLED, this.curlTicks > 0);
        this.setSilent(true);
    }
}
