package net.beamex.shamaschizm.garden;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.world.Schizm;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class GardenAudio {
    // Written by the client payload handler only. No client classes loaded on the server.
    public static float proximity;
    public static boolean inside;
    public record Update(float gain, boolean inside) implements CustomPacketPayload {
        public static final Type<Update> TYPE = new Type<>(Shamaschizm.id("garden_audio"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Update> CODEC = new StreamCodec<>() {
            public Update decode(RegistryFriendlyByteBuf b) { return new Update(b.readFloat(), b.readBoolean()); }
            public void encode(RegistryFriendlyByteBuf b, Update u) { b.writeFloat(u.gain()); b.writeBoolean(u.inside()); }
        };
        @Override public Type<Update> type() { return TYPE; }
    }
    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent e) {
        e.registrar("1").playToClient(Update.TYPE, Update.CODEC, (u, context) -> {
            proximity = Math.max(0, Math.min(1, u.gain())); inside = u.inside();
        });
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 2 != 0) return;
        double nearest = Double.POSITIVE_INFINITY;
        boolean within = false;
        if (p.level().dimension().equals(Schizm.KEY)) {
            for (var b : GardenData.get(p.level()).rooms) {
                double dx = Math.max(0, Math.max(b.minX() - p.getX(), p.getX() - (b.maxX() + 1.0)));
                double dy = Math.max(0, Math.max(b.minY() - p.getY(), p.getY() - (b.maxY() + 1.0)));
                double dz = Math.max(0, Math.max(b.minZ() - p.getZ(), p.getZ() - (b.maxZ() + 1.0)));
                nearest = Math.min(nearest, Math.sqrt(dx*dx + dy*dy + dz*dz));
                within |= p.getX() >= b.minX() && p.getX() < b.maxX()+1.0
                        && p.getY() >= b.minY() && p.getY() < b.maxY()+1.0
                        && p.getZ() >= b.minZ() && p.getZ() < b.maxZ()+1.0;
            }
        }
        PacketDistributor.sendToPlayer(p, new Update((float)Math.max(0, 1-nearest/18.0), within));
    }
}
