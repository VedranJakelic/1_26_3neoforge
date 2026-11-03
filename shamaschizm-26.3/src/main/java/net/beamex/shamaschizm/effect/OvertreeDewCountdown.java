package net.beamex.shamaschizm.effect;

import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Countdown is server-authoritative and remains saved with the player across logouts. */
@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class OvertreeDewCountdown {
    private static final String TIME_KEY = "shamaschizm_dew_countdown";
    private static final String OWN_GLOW_KEY = "shamaschizm_dew_owns_glow";

    // Alternating on/off intervals, getting shorter near the end.
    // The first 20 ticks are continuously on. Sum = 150 ticks (7.5 seconds).
    private static final int[] BLINK_INTERVALS = {
            20, 15, 15, 12, 12, 10, 10, 8, 8, 6, 6, 5, 5, 4, 4, 3, 3, 2, 2
    };
    private static final int BLINK_TICKS = 150;
    private static final int PAUSE_TICKS = 20;
    private static final int TOTAL_TICKS = BLINK_TICKS + PAUSE_TICKS;
    private static final int POISON_TICKS = 40;
    private static final double POISON_RADIUS_SQUARED = 4.0 * 4.0;

    private OvertreeDewCountdown() {}

    /** A new successful roll restarts a pending countdown. */
    public static void arm(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        releaseOwnedGlow(player, data);
        data.putInt(TIME_KEY, TOTAL_TICKS);
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        CompoundTag data = player.getPersistentData();
        int remaining = data.getInt(TIME_KEY).orElse(0);
        if (remaining <= 0) return;
        if (!player.isAlive()) {
            releaseOwnedGlow(player, data);
            data.remove(TIME_KEY);
            return;
        }

        int elapsed = TOTAL_TICKS - remaining;
        boolean glow = elapsed < BLINK_TICKS && shouldGlow(elapsed);
        updateGlow(player, data, glow);

        if (remaining == 1) {
            releaseOwnedGlow(player, data);
            data.remove(TIME_KEY);
            burst(player);
        } else {
            data.putInt(TIME_KEY, remaining - 1);
        }
    }

    private static boolean shouldGlow(int elapsed) {
        for (int phase = 0; phase < BLINK_INTERVALS.length; phase++) {
            if (elapsed < BLINK_INTERVALS[phase]) return (phase & 1) == 0;
            elapsed -= BLINK_INTERVALS[phase];
        }
        return false;
    }

    private static void updateGlow(ServerPlayer player, CompoundTag data, boolean glow) {
        if (!glow) {
            releaseOwnedGlow(player, data);
            return;
        }

        boolean owned = data.getBoolean(OWN_GLOW_KEY).orElse(false);
        MobEffectInstance current = player.getEffect(MobEffects.GLOWING);
        if (!owned && current != null) {
            // Do not remove a potion/command's existing Glowing effect in an off-phase.
            return;
        }
        if (!owned) data.putBoolean(OWN_GLOW_KEY, true);
        if (current == null || current.getDuration() < 3) {
            player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 4, 0, true, false, false));
        }
    }

    private static void releaseOwnedGlow(ServerPlayer player, CompoundTag data) {
        if (data.getBoolean(OWN_GLOW_KEY).orElse(false)) {
            player.removeEffect(MobEffects.GLOWING);
            data.remove(OWN_GLOW_KEY);
        }
    }

    private static void burst(ServerPlayer source) {
        if (!(source.level() instanceof ServerLevel level)) return;
        for (int i = 0; i < 8; i++) {
            double x = source.getX() + (level.getRandom().nextDouble() - 0.5) * 0.45;
            double y = source.getY() + 0.55 + level.getRandom().nextDouble() * 1.0;
            double z = source.getZ() + (level.getRandom().nextDouble() - 0.5) * 0.45;
            double vx = level.getRandom().nextGaussian() * 0.055;
            double vy = 0.015 + level.getRandom().nextDouble() * 0.045;
            double vz = level.getRandom().nextGaussian() * 0.055;
            // Same NOXIOUS_GAS particles and individual-velocity mode as PoisonTrapBlock.
            level.sendParticles(ParticleTypes.NOXIOUS_GAS, x, y, z, 0, vx, vy, vz, 1.0);
        }

        for (ServerPlayer target : level.players()) {
            if (!target.isAlive() || target.isSpectator()) continue;
            if (target.distanceToSqr(source) > POISON_RADIUS_SQUARED) continue;
            target.addEffect(new MobEffectInstance(MobEffects.POISON,
                    POISON_TICKS, 0, false, true, true));
        }
    }
}
