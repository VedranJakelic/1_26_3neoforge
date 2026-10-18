package net.beamex.shamaschizm.saints.audio;
import java.util.*;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.EntitySpawnReason;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
@EventBusSubscriber(modid=Shamaschizm.MOD_ID)
public final class ArenaAudio {
 public static volatile float clientGain=1;
 public static final Queue<Music> MUSIC=new java.util.concurrent.ConcurrentLinkedQueue<>();
 public record Music(UUID encounter,boolean active,boolean cue,net.minecraft.world.phys.Vec3 centre) implements CustomPacketPayload {
  public static final Type<Music> TYPE=new Type<>(Shamaschizm.id("saints_music"));
  public static final StreamCodec<RegistryFriendlyByteBuf,Music> CODEC=new StreamCodec<>(){
   public Music decode(RegistryFriendlyByteBuf b){return new Music(b.readUUID(),b.readBoolean(),b.readBoolean(),new net.minecraft.world.phys.Vec3(b.readDouble(),b.readDouble(),b.readDouble()));}
   public void encode(RegistryFriendlyByteBuf b,Music m){b.writeUUID(m.encounter);b.writeBoolean(m.active);b.writeBoolean(m.cue);b.writeDouble(m.centre.x);b.writeDouble(m.centre.y);b.writeDouble(m.centre.z);}
  };
  public Type<Music> type(){return TYPE;}
 }
 public record Gain(float value) implements CustomPacketPayload {
  public static final Type<Gain> TYPE=new Type<>(Shamaschizm.id("arena_gain"));
  public static final StreamCodec<RegistryFriendlyByteBuf,Gain> CODEC=new StreamCodec<>(){
   public Gain decode(RegistryFriendlyByteBuf b){return new Gain(b.readFloat());}
   public void encode(RegistryFriendlyByteBuf b,Gain g){b.writeFloat(g.value);}
  };
  public Type<Gain> type(){return TYPE;}
 }
 @SubscribeEvent public static void register(RegisterPayloadHandlersEvent e){
  e.registrar("2").playToClient(Gain.TYPE,Gain.CODEC,(g,c)->clientGain=Math.max(0,Math.min(1,g.value)))
   .playToClient(Music.TYPE,Music.CODEC,(m,c)->MUSIC.add(m));
 }
 @SubscribeEvent public static void tick(PlayerTickEvent.Post e){
  if(e.getEntity() instanceof ServerPlayer p && p.tickCount%2==0){
   float gain=ArenaZones.get(p.level()).gain(p.position());
   var locks=net.beamex.shamaschizm.experimental.BossGardenLocks.get(p.level());
   // Override both the recorded zone mute and the legacy arena-lock mute after victory.
   // Keep insideArena unchanged: natural-spawn protection survives the boss fight.
   if(locks.insideDefeatedArena(p.blockPosition()))gain=1;
   else if(locks.insideArena(p.blockPosition()))gain=0;
   PacketDistributor.sendToPlayer(p,new Gain(gain));
  }
 }
 @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.LOWEST) public static void spawn(MobSpawnEvent.PositionCheck e){
  if(e.getSpawnType()!=EntitySpawnReason.NATURAL && e.getSpawnType()!=EntitySpawnReason.CHUNK_GENERATION)return;
  ServerLevel l=e.getLevel().getLevel();
  if(ArenaZones.get(l).arena(e.getEntity().blockPosition()) || net.beamex.shamaschizm.experimental.BossGardenLocks.get(l).insideArena(e.getEntity().blockPosition()))
   e.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
 }
 @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.LOWEST)
 public static void finalizeSpawn(net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent e){
  if(e.getSpawnType()!=EntitySpawnReason.NATURAL && e.getSpawnType()!=EntitySpawnReason.CHUNK_GENERATION)return;
  ServerLevel l=e.getLevel().getLevel();
  if(ArenaZones.get(l).arena(e.getEntity().blockPosition()) || net.beamex.shamaschizm.experimental.BossGardenLocks.get(l).insideArena(e.getEntity().blockPosition()))e.setSpawnCancelled(true);
 }
 public static void send(ServerLevel l,UUID id,net.minecraft.world.phys.Vec3 centre,boolean active,boolean cue){
  for(ServerPlayer p:l.players())if(p.position().distanceToSqr(centre)<128*128)
   PacketDistributor.sendToPlayer(p,new Music(id,active,cue,centre));
 }
}
