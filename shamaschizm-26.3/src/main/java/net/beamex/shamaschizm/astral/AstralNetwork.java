package net.beamex.shamaschizm.astral;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
@EventBusSubscriber(modid=Shamaschizm.MOD_ID)
public final class AstralNetwork {
 public record Input(long session,float forward,float strafe,float vertical,float yaw,float pitch,boolean fast) implements CustomPacketPayload {
  public static final Type<Input> TYPE=new Type<>(Shamaschizm.id("astral_input"));
  public static final StreamCodec<RegistryFriendlyByteBuf,Input> CODEC=new StreamCodec<>(){
   public Input decode(RegistryFriendlyByteBuf b){return new Input(b.readLong(),b.readFloat(),b.readFloat(),b.readFloat(),b.readFloat(),b.readFloat(),b.readBoolean());}
   public void encode(RegistryFriendlyByteBuf b,Input p){b.writeLong(p.session);b.writeFloat(p.forward);b.writeFloat(p.strafe);b.writeFloat(p.vertical);b.writeFloat(p.yaw);b.writeFloat(p.pitch);b.writeBoolean(p.fast);}
  };
  public Type<Input> type(){return TYPE;}
 }
 public record State(long session,boolean active,double x,double y,double z,float yaw,float pitch,int age,float danger,boolean crossed) implements CustomPacketPayload {
  public static final Type<State> TYPE=new Type<>(Shamaschizm.id("astral_state"));
  public static final StreamCodec<RegistryFriendlyByteBuf,State> CODEC=new StreamCodec<>(){
   public State decode(RegistryFriendlyByteBuf b){return new State(b.readLong(),b.readBoolean(),b.readDouble(),b.readDouble(),b.readDouble(),b.readFloat(),b.readFloat(),b.readInt(),b.readFloat(),b.readBoolean());}
   public void encode(RegistryFriendlyByteBuf b,State p){b.writeLong(p.session);b.writeBoolean(p.active);b.writeDouble(p.x);b.writeDouble(p.y);b.writeDouble(p.z);b.writeFloat(p.yaw);b.writeFloat(p.pitch);b.writeInt(p.age);b.writeFloat(p.danger);b.writeBoolean(p.crossed);}
  };
  public Type<State> type(){return TYPE;}
 }
 @SubscribeEvent public static void register(RegisterPayloadHandlersEvent e){
  var r=e.registrar("1");r.playToClient(State.TYPE,State.CODEC);
  r.playToServer(Input.TYPE,Input.CODEC,(p,c)->c.enqueueWork(()->{if(c.player() instanceof ServerPlayer player)AstralProjection.input(player,p);}));
 }
}
