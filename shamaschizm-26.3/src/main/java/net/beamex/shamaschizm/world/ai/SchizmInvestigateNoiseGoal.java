package net.beamex.shamaschizm.world.ai;

import java.util.EnumSet;
import net.beamex.shamaschizm.world.Schizm;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Makes Schizm zombies investigate the player or position responsible for a recent noise. */
public final class SchizmInvestigateNoiseGoal extends Goal {
    private final Zombie zombie;
    private SchizmNoiseTracker.Noise noise;
    private Vec3 destination;
    private int nextPathUpdate;

    public SchizmInvestigateNoiseGoal(Zombie zombie) {
        this.zombie = zombie;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean enabled() {
        return zombie.level() instanceof ServerLevel
                && zombie.level().dimension().equals(Schizm.KEY)
                && zombie.isAlive() && !zombie.isNoAi() && !zombie.isPassenger();
    }

    @Override
    public boolean canUse() {
        if (!enabled() || zombie.getTarget() != null) return false;
        noise = SchizmNoiseTracker.loudestAudible(zombie);
        if (noise == null) return false;
        destination = destination();
        return zombie.getNavigation().createPath(destination.x, destination.y, destination.z, 1) != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (!enabled() || zombie.getTarget() != null || noise == null) return false;
        SchizmNoiseTracker.Noise current = SchizmNoiseTracker.loudestAudible(zombie);
        if (current == null) return false;
        noise = current;
        destination = destination();
        return zombie.distanceToSqr(destination) > 2.25D;
    }

    @Override
    public void start() {
        nextPathUpdate = 0;
        moveToNoise();
    }

    @Override
    public void tick() {
        destination = destination();
        zombie.getLookControl().setLookAt(destination.x, destination.y, destination.z);
        if (--nextPathUpdate <= 0) {
            nextPathUpdate = 10;
            moveToNoise();
        }

        Player player = noise.player((ServerLevel)zombie.level());
        if (player != null && zombie.distanceToSqr(player) <= 30.0D * 30.0D
                && zombie.getSensing().hasLineOfSight(player) && zombie.canAttack(player)) {
            zombie.setTarget(player);
        }
    }

    private Vec3 destination() {
        Player player = noise.player((ServerLevel)zombie.level());
        return player != null && player.isAlive() && !player.isCreative() && !player.isSpectator()
                ? player.position() : noise.position();
    }

    private void moveToNoise() {
        zombie.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.08D);
    }

    @Override
    public void stop() {
        zombie.getNavigation().stop();
        noise = null;
        destination = null;
    }
}
