package net.beamex.shamaschizm.entity.custom;

import net.beamex.shamaschizm.sound.ModSounds;
import net.beamex.shamaschizm.entity.ModEntities;
import net.beamex.shamaschizm.world.Schizm;
import net.beamex.shamaschizm.world.ai.RoachFleeLightGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class RoachEntity extends PathfinderMob {
    private static final double OBSERVATION_DISTANCE_SQR = 48.0D * 48.0D;
    private static final double VIEW_CONE_DOT = 0.20D;
    private static final int UNSEEN_DESPAWN_TICKS = 100;
    private static final int VISIBILITY_CHECK_INTERVAL = 10;

    private int unseenTicks;
    private int nextCrawlSoundTick;

    public RoachEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 48.0D)
                .add(Attributes.ARMOR, 12.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.FOLLOW_RANGE, 20.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.60D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new RoachFleeLightGoal(this, 1.45D));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.35D));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.85D));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    public static boolean canSpawn(EntityType<RoachEntity> type, ServerLevelAccessor level,
                                   EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        return level.getLevel().dimension().equals(Schizm.KEY)
                && level.getMaxLocalRawBrightness(pos) <= 7
                && Mob.checkMobSpawnRules(type, level, reason, pos, random);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel level
                && this.getType() == ModEntities.ROACH.get()) {
            int babies = this.getPersistentData().getIntOr(
                    BabyRoachEntity.PENDING_FAMILY_SIZE_TAG, 0);
            if (babies > 0) {
                this.getPersistentData().remove(BabyRoachEntity.PENDING_FAMILY_SIZE_TAG);
                BabyRoachEntity.spawnFamilyAround(level, this, babies);
            }
        }
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        return 15.0F - level.getMaxLocalRawBrightness(pos);
    }

    @Override
    public void checkDespawn() {
        super.checkDespawn();
        if (this.isRemoved() || this.isPersistenceRequired() || this.requiresCustomPersistence()) {
            this.unseenTicks = 0;
            return;
        }
        if (!(this.level() instanceof ServerLevel level)
                || this.tickCount % VISIBILITY_CHECK_INTERVAL != 0) return;

        if (isObservedByAnyPlayer(level)) {
            this.unseenTicks = 0;
        } else {
            this.unseenTicks += VISIBILITY_CHECK_INTERVAL;
            if (this.unseenTicks >= UNSEEN_DESPAWN_TICKS) this.discard();
        }
    }

    private boolean isObservedByAnyPlayer(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            if (!player.isAlive() || player.isSpectator() || player.distanceToSqr(this) > OBSERVATION_DISTANCE_SQR) {
                continue;
            }
            Vec3 towardRoach = this.getBoundingBox().getCenter()
                    .subtract(player.getEyePosition()).normalize();
            if (player.getLookAngle().dot(towardRoach) >= VIEW_CONE_DOT
                    && player.hasLineOfSight(this)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        if (this.tickCount < this.nextCrawlSoundTick) return;
        this.playSound(ModSounds.ROACH_CRAWL.get(), 0.45F,
                0.94F + this.random.nextFloat() * 0.12F);
        this.nextCrawlSoundTick = this.tickCount + 36 + this.random.nextInt(25);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SILVERFISH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SILVERFISH_DEATH;
    }

    @Override
    public boolean removeWhenFarAway(double distanceSquared) {
        return true;
    }
}
