package net.beamex.shamaschizm.centipede;
import net.beamex.shamaschizm.Shamaschizm;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
/** Client notification state without dedicated-server references to client classes. */
@EventBusSubscriber(modid=Shamaschizm.MOD_ID)
public final class BiteVisuals {
 public static long receivedNanos;
 public static int playerId=-1;
 public record Bite(int playerId) implements CustomPacketPayload {
  public static final Type<Bite> TYPE=new Type<>(Shamaschizm.id("centipede_bite"));
  public static final StreamCodec<RegistryFriendlyByteBuf,Bite> CODEC=new StreamCodec<>(){
   public Bite decode(RegistryFriendlyByteBuf b){return new Bite(b.readInt());}
   public void encode(RegistryFriendlyByteBuf b,Bite p){b.writeInt(p.playerId());}
  };
  @Override public Type<Bite> type(){return TYPE;}
 }
 @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event){event.registrar("1").playToClient(Bite.TYPE,Bite.CODEC,(p,c)->{
  playerId=p.playerId();receivedNanos=System.nanoTime();
 });}
}
