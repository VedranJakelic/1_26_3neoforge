package net.beamex.shamaschizm.wisdom.giant;
import java.util.*;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
@EventBusSubscriber(modid=Shamaschizm.MOD_ID)
public final class GiantWisdomNetwork {
    public record Quake(float strength,int crouch,boolean warning) implements CustomPacketPayload {
        public static final Type<Quake> TYPE=new Type<>(Shamaschizm.id("giant_wisdom_quake"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Quake> CODEC=new StreamCodec<>(){
            public Quake decode(RegistryFriendlyByteBuf b){return new Quake(b.readFloat(),b.readVarInt(),b.readBoolean());}
            public void encode(RegistryFriendlyByteBuf b,Quake p){b.writeFloat(p.strength);b.writeVarInt(p.crouch);b.writeBoolean(p.warning);}
        };
        public Type<Quake> type(){return TYPE;}
    }
    private static final Map<ServerPlayer,Long> CROUCH=new WeakHashMap<>();
    public static void send(ServerPlayer p,float strength,int ticks,boolean warning){PacketDistributor.sendToPlayer(p,new Quake(strength,ticks,warning));}
    public static void crouch(ServerPlayer p,int ticks){CROUCH.put(p,p.level().getGameTime()+ticks);}
    @SubscribeEvent public static void tick(PlayerTickEvent.Post e){
        if(!(e.getEntity() instanceof ServerPlayer p))return;
        Long until=CROUCH.get(p);if(until==null)return;
        if(!p.isAlive()||p.isSpectator()||p.level().getGameTime()>=until){CROUCH.remove(p);return;}
        if(!p.isSleeping()&&!p.isPassenger()&&!p.isSwimming()&&!p.isFallFlying())p.setPose(Pose.CROUCHING);
    }
    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent e){e.registrar("1").playToClient(Quake.TYPE,Quake.CODEC);}
}
