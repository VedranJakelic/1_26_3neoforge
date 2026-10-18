package net.beamex.shamaschizm.world.ai;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.beamex.shamaschizm.world.Schizm;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Short-lived, server-only memories of sounds made by players in the Schizm. */
public final class SchizmNoiseTracker {
    private static final Map<ServerLevel, Map<UUID, Noise>> NOISES = new WeakHashMap<>();

    private SchizmNoiseTracker() {}

    public static void playerNoise(ServerLevel level, Player player, double radius, int lifetimeTicks) {
        if (!level.dimension().equals(Schizm.KEY) || player.isCreative() || player.isSpectator()) return;
        put(level, player.getUUID(), player.position(), radius, lifetimeTicks);
    }

    public static void gateNoise(ServerLevel level, Vec3 position) {
        if (!level.dimension().equals(Schizm.KEY)) return;
        Player player = level.players().stream()
                .filter(candidate -> candidate.isAlive() && !candidate.isCreative() && !candidate.isSpectator())
                .filter(candidate -> candidate.distanceToSqr(position) <= 16.0D * 16.0D)
                .min((a, b) -> Double.compare(a.distanceToSqr(position), b.distanceToSqr(position)))
                .orElse(null);
        // A gate is still investigable even if its opener has already moved away.
        UUID source = player != null ? player.getUUID() : UUID.randomUUID();
        put(level, source, position, 60.0D, 400);
    }

    private static void put(ServerLevel level, UUID source, Vec3 position, double radius, int lifetimeTicks) {
        synchronized (NOISES) {
            long now = level.getGameTime();
            Map<UUID, Noise> levelNoises = NOISES.computeIfAbsent(level, ignored -> new HashMap<>());
            Noise previous = levelNoises.get(source);
            // Footsteps made immediately after opening a gate update the player's
            // position without replacing the stronger 60-block gate alert.
            double retainedRadius = previous != null && previous.expiresAt() > now
                    ? Math.max(radius, previous.radius()) : radius;
            long retainedExpiry = previous != null && previous.expiresAt() > now
                    ? Math.max(now + lifetimeTicks, previous.expiresAt()) : now + lifetimeTicks;
            levelNoises.put(source, new Noise(source, position, retainedRadius, retainedExpiry));
        }
    }

    public static @Nullable Noise loudestAudible(Zombie zombie) {
        if (!(zombie.level() instanceof ServerLevel level) || !level.dimension().equals(Schizm.KEY)) return null;
        long now = level.getGameTime();
        Noise best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        synchronized (NOISES) {
            Map<UUID, Noise> levelNoises = NOISES.get(level);
            if (levelNoises == null) return null;
            levelNoises.values().removeIf(noise -> noise.expiresAt() <= now);
            for (Noise noise : levelNoises.values()) {
                double distanceSquared = zombie.distanceToSqr(noise.position());
                if (distanceSquared > noise.radius() * noise.radius()) continue;
                // Prefer greater noise radius, then nearer sources of equal strength.
                double score = noise.radius() * noise.radius() - distanceSquared;
                if (score > bestScore) {
                    bestScore = score;
                    best = noise;
                }
            }
        }
        return best;
    }

    public record Noise(UUID source, Vec3 position, double radius, long expiresAt) {
        public @Nullable Player player(ServerLevel level) {
            return level.players().stream().filter(candidate -> candidate.getUUID().equals(source)).findFirst().orElse(null);
        }
    }
}
