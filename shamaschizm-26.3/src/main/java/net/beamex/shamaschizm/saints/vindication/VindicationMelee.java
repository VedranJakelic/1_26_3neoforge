package net.beamex.shamaschizm.saints.vindication;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
@EventBusSubscriber(modid=Shamaschizm.MOD_ID)
public final class VindicationMelee {
 public record Contact(VindicationEntity boss,Vec3 point){}
 /** First ring surface along the actual melee reach, before terrain, another entity or the core. */
 public static Contact trace(Player p){
  if(p.isSpectator() || !p.isAlive())return null;
  Vec3 from=p.getEyePosition(),to=from.add(p.getLookAngle().scale(p.entityInteractionRange()));
  var block=p.level().clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
  double limit=from.distanceToSqr(block.getLocation());Contact result=null;
  var area=new AABB(from,to).inflate(3.2);
  for(var entity:p.level().getEntities(p,area,e->e.isPickable()&&!e.isSpectator())){
   var box=entity.getBoundingBox();Vec3 hit=box.contains(from)?from:box.clip(from,to).orElse(null);
   if(hit!=null)limit=Math.min(limit,from.distanceToSqr(hit));
  }
  for(var boss:p.level().getEntitiesOfClass(VindicationEntity.class,area,b->b.isAlive()&&b.phaseAge()>=40)){
   Vec3 hit=RingGeometry.hit(from,to,boss.center(),boss.phaseAge()-40);
   if(hit!=null && from.distanceToSqr(hit)<limit){limit=from.distanceToSqr(hit);result=new Contact(boss,hit);}
  }
  return result;
 }
 public record Swing(int bossId) implements CustomPacketPayload {
  public static final Type<Swing> TYPE=new Type<>(Shamaschizm.id("vindication_ring_swing"));
  public static final StreamCodec<RegistryFriendlyByteBuf,Swing> CODEC=new StreamCodec<>(){
   public Swing decode(RegistryFriendlyByteBuf b){return new Swing(b.readVarInt());}
   public void encode(RegistryFriendlyByteBuf b,Swing s){b.writeVarInt(s.bossId);}
  };
  public Type<Swing> type(){return TYPE;}
 }
 @SubscribeEvent public static void register(RegisterPayloadHandlersEvent e){
  e.registrar("1").playToServer(Swing.TYPE,Swing.CODEC,(s,c)->{
   if(c.player() instanceof ServerPlayer p){
    Contact hit=trace(p);
    if(hit!=null && hit.boss.getId()==s.bossId){hit.boss.ringHit(hit.point);p.resetAttackStrengthTicker();}
   }
  });
 }
 @SubscribeEvent public static void attack(AttackEntityEvent e){
  if(!(e.getEntity() instanceof ServerPlayer p))return;
  Contact hit=trace(p);
  if(hit!=null){e.setCanceled(true);hit.boss.ringHit(hit.point);p.resetAttackStrengthTicker();}
 }
}
