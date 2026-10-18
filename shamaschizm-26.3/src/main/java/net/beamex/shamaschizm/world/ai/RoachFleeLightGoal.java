package net.beamex.shamaschizm.world.ai;

import java.util.EnumSet;
import net.beamex.shamaschizm.entity.custom.RoachEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

/** Makes a roach immediately seek a reachable position darker than its current one. */
public final class RoachFleeLightGoal extends Goal {
    private static final int FLEE_AT_LIGHT = 7;
    private static final int SAFE_LIGHT = 4;

    private final RoachEntity roach;
    private final double speedModifier;
    private Vec3 destination;

    public RoachFleeLightGoal(RoachEntity roach, double speedModifier) {
        this.roach = roach;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        int currentLight = this.roach.level().getMaxLocalRawBrightness(this.roach.blockPosition());
        if (currentLight < FLEE_AT_LIGHT) return false;

        Vec3 candidate = DefaultRandomPos.getPos(this.roach, 12, 5);
        if (candidate == null) return false;

        int candidateLight = this.roach.level().getMaxLocalRawBrightness(
                net.minecraft.core.BlockPos.containing(candidate));
        if (candidateLight >= currentLight) return false;

        this.destination = candidate;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return !this.roach.getNavigation().isDone()
                && this.roach.level().getMaxLocalRawBrightness(this.roach.blockPosition()) > SAFE_LIGHT;
    }

    @Override
    public void start() {
        this.roach.getNavigation().moveTo(
                this.destination.x, this.destination.y, this.destination.z, this.speedModifier);
    }

    @Override
    public void stop() {
        this.destination = null;
    }
}
