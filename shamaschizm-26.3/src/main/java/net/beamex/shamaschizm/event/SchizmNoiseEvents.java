package net.beamex.shamaschizm.event;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.world.Schizm;
import net.beamex.shamaschizm.world.ai.SchizmInvestigateNoiseGoal;
import net.beamex.shamaschizm.world.ai.SchizmNoiseTracker;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

/** Converts real player/gate sounds into temporary investigation targets for Schizm zombies. */
@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class SchizmNoiseEvents {
    private SchizmNoiseEvents() {}

    @SubscribeEvent
    public static void addNoiseGoal(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !level.dimension().equals(Schizm.KEY)
                || !(event.getEntity() instanceof Zombie zombie)) return;
        boolean present = zombie.getGoalSelector().getAvailableGoals().stream()
                .anyMatch(wrapped -> wrapped.getGoal() instanceof SchizmInvestigateNoiseGoal);
        if (!present) zombie.getGoalSelector().addGoal(3, new SchizmInvestigateNoiseGoal(zombie));
    }

    @SubscribeEvent
    public static void soundAtEntity(PlayLevelSoundEvent.AtEntity event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !level.dimension().equals(Schizm.KEY)) return;
        if (isGateSound(event.getSound())) {
            SchizmNoiseTracker.gateNoise(level, event.getEntity().position());
            return;
        }
        if (!(event.getEntity() instanceof Player player) || event.getOriginalVolume() <= 0.05F) return;
        // Sneaking suppresses movement noise only. Other actions performed while
        // crouching (breaking blocks, using items, taking damage, etc.) remain audible.
        if (player.isCrouching() && isStepSound(event.getSound())) return;
        double strength = Mth.clamp(event.getOriginalVolume(), 0.0F, 1.0F);
        double radius = 15.0D + 15.0D * strength;
        int lifetime = 100 + (int)(140.0D * strength);
        SchizmNoiseTracker.playerNoise(level, player, radius, lifetime);
    }

    @SubscribeEvent
    public static void soundAtPosition(PlayLevelSoundEvent.AtPosition event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension().equals(Schizm.KEY)
                && isGateSound(event.getSound())) {
            SchizmNoiseTracker.gateNoise(level, event.getPosition());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void blockBroken(BreakBlockEvent event) {
        if (!event.isCanceled() && event.getLevel() instanceof ServerLevel level
                && level.dimension().equals(Schizm.KEY)) {
            SchizmNoiseTracker.playerNoise(level, event.getPlayer(), 30.0D, 240);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void blockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!event.isCanceled() && event.getLevel() instanceof ServerLevel level
                && level.dimension().equals(Schizm.KEY) && event.getEntity() instanceof Player player) {
            SchizmNoiseTracker.playerNoise(level, player, 25.0D, 180);
        }
    }

    private static boolean isGateSound(Holder<SoundEvent> sound) {
        Identifier id = soundId(sound);
        if (id == null) return false;
        return (id.getNamespace().equals("gate") && id.getPath().equals("gate1"))
                || (id.getNamespace().equals(Shamaschizm.MOD_ID) && id.getPath().equals("meggate"));
    }

    private static boolean isStepSound(Holder<SoundEvent> sound) {
        Identifier id = soundId(sound);
        return id != null && (id.getPath().endsWith(".step") || id.getPath().contains(".step."));
    }

    private static Identifier soundId(Holder<SoundEvent> sound) {
        return sound == null ? null : sound.unwrapKey().map(key -> key.identifier()).orElse(null);
    }
}
