package net.beamex.shamaschizm.world.ai;

import java.util.EnumSet;
import java.util.List;
import net.beamex.shamaschizm.event.SchizmSpawnEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Keeps a shield zombie between its nearby group and the attacked player. */
public final class SchizmShieldFrontlinerGoal extends Goal {
    private static final double GROUP_RADIUS = 10.0D;
    private static final double GUARD_DISTANCE = 2.15D;
    private static final double ARRIVAL_DISTANCE_SQUARED = 0.55D * 0.55D;
    private static final double MOVE_SPEED = 1.25D;

    private final Zombie zombie;
    private Vec3 guardPoint;

    public SchizmShieldFrontlinerGoal(Zombie zombie) {
        this.zombie = zombie;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return validTarget() != null && hasShield() && isMarkedFrontliner();
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.raiseShield();
        this.updateGuardPoint(validTarget());
    }

    @Override
    public void stop() {
        this.zombie.getNavigation().stop();
        if (this.zombie.isUsingItem() && this.zombie.getUsedItemHand() == InteractionHand.OFF_HAND) {
            this.zombie.stopUsingItem();
        }
        this.guardPoint = null;
    }

    @Override
    public void tick() {
        Player target = validTarget();
        if (target == null) return;

        this.zombie.getLookControl().setLookAt(target, 45.0F, 45.0F);
        this.zombie.lookAt(target, 45.0F, 45.0F);
        this.raiseShield();

        if (this.guardPoint == null || this.zombie.tickCount % 10 == 0) {
            this.updateGuardPoint(target);
        }
        if (this.guardPoint == null) return;

        if (this.zombie.distanceToSqr(this.guardPoint) > ARRIVAL_DISTANCE_SQUARED) {
            if (!this.zombie.getNavigation().moveTo(
                    this.guardPoint.x, this.guardPoint.y, this.guardPoint.z, MOVE_SPEED)) {
                // If the ideal point is blocked, closing directly on the player
                // still keeps this zombie ahead of most of its group.
                this.zombie.getNavigation().moveTo(target, MOVE_SPEED);
            }
        } else {
            this.zombie.getNavigation().stop();
        }
    }

    private void updateGuardPoint(Player target) {
        if (target == null) {
            this.guardPoint = null;
            return;
        }

        AABB groupArea = this.zombie.getBoundingBox().inflate(GROUP_RADIUS, 4.0D, GROUP_RADIUS);
        List<Zombie> group = this.zombie.level().getEntitiesOfClass(
                Zombie.class,
                groupArea,
                other -> other.isAlive() && other.getTarget() == target);

        double centerX = this.zombie.getX();
        double centerZ = this.zombie.getZ();
        if (!group.isEmpty()) {
            centerX = group.stream().mapToDouble(Zombie::getX).average().orElse(centerX);
            centerZ = group.stream().mapToDouble(Zombie::getZ).average().orElse(centerZ);
        }

        Vec3 towardGroup = new Vec3(centerX - target.getX(), 0.0D, centerZ - target.getZ());
        if (towardGroup.lengthSqr() < 1.0E-4D) {
            towardGroup = new Vec3(this.zombie.getX() - target.getX(), 0.0D, this.zombie.getZ() - target.getZ());
        }
        if (towardGroup.lengthSqr() < 1.0E-4D) {
            towardGroup = Vec3.directionFromRotation(0.0F, target.getYRot());
        }

        Vec3 direction = towardGroup.normalize();
        this.guardPoint = new Vec3(
                target.getX() + direction.x * GUARD_DISTANCE,
                target.getY(),
                target.getZ() + direction.z * GUARD_DISTANCE);
    }

    private void raiseShield() {
        if (!this.zombie.isUsingItem() || this.zombie.getUsedItemHand() != InteractionHand.OFF_HAND) {
            this.zombie.startUsingItem(InteractionHand.OFF_HAND);
        }
    }

    private boolean hasShield() {
        return this.zombie.getOffhandItem().has(DataComponents.BLOCKS_ATTACKS);
    }

    private boolean isMarkedFrontliner() {
        return this.zombie.getPersistentData().getIntOr(SchizmSpawnEvents.SHIELD_FRONTLINER_TAG, 0) == 1;
    }

    private Player validTarget() {
        if (!(this.zombie.getTarget() instanceof Player player)
                || !player.isAlive()
                || player.isCreative()
                || player.isSpectator()) {
            return null;
        }
        return player;
    }
}
