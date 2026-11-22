package net.beamex.shamaschizm.stew;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.effect.ModEffects;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.clock.ClockTimeMarkers;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.WeakHashMap;

/** The stew has real poison/tripping effects; blur and eyelids are visuals only. */
@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class PoppyStewEvents {
    private static final String POPPIES = "shamaschizm_poppy_stew";
    private static final String POISON_LEFT = "shamaschizm_stew_poison_left";
    private static final String PENDING_TRIP = "shamaschizm_stew_pending_trip";
    private static final String TRIP_LEFT = "shamaschizm_stew_trip_left";
    private static final String SKIP_LEFT = "shamaschizm_stew_skip_left";

    private static final int POISON_TICKS = 100;       // 5 seconds
    private static final int TICKS_PER_POPPY = 300;    // 15 seconds
    private static final int CLOSE_TICKS = 60;         // final 3 seconds of tripping
    private static final int BLACKOUT_TICKS = 10;      // half-second fully closed
    private static final int OPEN_TICKS = 20;          // 1 second reopening

    private static final Map<MinecraftServer, Long> LAST_SKIP_TICK = new WeakHashMap<>();

    private PoppyStewEvents() {}

    @SubscribeEvent
    public static void onFinishUsing(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!event.getItem().is(Items.SUSPICIOUS_STEW)) return;

        CustomData customData = event.getItem().get(DataComponents.CUSTOM_DATA);
        if (customData == null || !customData.contains(POPPIES)) return;
        int poppies = customData.copyTag().getInt(POPPIES).orElse(0);
        if (poppies < 1 || poppies > 4) return;

        var data = player.getPersistentData();
        data.putInt(POISON_LEFT, POISON_TICKS);
        data.putInt(PENDING_TRIP, Math.min(1200, TICKS_PER_POPPY * poppies));
        data.remove(TRIP_LEFT);
        data.remove(SKIP_LEFT);
        // Start the dedicated motion blur with Poison, not with Tripping.
        PoppyStewNetwork.send(player, POISON_TICKS, 0);
        Shamaschizm.LOGGER.info("Poppy stew consumed: {} poppies, {} trip ticks, player={}",
                poppies, TICKS_PER_POPPY * poppies, player.getName().getString());
        player.addEffect(new MobEffectInstance(MobEffects.POISON, POISON_TICKS, 0));
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var data = player.getPersistentData();
        if (!player.isAlive()) {
            if (data.getInt(POISON_LEFT).orElse(0) > 0
                    || data.getInt(TRIP_LEFT).orElse(0) > 0
                    || data.getInt(SKIP_LEFT).orElse(0) > 0)
                PoppyStewNetwork.send(player, 0, 0);
            data.remove(POISON_LEFT);
            data.remove(PENDING_TRIP);
            data.remove(TRIP_LEFT);
            data.remove(SKIP_LEFT);
            return;
        }

        int poisonLeft = data.getInt(POISON_LEFT).orElse(0);
        if (poisonLeft > 0) {
            // Keep the poison-stage blur synchronized across client lag and reconnects.
            if (poisonLeft % 20 == 0)
                PoppyStewNetwork.send(player, poisonLeft, 0);
            if (poisonLeft == 1) {
                data.remove(POISON_LEFT);
                int duration = data.getInt(PENDING_TRIP).orElse(0);
                data.remove(PENDING_TRIP);
                if (duration > 0) startTrip(player, duration);
            } else {
                data.putInt(POISON_LEFT, poisonLeft - 1);
            }
            return;
        }

        int tripLeft = data.getInt(TRIP_LEFT).orElse(0);
        if (tripLeft > 0) {
            // Include the remaining blackout/reopening time during the last 3s.
            int lidLeft = tripLeft <= CLOSE_TICKS ? tripLeft + BLACKOUT_TICKS + OPEN_TICKS : 0;
            if (tripLeft == CLOSE_TICKS || tripLeft % 20 == 0)
                PoppyStewNetwork.send(player, 0, lidLeft);
            if (tripLeft == 1) {
                data.remove(TRIP_LEFT);
                data.putInt(SKIP_LEFT, BLACKOUT_TICKS);
                PoppyStewNetwork.send(player, 0, BLACKOUT_TICKS + OPEN_TICKS);
            } else {
                data.putInt(TRIP_LEFT, tripLeft - 1);
            }
            return;
        }

        int skipLeft = data.getInt(SKIP_LEFT).orElse(0);
        if (skipLeft > 0) {
            if (skipLeft == BLACKOUT_TICKS)
                PoppyStewNetwork.send(player, 0, BLACKOUT_TICKS + OPEN_TICKS);
            if (skipLeft == 1) {
                data.remove(SKIP_LEFT);
                advanceSharedDayNightClock(player);
                Shamaschizm.LOGGER.info("Poppy stew reached time-skip stage: player={}",
                        player.getName().getString());
                PoppyStewNetwork.send(player, 0, OPEN_TICKS);
            } else {
                data.putInt(SKIP_LEFT, skipLeft - 1);
            }
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var data = player.getPersistentData();
        int tripLeft = data.getInt(TRIP_LEFT).orElse(0);
        int skipLeft = data.getInt(SKIP_LEFT).orElse(0);
        int lidLeft = skipLeft > 0 ? skipLeft + OPEN_TICKS
                : tripLeft > 0 && tripLeft <= CLOSE_TICKS
                    ? tripLeft + BLACKOUT_TICKS + OPEN_TICKS : 0;
        int poisonLeft = data.getInt(POISON_LEFT).orElse(0);
        PoppyStewNetwork.send(player, poisonLeft, lidLeft);
    }

    private static void startTrip(ServerPlayer player, int duration) {
        duration = Math.max(TICKS_PER_POPPY, Math.min(1200, duration));
        player.getPersistentData().putInt(TRIP_LEFT, duration);
        MobEffectInstance existing = player.getEffect(ModEffects.TRIPPING);
        // Preserve a stronger or longer unrelated mushroom trip.
        if (existing == null || (!existing.isInfiniteDuration() && existing.getDuration() < duration)) {
            int amplifier = existing == null ? 0 : existing.getAmplifier();
            player.addEffect(new MobEffectInstance(ModEffects.TRIPPING, duration, amplifier));
        }
        // Poison has ended: stop its motion blur; retain normal tripping visuals.
        PoppyStewNetwork.send(player, 0, 0);
        Shamaschizm.LOGGER.info("Poppy stew trip started: player={}, duration={} ticks",
                player.getName().getString(), duration);
    }

    private static void advanceSharedDayNightClock(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        long gameTick = server.overworld().getGameTime();
        synchronized (LAST_SKIP_TICK) {
            if (Long.valueOf(gameTick).equals(LAST_SKIP_TICK.get(server))) return;
            LAST_SKIP_TICK.put(server, gameTick);
        }
        var overworld = server.overworld();
        var clock = overworld.dimensionType().defaultClock().orElse(null);
        if (clock == null) return;
        long phase = Math.floorMod(overworld.getOverworldClockTime(), 24000L);
        server.clockManager().moveToTimeMarker(clock,
                phase < 12000L ? ClockTimeMarkers.NIGHT : ClockTimeMarkers.DAY);
    }
}
