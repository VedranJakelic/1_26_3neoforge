package net.beamex.shamaschizm.stew;

import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Synchronizes only the local animation clock; never creates inventory-visible effects. */
@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class PoppyStewNetwork {
    private PoppyStewNetwork() {}

    public record State(int blurTicks, int eyelidTicks) implements CustomPacketPayload {
        public static final Type<State> TYPE = new Type<>(Shamaschizm.id("poppy_stew_visuals"));
        public static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC = new StreamCodec<>() {
            @Override public State decode(RegistryFriendlyByteBuf buf) {
                return new State(buf.readVarInt(), buf.readVarInt());
            }
            @Override public void encode(RegistryFriendlyByteBuf buf, State packet) {
                buf.writeVarInt(packet.blurTicks());
                buf.writeVarInt(packet.eyelidTicks());
            }
        };
        @Override public Type<State> type() { return TYPE; }
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        // The actual callback lives in the client-only PoppyStewVisuals class.
        event.registrar("1").playToClient(State.TYPE, State.CODEC);
    }

    public static void send(ServerPlayer player, int blurTicks, int eyelidTicks) {
        PacketDistributor.sendToPlayer(player,
                new State(Math.max(0, blurTicks), Math.max(0, eyelidTicks)));
    }
}
