package net.beamex.shamaschizm.world;

import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Server weather controller and the tiny per-player proximity sync packet. */
@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class PortalAtmosphere {
    private static final int SYNC_INTERVAL = 20;
    private static final int RAIN_ROLL_INTERVAL = 20 * 60;
    private static final float MAX_RAIN_CHANCE = 0.15F;
    // Mirrors vanilla ServerLevel.THUNDER_DELAY, which is private in 26.2.
    private static final int MIN_THUNDER_DELAY = 12_000;
    private static final int THUNDER_DELAY_SPREAD = 168_001;
    private static long lastRainRoll = Long.MIN_VALUE;

    /** Written only by the client payload handler. */
    public static volatile float clientTargetProximity;

    private PortalAtmosphere() {}

    public record Update(float proximity) implements CustomPacketPayload {
        public static final Type<Update> TYPE = new Type<>(Shamaschizm.id("portal_atmosphere"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Update> CODEC = new StreamCodec<>() {
            @Override public Update decode(RegistryFriendlyByteBuf buffer) {
                return new Update(buffer.readFloat());
            }
            @Override public void encode(RegistryFriendlyByteBuf buffer, Update update) {
                buffer.writeFloat(update.proximity());
            }
        };
        @Override public Type<Update> type() { return TYPE; }
    }

    @SubscribeEvent
    public static void registerPayload(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(Update.TYPE, Update.CODEC, (update, context) ->
                clientTargetProximity = Mth.clamp(update.proximity(), 0.0F, 1.0F));
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || player.tickCount % SYNC_INTERVAL != 0) return;

        ServerLevel level = player.level();
        if (level.dimension().equals(Schizm.KEY)) {
            clearSchizmWeather(level);
            PacketDistributor.sendToPlayer(player, new Update(0.0F));
            return;
        }
        if (!level.dimension().equals(Level.OVERWORLD)) {
            PacketDistributor.sendToPlayer(player, new Update(0.0F));
            return;
        }

        PortalAtmosphereData data = PortalAtmosphereData.get(level);
        ServerLevel schizm = level.getServer().getLevel(Schizm.KEY);
        if (schizm != null) data.addAll(PortalLinks.get(schizm).sourcePositions());

        float proximity = data.proximity(player.blockPosition());
        PacketDistributor.sendToPlayer(player, new Update(proximity));
        maybeStartRain(level, data);
    }

    @SubscribeEvent
    public static void chunkLoaded(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level
                && level.dimension().equals(Level.OVERWORLD)
                && event.getChunk() instanceof net.minecraft.world.level.chunk.LevelChunk chunk) {
            PortalAtmosphereData.get(level).indexChunk(chunk);
        }
    }

    private static void maybeStartRain(ServerLevel overworld, PortalAtmosphereData data) {
        long roll = overworld.getGameTime() / RAIN_ROLL_INTERVAL;
        if (roll == lastRainRoll) return;
        lastRainRoll = roll;

        float strongest = 0.0F;
        for (ServerPlayer player : overworld.players()) {
            if (!player.isSpectator()) {
                strongest = Math.max(strongest, data.proximity(player.blockPosition()));
            }
        }
        if (strongest <= 0.0F || overworld.isRaining()
                || overworld.getRandom().nextFloat() >= strongest * MAX_RAIN_CHANCE) return;

        var weather = overworld.getWeatherData();
        weather.setClearWeatherTime(0);
        weather.setRainTime(ServerLevel.RAIN_DURATION.sample(overworld.getRandom()));
        weather.setRaining(true);
        weather.setThunderTime(MIN_THUNDER_DELAY
                + overworld.getRandom().nextInt(THUNDER_DELAY_SPREAD));
        weather.setThundering(false);
    }

    private static void clearSchizmWeather(ServerLevel schizm) {
        var weather = schizm.getWeatherData();
        weather.setClearWeatherTime(6000);
        weather.setRainTime(0);
        weather.setRaining(false);
        weather.setThunderTime(0);
        weather.setThundering(false);
    }
}
