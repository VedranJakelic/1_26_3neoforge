package net.beamex.shamaschizm.localportal;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Metadata only. Destination terrain streaming is a separate rendering milestone. */
@EventBusSubscriber(modid = "shamaschizm")
public final class DoorwayNetwork {
    public static final AtomicReference<List<Doorway>> PENDING = new AtomicReference<>();
    public record State(List<Doorway> doorways) implements CustomPacketPayload {
        public State { doorways = List.copyOf(doorways); }
        public static final Type<State> TYPE = new Type<>(Shamaschizm.id("local_doorways"));
        public static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC = new StreamCodec<>() {
            public State decode(RegistryFriendlyByteBuf b) {
                int count = b.readVarInt();
                if (count < 0 || count > Doorway.MAX_DOORWAYS) throw new IllegalArgumentException("Invalid doorway count");
                List<Doorway> result = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    String name = b.readUtf(48);
                    Vec3 origin = new Vec3(b.readDouble(), b.readDouble(), b.readDouble());
                    Doorway.Facing facing = b.readEnum(Doorway.Facing.class);
                    double width = b.readDouble(), height = b.readDouble();
                    result.add(new Doorway(name, origin, facing, width, height, b.readUtf(48), b.readBoolean()));
                }
                return new State(result);
            }
            public void encode(RegistryFriendlyByteBuf b, State packet) {
                b.writeVarInt(packet.doorways.size());
                for (Doorway d : packet.doorways) {
                    b.writeUtf(d.name(), 48);
                    b.writeDouble(d.origin().x); b.writeDouble(d.origin().y); b.writeDouble(d.origin().z);
                    b.writeEnum(d.facing()); b.writeDouble(d.width()); b.writeDouble(d.height());
                    b.writeUtf(d.target(), 48); b.writeBoolean(d.enabled());
                }
            }
        };
        public Type<State> type() { return TYPE; }
    }
    /** Sent immediately after the vanilla position/rotation teleport packet. */
    public record Turn(float yawDelta) implements CustomPacketPayload {
        public static final Type<Turn> TYPE = new Type<>(Shamaschizm.id("local_doorway_turn"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Turn> CODEC = new StreamCodec<>() {
            public Turn decode(RegistryFriendlyByteBuf b) { return new Turn(b.readFloat()); }
            public void encode(RegistryFriendlyByteBuf b, Turn packet) { b.writeFloat(packet.yawDelta()); }
        };
        public Type<Turn> type() { return TYPE; }
    }
    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(PortalInteractionPacket.TYPE, PortalInteractionPacket.CODEC,
                (packet, context) -> context.enqueueWork(() -> PortalInteractions.handle((net.minecraft.server.level.ServerPlayer)context.player(),packet)));
        event.registrar("1").playToClient(PortalSound.TYPE, PortalSound.CODEC);
        event.registrar("1").playToClient(PortalTerrain.TYPE, PortalTerrain.CODEC);
        event.registrar("1").playToClient(PortalEntities.TYPE, PortalEntities.CODEC);
        event.registrar("1").playToClient(Turn.TYPE, Turn.CODEC);
        event.registrar("1").playToClient(State.TYPE, State.CODEC,
                (packet, context) -> PENDING.set(packet.doorways()));
    }
}
