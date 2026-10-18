package net.beamex.shamaschizm.world.ai;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.beamex.shamaschizm.registry.ModBlocks;
import net.beamex.shamaschizm.world.Schizm;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.BaseTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;

/** Exclusive movement goal; no reflection, mixins, or changes outside Schizm. */
public final class SchizmZombieGoal extends Goal {
    public static final int LIGHT_RADIUS = 10;
    public static final int PLAYER_NEARBY_RADIUS = 8;
    public static final int LIGHT_SCAN_INTERVAL = 100;
    public static final int MIN_DIG_TICKS = 80;
    public static final double DIG_REACH = 2.6;
    private final Zombie mob;
    private BlockPos work;
    private BlockState original;
    private Player target;
    private Player rememberedPlayer;
    private int rememberUntil;
    private Path lightPath;
    private boolean digging;
    private boolean finished;
    private int progress;
    private int elapsed;
    private int duration;
    private int nextCheck;
    private int nextLightScan;
    private int blockedChecks;

    public SchizmZombieGoal(Zombie mob) {
        this.mob = mob;
        this.nextCheck = mob.tickCount + mob.getRandom().nextInt(20);
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private ServerLevel level() { return (ServerLevel) mob.level(); }
    private boolean enabled() {
        return mob.level() instanceof ServerLevel && mob.level().dimension().equals(Schizm.KEY)
                && mob.isAlive() && !mob.isNoAi() && !mob.isPassenger()
                && EventHooks.canEntityGrief(level(), mob);
    }
    private boolean nearbyPlayer() {
        return level().players().stream().anyMatch(p -> p.isAlive() && !p.isSpectator()
                && p.distanceToSqr(mob) <= PLAYER_NEARBY_RADIUS * PLAYER_NEARBY_RADIUS);
    }
    private boolean validTarget(Player player) {
        return player != null && player.isAlive() && !player.isCreative() && !player.isSpectator()
                && player.level() == mob.level() && player.distanceToSqr(mob) <= 32 * 32 && mob.canAttack(player);
    }
    private boolean blocked(Player player) {
        Path path = mob.getNavigation().createPath(player, 1);
        return path == null || !path.canReach();
    }
    private boolean light(BlockState state) {
        return state.getBlock() instanceof BaseTorchBlock || AbstractCandleBlock.isLit(state);
    }
    private boolean permitted(BlockPos pos, BlockState state) {
        return level().hasChunkAt(pos) && level().getWorldBorder().isWithinBounds(pos)
                && !state.is(ModBlocks.SCHIZM_PORTAL)
                && !state.hasBlockEntity() && state.getDestroySpeed(level(), pos) >= 0
                && state.canEntityDestroy(level(), pos, mob);
    }

    /** First physical obstacle towards the target, never a distant block through a wall. */
    private BlockPos obstacle(Player player) {
        for (double height : new double[]{0.35, mob.getEyeHeight()}) {
            Vec3 from = mob.position().add(0, height, 0);
            Vec3 delta = player.position().add(0, height, 0).subtract(from);
            if (delta.lengthSqr() < 0.01) continue;
            Vec3 to = from.add(delta.normalize().scale(Math.min(DIG_REACH, delta.length())));
            var hit = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mob));
            if (hit.getType() != HitResult.Type.BLOCK) continue;
            BlockPos p = hit.getBlockPos();
            // Do not excavate the block directly supporting the zombie.
            if (p.equals(mob.blockPosition().below())) continue;
            BlockState state = level().getBlockState(p);
            if (!(state.getBlock() instanceof AbstractCandleBlock) && permitted(p, state)) return p.immutable();
        }
        return null;
    }

    @Override
    public boolean canUse() {
        if (mob.tickCount < nextCheck) return false;
        nextCheck = mob.tickCount + 20;
        work = null;
        if (!enabled()) { blockedChecks = 0; rememberedPlayer = null; return false; }
        Player player = mob.getTarget() instanceof Player p ? p : null;
        if (validTarget(player)) {
            rememberedPlayer = player;
            rememberUntil = mob.tickCount + 1800;
        } else if (mob.getTarget() == null && mob.tickCount < rememberUntil && validTarget(rememberedPlayer)) {
            player = rememberedPlayer;
        }
        if (validTarget(player)) {
            if (!blocked(player)) {
                blockedChecks = 0;
                if (mob.getTarget() == null) { mob.setTarget(player); mob.getNavigation().moveTo(player, 1.0); }
                return false;
            }
            if (++blockedChecks < 2) return false;
            work = obstacle(player);
            if (work == null) return false; // Vanilla navigation can approach a partial path's endpoint.
            target = player;
            digging = true;
        } else {
            blockedChecks = 0;
            if (mob.getTarget() != null || nearbyPlayer() || mob.tickCount < nextLightScan) return false;
            nextLightScan = mob.tickCount + LIGHT_SCAN_INTERVAL + mob.getRandom().nextInt(20);
            List<BlockPos> candidates = new ArrayList<>();
            BlockPos center = mob.blockPosition();
            for (BlockPos p : BlockPos.betweenClosed(center.offset(-LIGHT_RADIUS, -LIGHT_RADIUS, -LIGHT_RADIUS),
                    center.offset(LIGHT_RADIUS, LIGHT_RADIUS, LIGHT_RADIUS))) {
                if (p.distSqr(center) > LIGHT_RADIUS * LIGHT_RADIUS || !level().hasChunkAt(p)) continue;
                BlockState state = level().getBlockState(p);
                if (light(state) && permitted(p, state)) candidates.add(p.immutable());
            }
            // Bound expensive path searches, and vary candidates so a blocked light cannot starve others.
            java.util.Collections.shuffle(candidates, new java.util.Random(mob.getRandom().nextLong()));
            for (int i = 0; i < Math.min(4, candidates.size()); i++) {
                BlockPos p = candidates.get(i);
                Path path = mob.getNavigation().createPath(p, 1);
                if (path != null && path.canReach()) { work = p; lightPath = path; break; }
            }
            if (work == null) return false;
            digging = false;
            target = null;
        }
        original = level().getBlockState(work);
        return true;
    }

    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public void start() {
        progress = 0;
        elapsed = 0;
        finished = false;
        duration = digging ? Math.min(1200, MIN_DIG_TICKS + (int)(40 * original.getDestroySpeed(level(), work))) : 20;
        if (digging) mob.getNavigation().stop();
        else mob.getNavigation().moveTo(lightPath, 1.0);
    }
    @Override public boolean canContinueToUse() {
        if (finished || work == null || !enabled() || !level().hasChunkAt(work)
                || !level().getBlockState(work).equals(original)) return false;
        if (digging) return (mob.getTarget() == target || mob.getTarget() == null) && validTarget(target)
                && mob.tickCount < rememberUntil
                && Vec3.atCenterOf(work).distanceToSqr(mob.getEyePosition()) <= 12;
        return elapsed < 300 && mob.getTarget() == null && !nearbyPlayer() && light(original);
    }
    private boolean visible() {
        var hit = level().clip(new ClipContext(mob.getEyePosition(), Vec3.atCenterOf(work),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mob));
        return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(work);
    }
    @Override public void tick() {
        elapsed++;
        mob.getLookControl().setLookAt(work.getX()+0.5, work.getY()+0.5, work.getZ()+0.5);
        if (digging && elapsed % 20 == 0 && !blocked(target)) { finished = true; return; }
        if (!digging && elapsed % 20 == 0) mob.getNavigation().moveTo(work.getX()+0.5, work.getY(), work.getZ()+0.5, 1.0);
        if (Vec3.atCenterOf(work).distanceToSqr(mob.getEyePosition()) > DIG_REACH * DIG_REACH || !visible()) {
            progress = 0;
            if (digging) { level().destroyBlockProgress(mob.getId(), work, -1); finished = true; }
            return;
        }
        if (progress++ % 20 == 0) mob.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT);
        // Candles are always extinguished, including when one was selected as an obstacle.
        if (digging && !AbstractCandleBlock.isLit(original))
            level().destroyBlockProgress(mob.getId(), work, Math.min(9, progress * 10 / duration));
        if (progress < duration) return;
        if (permitted(work, original) && EventHooks.onEntityDestroyBlock(mob, work, original)) {
            if (original.getBlock() instanceof AbstractCandleBlock) {
                if (AbstractCandleBlock.isLit(original)) AbstractCandleBlock.extinguish(null, original, level(), work);
            } else {
                level().destroyBlock(work, true, mob);
            }
        }
        finished = true;
    }
    @Override public void stop() {
        if (work != null && mob.level() instanceof ServerLevel server)
            server.destroyBlockProgress(mob.getId(), work, -1);
        mob.getNavigation().stop();
        work = null;
        target = null;
        lightPath = null;
        blockedChecks = 0;
    }
}
