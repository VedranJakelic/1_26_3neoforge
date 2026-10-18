package net.beamex.shamaschizm.effect;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.world.Schizm;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Pending onset stays with the saved player; a new mushroom restarts its delays. */
@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class MushroomOnset {
    private static final String NAUSEA = "shamaschizm_mushroom_nausea_delay";
    private static final String TRIP = "shamaschizm_mushroom_trip_delay";
    private MushroomOnset() {}
    public static void schedule(ServerPlayer player) {
        player.getPersistentData().putInt(NAUSEA, 200);
        player.getPersistentData().putInt(TRIP, 300);
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!player.isAlive()) {
            player.getPersistentData().remove(NAUSEA);
            player.getPersistentData().remove(TRIP);
            return;
        }
        if (ready(player, NAUSEA)) player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 300, 0));
        if (ready(player, TRIP)) player.addEffect(new MobEffectInstance(ModEffects.TRIPPING,
                player.level().dimension().equals(Schizm.KEY) ? 2400 : 1200, 0));
    }
    private static boolean ready(ServerPlayer player, String key) {
        int ticks = player.getPersistentData().getInt(key).orElse(0);
        if (ticks <= 0) return false;
        if (ticks == 1) { player.getPersistentData().remove(key); return true; }
        player.getPersistentData().putInt(key, ticks - 1);
        return false;
    }
}
