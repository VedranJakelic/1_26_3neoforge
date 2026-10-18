package net.beamex.shamaschizm.saints.audio;
import java.util.Queue;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.resources.Identifier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
/** Quiet delayed reflections; not a global sound-engine/OpenAL modification. */
@EventBusSubscriber(modid=Shamaschizm.MOD_ID)
public final class VindicationEcho {
 public static final Queue<Echo> PENDING=new java.util.concurrent.ConcurrentLinkedQueue<>();
 public record Echo(Identifier sound,Vec3 pos,float volume,float pitch) implements CustomPacketPayload {
  public static final Type<Echo> TYPE=new Type<>(Shamaschizm.id("vindication_echo"));
  public static final StreamCodec<RegistryFriendlyByteBuf,Echo> CODEC=new StreamCodec<>(){
   public Echo decode(RegistryFriendlyByteBuf b){return new Echo(b.readIdentifier(),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),b.readFloat(),b.readFloat());}
   public void encode(RegistryFriendlyByteBuf b,Echo e){b.writeIdentifier(e.sound);b.writeDouble(e.pos.x);b.writeDouble(e.pos.y);b.writeDouble(e.pos.z);b.writeFloat(e.volume);b.writeFloat(e.pitch);}
  };
  public Type<Echo> type(){return TYPE;}
 }
 @SubscribeEvent public static void register(RegisterPayloadHandlersEvent e){e.registrar("1").playToClient(Echo.TYPE,Echo.CODEC,(p,c)->PENDING.add(p));}
 public static void send(ServerLevel l,SoundEvent sound,Vec3 p,float volume,float pitch){
  for(var player:l.players())if(player.position().distanceToSqr(p)<96*96)
   PacketDistributor.sendToPlayer(player,new Echo(sound.location(),p,volume,pitch));
 }
}
