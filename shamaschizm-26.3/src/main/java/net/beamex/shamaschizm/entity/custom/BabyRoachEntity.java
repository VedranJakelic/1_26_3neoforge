package net.beamex.shamaschizm.entity.custom;

import net.beamex.shamaschizm.entity.ModEntities;
import net.beamex.shamaschizm.world.Schizm;
import net.beamex.shamaschizm.world.ai.BabyRoachFollowAdultGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.SpawnUtil;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

public final class BabyRoachEntity extends RoachEntity {
    public static final String PENDING_FAMILY_SIZE_TAG = "ShamaschizmPendingBabyRoaches";

    public BabyRoachEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 12.0D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.27D)
                .add(Attributes.FOLLOW_RANGE, 20.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.20D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(3, new BabyRoachFollowAdultGoal(this, 0.85D, 16.0D));
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        // Baby roaches are silent.
    }

    public static boolean canBabySpawn(EntityType<BabyRoachEntity> type, ServerLevelAccessor level,
                                       EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        return level.getLevel().dimension().equals(Schizm.KEY)
                && level.getMaxLocalRawBrightness(pos) <= 7
                && Mob.checkMobSpawnRules(type, level, reason, pos, random);
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor level, EntitySpawnReason reason) {
        return this.level().dimension().equals(Schizm.KEY)
                && level.getMaxLocalRawBrightness(this.blockPosition()) <= 7
                && super.checkSpawnRules(level, reason);
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 10;
    }

    public static void spawnFamilyAround(ServerLevel level, RoachEntity adult, int familySize) {
        for (int i = 0; i < familySize; i++) {
            SpawnUtil.trySpawnMob(
                    ModEntities.BABY_ROACH.get(),
                    EntitySpawnReason.EVENT,
                    level,
                    adult.blockPosition(),
                    8,
                    5,
                    3,
                    SpawnUtil.Strategy.ON_TOP_OF_COLLIDER_NO_LEAVES,
                    true
            );
        }
    }
}
