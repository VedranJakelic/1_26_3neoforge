package net.beamex.shamaschizm.world.ai;

import java.util.EnumSet;
import net.beamex.shamaschizm.entity.ModEntities;
import net.beamex.shamaschizm.entity.custom.BabyRoachEntity;
import net.beamex.shamaschizm.entity.custom.RoachEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import org.jspecify.annotations.Nullable;

/** Follows only ordinary adult roaches which the baby can currently see. */
public final class BabyRoachFollowAdultGoal extends Goal {
    private static final double START_DISTANCE_SQR = 3.0D * 3.0D;
    private static final double STOP_DISTANCE_SQR = 2.0D * 2.0D;

    private final BabyRoachEntity baby;
    private final double speedModifier;
    private final double searchDistance;
    private @Nullable RoachEntity adult;
    private int pathRecalculationTicks;

    public BabyRoachFollowAdultGoal(BabyRoachEntity baby, double speedModifier,
                                    double searchDistance) {
        this.baby = baby;
        this.speedModifier = speedModifier;
        this.searchDistance = searchDistance;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        RoachEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (RoachEntity candidate : this.baby.level().getEntitiesOfClass(
                RoachEntity.class,
                this.baby.getBoundingBox().inflate(this.searchDistance),
                candidate -> candidate.isAlive()
                        && candidate.getType() == ModEntities.ROACH.get()
                        && this.baby.getSensing().hasLineOfSight(candidate))) {
            double distance = this.baby.distanceToSqr(candidate);
            if (distance > START_DISTANCE_SQR && distance < nearestDistance) {
                nearest = candidate;
                nearestDistance = distance;
            }
        }
        this.adult = nearest;
        return nearest != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.adult != null
                && this.adult.isAlive()
                && this.baby.distanceToSqr(this.adult) > STOP_DISTANCE_SQR
                && this.baby.distanceToSqr(this.adult) <= this.searchDistance * this.searchDistance
                && this.baby.getSensing().hasLineOfSight(this.adult);
    }

    @Override
    public void start() {
        this.pathRecalculationTicks = 0;
    }

    @Override
    public void stop() {
        this.baby.getNavigation().stop();
        this.adult = null;
    }

    @Override
    public void tick() {
        if (this.adult == null) return;
        this.baby.getLookControl().setLookAt(this.adult, 20.0F, this.baby.getMaxHeadXRot());
        if (--this.pathRecalculationTicks <= 0) {
            this.pathRecalculationTicks = this.adjustedTickDelay(10);
            this.baby.getNavigation().moveTo(this.adult, this.speedModifier);
        }
    }
}
