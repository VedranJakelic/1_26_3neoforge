package net.beamex.shamaschizm.loading;

import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid=Shamaschizm.MOD_ID)
public final class DungeonLoadingNetwork {
    public static final ConcurrentLinkedQueue<State> PENDING = new ConcurrentLinkedQueue<>();
    // 0: enter, 1: generation finished, 2: leave. Tokens reject delayed acknowledgements.
    public record State(UUID player, UUID token, int phase, String dimension, BlockPos position) implements CustomPacketPayload {
        public static final Type<State> TYPE = new Type<>(Shamaschizm.id("dungeon_loading"));
        public static final StreamCodec<RegistryFriendlyByteBuf,State> CODEC = new StreamCodec<>() {
            public State decode(RegistryFriendlyByteBuf b) { return new State(b.readUUID(),b.readUUID(),b.readVarInt(),b.readUtf(128),b.readBlockPos()); }
            public void encode(RegistryFriendlyByteBuf b,State s) { b.writeUUID(s.player);b.writeUUID(s.token);b.writeVarInt(s.phase);b.writeUtf(s.dimension,128);b.writeBlockPos(s.position); }
        };
        public Type<State> type(){return TYPE;}
    }
    // 1: first frame rendered, 2: destination received, 3: unable to render.
    public record Ack(UUID token,int phase) implements CustomPacketPayload {
        public static final Type<Ack> TYPE = new Type<>(Shamaschizm.id("dungeon_loading_ack"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Ack> CODEC = new StreamCodec<>() {
            public Ack decode(RegistryFriendlyByteBuf b){return new Ack(b.readUUID(),b.readVarInt());}
            public void encode(RegistryFriendlyByteBuf b,Ack a){b.writeUUID(a.token);b.writeVarInt(a.phase);}
        };
        public Type<Ack> type(){return TYPE;}
    }
    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent e){
        var r=e.registrar("1");
        r.playToClient(State.TYPE,State.CODEC,(p,c)->c.enqueueWork(()->PENDING.add(p)));
        r.playToServer(Ack.TYPE,Ack.CODEC,(p,c)->c.enqueueWork(()->DungeonLoading.ack((ServerPlayer)c.player(),p)));
    }
}
