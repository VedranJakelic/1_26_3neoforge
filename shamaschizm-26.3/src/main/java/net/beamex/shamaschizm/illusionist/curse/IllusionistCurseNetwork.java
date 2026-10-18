package net.beamex.shamaschizm.illusionist.curse;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid="shamaschizm")
public final class IllusionistCurseNetwork {
    // Only the client packet receiver writes this; common code never loads client classes.
    public static final java.util.Queue<State> PENDING = new java.util.concurrent.ConcurrentLinkedQueue<>();
    public record State(int remaining, long seed) implements CustomPacketPayload {
        public static final Type<State> TYPE = new Type<>(Identifier.fromNamespaceAndPath("shamaschizm", "illusionist_curse"));
        public static final StreamCodec<RegistryFriendlyByteBuf,State> CODEC = new StreamCodec<>() {
            public State decode(RegistryFriendlyByteBuf b) { return new State(b.readVarInt(), b.readLong()); }
            public void encode(RegistryFriendlyByteBuf b, State s) { b.writeVarInt(s.remaining); b.writeLong(s.seed); }
        };
        public Type<State> type() { return TYPE; }
    }
    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(State.TYPE, State.CODEC, (packet,context) -> PENDING.add(packet));
    }
}
