package net.beamex.shamaschizm.astral;
import java.util.*;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.dreamcatcher.DreamcatcherRegistration;
import net.beamex.shamaschizm.effect.ModEffects;
import net.beamex.shamaschizm.localportal.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.world.chunk.*;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;
/** The real player, inventory and damage remain at the bed. Only a viewpoint moves. */
@EventBusSubscriber(modid=Shamaschizm.MOD_ID)
public final class AstralProjection {
 private static final Map<UUID,Session> SESSIONS=new java.util.concurrent.ConcurrentHashMap<>();
 private static long nextSession;
 private static final String RECOVERY="shamaschizm_astral_restore=";
 public static final TicketController TICKETS=new TicketController(Shamaschizm.id("astral_projection"),(level,helper)->{
  helper.getEntityTickets().keySet().forEach(helper::removeAllTickets);
  helper.getBlockTickets().keySet().forEach(helper::removeAllTickets);
 });
 private static final class Session {
  final long id=++nextSession;final ServerLevel level;final BlockPos bed,other;final BlockState bedState,otherState;final MobEffectInstance original;
  final Set<Long> tickets=new HashSet<>();Vec3 position;float yaw,pitch,danger;int age,returnTicks,portalCooldown,lastInput;
  AstralNetwork.Input input;boolean departed;
  Session(ServerPlayer p,BlockPos pos){
   level=p.level();bed=pos.immutable();bedState=level.getBlockState(bed);
   other=bed.relative(bedState.getValue(BedBlock.PART)==BedPart.HEAD?bedState.getValue(BedBlock.FACING).getOpposite():bedState.getValue(BedBlock.FACING));otherState=level.getBlockState(other);
   original=new MobEffectInstance(p.getEffect(ModEffects.TRIPPING));position=p.getEyePosition();yaw=p.getYRot();pitch=p.getXRot();
  }
 }
 public static boolean active(Entity e){return e instanceof ServerPlayer&&SESSIONS.containsKey(e.getUUID());}
 public static Vec3 viewpoint(ServerPlayer p){var s=SESSIONS.get(p.getUUID());return s==null?p.getEyePosition():s.position;}
 public static Vec3 trackingPosition(ServerPlayer p){var s=SESSIONS.get(p.getUUID());return s==null?p.position():s.position;}
 private static ChunkPos chunk(Vec3 p){return new ChunkPos(Mth.floor(p.x)>>4,Mth.floor(p.z)>>4);}
 public static ChunkPos trackingChunk(ServerPlayer p){return chunk(trackingPosition(p));}
 public static boolean eligible(ServerPlayer p,BlockPos bed){
  if(active(p)||!p.isAlive()||p.isSpectator()||p.isSleeping()||!p.hasEffect(ModEffects.TRIPPING)||p.distanceToSqr(Vec3.atCenterOf(bed))>25||!(p.level().getBlockState(bed).getBlock() instanceof BedBlock))return false;
  var bedState=p.level().getBlockState(bed);
  BlockPos other=bed.relative(bedState.getValue(BedBlock.PART)==BedPart.HEAD?bedState.getValue(BedBlock.FACING).getOpposite():bedState.getValue(BedBlock.FACING));
  for(BlockPos pos:BlockPos.betweenClosed(bed.offset(-6,-5,-6),bed.offset(6,5,6)))
   if(Math.min(pos.distSqr(bed),pos.distSqr(other))<=25&&p.level().getBlockState(pos).is(DreamcatcherRegistration.BLOCK))return true;
  return false;
 }
 @SubscribeEvent public static void useBed(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock e){
  if(!(e.getEntity() instanceof ServerPlayer p)||p.isSecondaryUseActive()||e.getHand()!=net.minecraft.world.InteractionHand.MAIN_HAND)return;
  BlockPos pos=e.getPos();BlockState state=p.level().getBlockState(pos);
  if(!(state.getBlock() instanceof BedBlock))return;
  if(state.getValue(BedBlock.PART)==BedPart.FOOT)pos=pos.relative(state.getValue(BedBlock.FACING));
  if(begin(p,pos)){e.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);e.setCanceled(true);}
 }
 public static boolean begin(ServerPlayer p,BlockPos bed){
  if(!eligible(p,bed)||p.level().getBlockState(bed).getValue(BedBlock.OCCUPIED))return false;
  Session s=new Session(p,bed);if(!(s.otherState.getBlock() instanceof BedBlock)||!p.startSleeping(bed))return false;
  SESSIONS.put(p.getUUID(),s);
  p.addTag(RECOVERY+s.original.getDuration()+","+s.original.getAmplifier()+","+s.original.isAmbient()+","+s.original.isVisible()+","+s.original.showIcon());
  p.closeContainer();p.removeEffect(ModEffects.TRIPPING);p.addEffect(new MobEffectInstance(ModEffects.TRIPPING,-1,AstralRules.MAX_TRIP_LEVEL,false,false,true));
  p.level().updateSleepingPlayerList();tickets(p,s);send(p,s,false);return true;
 }
 public static void input(ServerPlayer p,AstralNetwork.Input input){
  Session s=SESSIONS.get(p.getUUID());if(s==null||input.session()!=s.id||!AstralRules.finite(input.forward(),input.strafe(),input.vertical(),input.yaw(),input.pitch()))return;
  s.input=input;s.lastInput=s.age;
 }
 @SubscribeEvent public static void register(RegisterTicketControllersEvent e){e.register(TICKETS);}
 private static boolean intact(BlockState now,BlockState original){return now.getBlock()==original.getBlock()&&now.getBlock() instanceof BedBlock&&now.getValue(BedBlock.FACING)==original.getValue(BedBlock.FACING)&&now.getValue(BedBlock.PART)==original.getValue(BedBlock.PART);}
 @SubscribeEvent public static void tick(PlayerTickEvent.Post e){
  if(!(e.getEntity() instanceof ServerPlayer p))return;Session s=SESSIONS.get(p.getUUID());if(s==null)return;
  if(!p.isAlive()||p.level()!=s.level){finish(p,false);return;}
  if(!intact(s.level.getBlockState(s.bed),s.bedState)||!intact(s.level.getBlockState(s.other),s.otherState)){kill(p);return;}
  ++s.age;p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;
  if(!p.hasEffect(ModEffects.TRIPPING)||p.getEffect(ModEffects.TRIPPING).getAmplifier()!=AstralRules.MAX_TRIP_LEVEL){p.removeEffect(ModEffects.TRIPPING);p.addEffect(new MobEffectInstance(ModEffects.TRIPPING,-1,AstralRules.MAX_TRIP_LEVEL,false,false,true));}
  Vec3 previous=s.position;boolean crossed=false;
  if(s.input!=null&&s.age-s.lastInput<10){
   var i=s.input;s.yaw=Mth.wrapDegrees(i.yaw());s.pitch=Mth.clamp(i.pitch(),-90,90);
   Vec3 look=Vec3.directionFromRotation(s.pitch,s.yaw),right=Vec3.directionFromRotation(0,s.yaw+90);
   Vec3 move=look.scale(Mth.clamp(i.forward(),-1,1)).add(right.scale(Mth.clamp(i.strafe(),-1,1))).add(0,Mth.clamp(i.vertical(),-1,1),0);
   if(move.lengthSqr()>1)move=move.normalize();Vec3 next=s.position.add(move.scale(i.fast()?AstralRules.FAST_SPEED:AstralRules.SPEED));
   if(s.level.getWorldBorder().isWithinBounds(BlockPos.containing(next))&&next.y>s.level.getMinY()-32&&next.y<s.level.getMaxY()+128)s.position=next;
  }
  if(s.portalCooldown>0)--s.portalCooldown;
  else if(s.level.dimension().equals(net.beamex.shamaschizm.world.Schizm.KEY)){
   var data=DoorwayData.get(s.level);
   for(var source:data.all()){
    var dest=data.get(source.target());if(!source.enabled()||dest==null||!dest.enabled()||!dest.target().equals(source.name())||!source.crossed(previous,s.position,.05F,.05F))continue;
    s.position=source.transformPoint(dest,s.position).add(dest.facing().normal.scale(.12));
    Vec3 look=source.transformVector(dest,Vec3.directionFromRotation(s.pitch,s.yaw));s.yaw=(float)Math.toDegrees(Math.atan2(-look.x,look.z));s.pitch=(float)-Math.toDegrees(Math.asin(Mth.clamp(look.y,-1,1)));
    s.input=null;s.portalCooldown=5;crossed=true;break;
   }
  }
  if(s.age%10==0||crossed)tickets(p,s);s.level.getChunkSource().move(p);
  if(s.level.dimension().equals(net.beamex.shamaschizm.world.Schizm.KEY)&&(s.age%20==0||crossed)){
   var data=DoorwayData.get(s.level);Map<String,Doorway> visible=new LinkedHashMap<>();
   for(var d:data.all())if(d.origin().distanceToSqr(s.position)<64*64){visible.put(d.name(),d);var other=data.get(d.target());if(other!=null)visible.put(other.name(),other);}
   PacketDistributor.sendToPlayer(p,new DoorwayNetwork.State(List.copyOf(visible.values())));
  }
  Vec3 body=Vec3.atCenterOf(s.bed).add(0,.25,0);double distance=s.position.distanceTo(body);if(distance>2)s.departed=true;
  Vec3 look=Vec3.directionFromRotation(s.pitch,s.yaw);
  boolean align=s.departed&&distance<AstralRules.RETURN_RADIUS&&(distance<.35||look.dot(body.subtract(s.position).normalize())>.94);
  s.returnTicks=align?s.returnTicks+1:0;if(s.returnTicks>=AstralRules.RETURN_TICKS){finish(p,true);return;}
  boolean gaze=false;
  for(boolean moon:new boolean[]{false,true}){
   Vec3 celestial=celestial(s.level,s.position,moon);
   // Astral celestial visions remain visible through terrain and below the horizon.
   // Match the world-anchored glow, not the sleeping body's access to open sky.
   if(look.dot(celestial)>.985)gaze=true;
  }
  s.danger=AstralRules.gaze(s.danger,gaze);if(s.danger>=.9999F){kill(p);return;}send(p,s,crossed);
 }
 public static Vec3 celestial(ServerLevel level,Vec3 position,boolean moon){float a=level.environmentAttributes().getValue(moon?EnvironmentAttributes.MOON_ANGLE:EnvironmentAttributes.SUN_ANGLE,position);double r=Math.toRadians(a);return new Vec3(-Math.sin(r),Math.cos(r),0);}
 private static void tickets(ServerPlayer p,Session s){
  ChunkPos center=chunk(s.position);Set<Long> wanted=new HashSet<>();
  for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)wanted.add(new ChunkPos(center.x()+x,center.z()+z).pack());
  for(long n:wanted)if(s.tickets.add(n)){ChunkPos c=ChunkPos.unpack(n);TICKETS.forceChunk(s.level,p.getUUID(),c.x(),c.z(),true,false);}
  for(var it=s.tickets.iterator();it.hasNext();){long n=it.next();if(!wanted.contains(n)){ChunkPos c=ChunkPos.unpack(n);TICKETS.forceChunk(s.level,p.getUUID(),c.x(),c.z(),false,false);it.remove();}}
 }
 private static void send(ServerPlayer p,Session s,boolean crossed){PacketDistributor.sendToPlayer(p,new AstralNetwork.State(s.id,true,s.position.x,s.position.y,s.position.z,s.yaw,s.pitch,s.age,s.danger,crossed));}
 private static void kill(ServerPlayer p){finish(p,false);p.kill(p.level());}
 public static void finish(ServerPlayer p,boolean restore){
  Session s=SESSIONS.remove(p.getUUID());if(s==null)return;
  for(String tag:new ArrayList<>(p.entityTags()))if(tag.startsWith(RECOVERY))p.removeTag(tag);
  for(long n:s.tickets){ChunkPos c=ChunkPos.unpack(n);TICKETS.forceChunk(s.level,p.getUUID(),c.x(),c.z(),false,false);}
  p.removeEffect(ModEffects.TRIPPING);if(restore&&p.isAlive())p.addEffect(s.original);
  p.stopSleepInBed(true,true);p.level().getChunkSource().move(p);
  PacketDistributor.sendToPlayer(p,new AstralNetwork.State(s.id,false,0,0,0,0,0,0,0,false));
 }
 @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){
  if(!(e.getEntity() instanceof ServerPlayer p))return;
  for(String tag:new ArrayList<>(p.entityTags()))if(tag.startsWith(RECOVERY)){
   p.removeTag(tag);p.removeEffect(ModEffects.TRIPPING);
   try{String[] v=tag.substring(RECOVERY.length()).split(",");int duration=Integer.parseInt(v[0]),amp=Integer.parseInt(v[1]);
    if(duration==-1||duration>0)p.addEffect(new MobEffectInstance(ModEffects.TRIPPING,duration,Math.max(0,Math.min(255,amp)),Boolean.parseBoolean(v[2]),Boolean.parseBoolean(v[3]),Boolean.parseBoolean(v[4])));
   }catch(RuntimeException ignored){}p.stopSleepInBed(true,true);
  }
 }
 @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p)finish(p,true);}
 @SubscribeEvent public static void changed(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p)finish(p,true);}
 @SubscribeEvent public static void stopped(ServerStoppedEvent e){SESSIONS.clear();}
}
