package net.beamex.shamaschizm.world.ai;

import java.util.EnumSet;
import net.beamex.shamaschizm.world.Schizm;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;

/** Target preference only: vanilla zombie melee AI performs navigation/attacks. */
public final class SchizmAxeShieldGoal extends Goal {
    public static final double TARGET_RADIUS = 24.0D;
    public static final int RUSH_TICKS = 40;
    private final Zombie zombie;
    private Player preferred;
    private Player rushTarget;
    private long rushUntil;
    private long nextScan;

    public SchizmAxeShieldGoal(Zombie zombie) {
        this.zombie = zombie;
        setFlags(EnumSet.of(Flag.TARGET));
    }

    public static boolean isAxeZombie(Zombie zombie) {
        return zombie.isAlive() && zombie.level().dimension().equals(Schizm.KEY)
                && zombie.getMainHandItem().is(ItemTags.AXES);
    }

    @Override public boolean canUse() { return isAxeZombie(zombie); }
    @Override public boolean canContinueToUse() { return canUse(); }
    @Override public boolean requiresUpdateEveryTick() { return true; }

    private boolean valid(Player player) {
        return player != null && player.level() == zombie.level() && player.isAlive()
                && !player.isCreative() && !player.isSpectator() && zombie.canAttack(player)
                && zombie.distanceToSqr(player) <= TARGET_RADIUS * TARGET_RADIUS
                && zombie.getSensing().hasLineOfSight(player);
    }

    private static boolean holdsShield(Player player) {
        return player.getMainHandItem().has(DataComponents.BLOCKS_ATTACKS)
                || player.getOffhandItem().has(DataComponents.BLOCKS_ATTACKS);
    }

    /** Called once per actual shield raise, never refreshed just by holding it. */
    public void rush(Player player) {
        if (!isAxeZombie(zombie) || !valid(player)) return;
        rushTarget = player;
        rushUntil = zombie.level().getGameTime() + RUSH_TICKS;
        preferred = player;
        zombie.setTarget(player);
        // Effect amplifier 1 is Speed II. 40 server ticks = two seconds.
        zombie.addEffect(new MobEffectInstance(MobEffects.SPEED, RUSH_TICKS, 1, false, true));
        zombie.getNavigation().moveTo(player, 1.0D);
    }

    @Override public void tick() {
        long now = zombie.level().getGameTime();
        if (now < rushUntil && valid(rushTarget)) {
            preferred = rushTarget;
        } else {
            rushTarget = null;
            if (preferred != null && (!valid(preferred) || !holdsShield(preferred))) preferred = null;
            if (now >= nextScan) {
                nextScan = now + 10;
                preferred = null;
                double best = Double.MAX_VALUE;
                for (Player player : zombie.level().players()) {
                    if (!holdsShield(player) || !valid(player)) continue;
                    // Active blockers take precedence over merely held shields.
                    double score = zombie.distanceToSqr(player)
                            + (player.isBlocking() ? 0 : TARGET_RADIUS * TARGET_RADIUS + 1);
                    if (score < best) {
                        best = score;
                        preferred = player;
                    }
                }
            }
        }
        // The normal target selector runs first. Reassert our preference here
        // without replacing its fallback targets when no shield user is visible.
        if (preferred != null && zombie.getTarget() != preferred) zombie.setTarget(preferred);
    }

    @Override public void stop() {
        preferred = null;
        rushTarget = null;
        rushUntil = 0;
        nextScan = 0;
    }
}
