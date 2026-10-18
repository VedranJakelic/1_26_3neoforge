package net.beamex.shamaschizm.serpent;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
@EventBusSubscriber(modid=Shamaschizm.MOD_ID)
public final class SerpentEscape {
 public record Press(int serpent) implements CustomPacketPayload {
  public static final Type<Press> TYPE=new Type<>(Shamaschizm.id("serpent_escape"));
  public static final StreamCodec<RegistryFriendlyByteBuf,Press> CODEC=new StreamCodec<>(){
   public Press decode(RegistryFriendlyByteBuf b){return new Press(b.readVarInt());}
   public void encode(RegistryFriendlyByteBuf b,Press p){b.writeVarInt(p.serpent);}
  };
  public Type<Press> type(){return TYPE;}
 }
 @SubscribeEvent public static void register(RegisterPayloadHandlersEvent e){e.registrar("1").playToServer(Press.TYPE,Press.CODEC,(p,c)->c.enqueueWork(()->{if(c.player().level().getEntity(p.serpent) instanceof SeaSerpent serpent)serpent.escapePress(c.player());}));}
}
