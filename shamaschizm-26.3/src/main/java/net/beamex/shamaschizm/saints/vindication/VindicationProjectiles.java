package net.beamex.shamaschizm.saints.vindication;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
@EventBusSubscriber(modid=Shamaschizm.MOD_ID)
public final class VindicationProjectiles {
 @SubscribeEvent public static void intercept(EntityTickEvent.Pre e){
  if(!(e.getEntity() instanceof Projectile p)||!(p.level() instanceof ServerLevel l)||!p.isAlive())return;
  Vec3 motion=p.getDeltaMovement(),from=p.position(),to=from.add(motion);if(motion.lengthSqr()<1e-6)return;
  VindicationEntity closest=null;Vec3 impact=null;double best=Double.MAX_VALUE;
  for(var boss:l.getEntitiesOfClass(VindicationEntity.class,new AABB(from,to).inflate(3.2),b->b.isAlive()&&b.phaseAge()>=40)){
   Vec3 hit=RingGeometry.hit(from,to,boss.center(),boss.phaseAge()-40);if(hit==null)continue;
   double distance=from.distanceToSqr(hit);if(distance>=best)continue;
   if(l.clip(new ClipContext(from,hit,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p)).getType()!=HitResult.Type.MISS)continue;
   closest=boss;impact=hit;best=distance;
  }
  if(closest!=null){closest.ringHit(impact);var owner=p.getOwner();p.setPos(impact.subtract(motion.normalize().scale(.12)));p.deflect(ProjectileDeflection.REVERSE,closest,null,false,1.0F);p.setOwner(owner);p.needsSync=true;}
 }
}
