package net.beamex.shamaschizm.earthworm;

import java.util.*;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** A grounded crawler: no vanilla goals, navigation, jump, body yaw or knockback motion. */
public final class EarthwormEntity extends PathfinderMob {
 private static final EntityDataAccessor<Float> CLOCK=SynchedEntityData.defineId(EarthwormEntity.class,EntityDataSerializers.FLOAT);
 private static final List<EntityDataAccessor<Vector3fc>> PARTS=new ArrayList<>();
 static {for(int i=0;i<WormMotion.PARTS;i++)PARTS.add(SynchedEntityData.defineId(EarthwormEntity.class,EntityDataSerializers.VECTOR3));}
 private final List<Vec3> trail=new ArrayList<>(); // newest head position first, bounded by length
 private final Vector3f[] before=new Vector3f[9],after=new Vector3f[9];
 private float beforeClock,afterClock;
 private boolean clientReady;
 private double clock,heading,targetHeading,fallSpeed;
 private int chooseIn;
 private int unseenTicks;
 private Vec3 lastHead;
 private int fallenLifetime = -1;
 private Vec3 flopVelocity = Vec3.ZERO;
 private boolean fallenLanded;
 public EarthwormEntity(EntityType<? extends PathfinderMob> type,Level level){super(type,level);setSilent(true);}
 public static AttributeSupplier.Builder attributes(){return createMobAttributes().add(Attributes.MAX_HEALTH,1).add(Attributes.MOVEMENT_SPEED,0).add(Attributes.KNOCKBACK_RESISTANCE,1).add(Attributes.STEP_HEIGHT,0);}
 @Override protected void registerGoals(){}
 @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(CLOCK,0F);for(int i=0;i<9;i++)b.define(PARTS.get(i),new Vector3f(0,0,(float)WormMotion.REST[i]));}
 @Override public void travel(Vec3 ignored){} // movement belongs exclusively to crawl(), on the server
 @Override public void tick(){
  super.tick();
  if(level() instanceof ServerLevel server){if(isAlive()){if(fallenLifetime >= 0)fallTick(server);else crawl(server);}}
  else {
   for(int i=0;i<9;i++){Vector3f next=new Vector3f(entityData.get(PARTS.get(i)));before[i]=clientReady?after[i]:next;after[i]=next;}
   beforeClock=clientReady?afterClock:entityData.get(CLOCK);afterClock=entityData.get(CLOCK);clientReady=true;
  }
 }
 /** Detached worms scatter, land, then use ordinary crawler movement until their timer expires. */
 public void fallAndWait(Vec3 velocity,int lifetime){
  fallenLifetime=lifetime;flopVelocity=velocity;fallenLanded=false;setPersistenceRequired();
  heading=velocity.horizontalDistanceSqr()>1e-6?Math.atan2(-velocity.x,-velocity.z):random.nextDouble()*Math.PI*2;
  heading+=(random.nextDouble()-.5)*1.1;targetHeading=heading;clock=random.nextDouble()*WormMotion.PERIOD;
  setYRot((float)(180-Math.toDegrees(heading)));yBodyRot=yHeadRot=getYRot();setOldPosAndRot();
  resetTrail();publish();
 }
 private void fallTick(ServerLevel server){
  if(--fallenLifetime<=0){discard();return;}
  if(fallenLanded){crawl(server);return;}
  flopVelocity=flopVelocity.add(0,-.04,0);
  move(MoverType.SELF,flopVelocity);
  if(onGround()){
   flopVelocity=Vec3.ZERO;fallenLanded=true;chooseIn=0;resetTrail();
  }else flopVelocity=new Vec3(horizontalCollision?0:flopVelocity.x*.98,verticalCollision?0:flopVelocity.y*.98,horizontalCollision?0:flopVelocity.z*.98);
  // No locomotion cycle while airborne: the actual crawl begins on landing.
  resetTrail();publish();
 }
 private Vec3 direction(double a){return new Vec3(-Math.sin(a),0,-Math.cos(a));}
 private void resetTrail(){
  trail.clear();Vec3 back=direction(heading).scale(-1);
  for(int i=0;i<=80;i++)trail.add(position().add(back.scale(i*.02)));
  lastHead=position();
 }
 private Vec3 ground(ServerLevel level,Vec3 at){
  var hit=level.clip(new ClipContext(at.add(0,.12,0),at.add(0,-.20,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
  return hit.getType()==HitResult.Type.BLOCK?hit.getLocation():null;
 }
 private boolean safe(ServerLevel level,double a,double distance){
  Vec3 at=position().add(direction(a).scale(distance));Vec3 floor=ground(level,at);
  return floor!=null&&Math.abs(floor.y-getY())<=.09
    &&!level.getBlockCollisions(this,getBoundingBox().move(floor.x-getX(),floor.y-getY(),floor.z-getZ())).iterator().hasNext();
 }
 private void crawl(ServerLevel level){
  setDeltaMovement(Vec3.ZERO);getNavigation().stop();setJumping(false);
  if(trail.isEmpty()||lastHead==null||position().distanceToSqr(lastHead)>1){heading=Math.toRadians(180-getYRot());targetHeading=heading;resetTrail();}
  Vec3 floor=ground(level,position());
  if(floor==null||getY()-floor.y>.03){
   fallSpeed=Math.max(-.5,fallSpeed-.04);move(MoverType.SELF,new Vec3(0,fallSpeed,0));if(onGround())fallSpeed=0;
   resetTrail();publish();return;
  }
  fallSpeed=0;
  if(--chooseIn<=0){targetHeading=heading+(random.nextDouble()-.5)*1.4;chooseIn=80+random.nextInt(120);}
  // Probe ahead before the nose reaches a wall or ledge; turn with forward travel only.
  if(!safe(level,heading,.38)){
   double left=heading+.9,right=heading-.9;
   if(safe(level,left,.30))targetHeading=left;
   else if(safe(level,right,.30))targetHeading=right;
   chooseIn=20;
  }
  double nextClock=clock+.05;
  double step=WormMotion.forward(0,nextClock)-WormMotion.forward(0,clock);
  double nextHeading=WormMotion.steer(heading,targetHeading,step);
  // The longer look-ahead can land beyond a narrow gap; also validate the actual step.
  if(step>1e-9&&(!safe(level,nextHeading,Math.max(.07,step))||!safe(level,nextHeading,step))) {
   // A blocked crawler must be able to turn before advancing, not freeze forever against the wall.
   for(int i=1;i<=12;i++){
    double candidate=heading+(i%2==0?-1:1)*((i+1)/2)*Math.PI/6;
    if(safe(level,candidate,.12)){targetHeading=candidate;heading+=Math.max(-.12,Math.min(.12,WormMotion.wrap(targetHeading-heading)));break;}
   }
   resetTrail();publish();return;
  }
  if(step>1e-9){
   Vec3 wanted=position().add(direction(nextHeading).scale(step));Vec3 support=ground(level,wanted);
   // Never dereference a missing floor, including after block/structure changes.
   if(support==null||Math.abs(support.y-getY())>.09){chooseIn=0;publish();return;}
   Vec3 old=position();move(MoverType.SELF,new Vec3(wanted.x-getX(),support.y-getY(),wanted.z-getZ()));
   if(position().distanceToSqr(old)<step*step*.5){publish();return;}
   heading=nextHeading;
   if(trail.size()>1&&position().distanceToSqr(trail.get(1))<.004*.004)trail.set(0,position());
   else trail.add(0,position());
   double total=0;for(int i=1;i<trail.size();i++){total+=trail.get(i-1).distanceTo(trail.get(i));if(total>1.6){trail.subList(i+1,trail.size()).clear();break;}}
  }
  clock=nextClock;publish();
 }
 private Vec3 sample(double distance){
  for(int i=1;i<trail.size();i++){
   Vec3 a=trail.get(i-1),b=trail.get(i);double length=a.distanceTo(b);
   if(distance<=length)return a.lerp(b,length<1e-9?0:distance/length);
   distance-=length;
  }
  return trail.get(trail.size()-1);
 }
 private void publish(){
  lastHead=position();entityData.set(CLOCK,(float)clock);
  for(int i=0;i<9;i++){Vec3 p=sample(WormMotion.behind(i,clock)).subtract(position());entityData.set(PARTS.get(i),new Vector3f((float)p.x,(float)p.y,(float)p.z));}
 }
 public Vector3f part(int i,float partial){return clientReady?new Vector3f(before[i]).lerp(after[i],partial):new Vector3f(entityData.get(PARTS.get(i)));}
 public float animationTime(float partial){return clientReady?beforeClock+(afterClock-beforeClock)*partial:entityData.get(CLOCK);}
 @Override public boolean isPushable(){return false;}
 @Override public boolean canBeCollidedWith(Entity e){return false;}
 @Override public boolean canCollideWith(Entity e){return false;}
 @Override public void push(Entity e){}
 @Override public void push(double x,double y,double z){}
 @Override protected void doPush(Entity e){}
 @Override public void knockback(double strength,double x,double z,net.minecraft.world.damagesource.DamageSource source,float damage,boolean force){}
 @Override public void knockback(double strength,double x,double z,net.minecraft.world.damagesource.DamageSource source,float damage){}
 @Override public void jumpFromGround(){}
 @Override public boolean isSilent(){return true;}
 @Override public boolean causeFallDamage(double distance,float multiplier,net.minecraft.world.damagesource.DamageSource source){return fallenLifetime<0 && super.causeFallDamage(distance,multiplier,source);}
 @Override public boolean removeWhenFarAway(double distance){return true;}
 // Match roach visibility-based cleanup. Named/persistent worms are retained.
 @Override public void checkDespawn(){
  if(fallenLifetime>=0)return;
  super.checkDespawn();
  if(isRemoved()||isPersistenceRequired()||requiresCustomPersistence()){unseenTicks=0;return;}
  if(!(level() instanceof ServerLevel server)||tickCount%10!=0)return;
  boolean observed=false;
  for(var player:server.players()){
   if(!player.isAlive()||player.isSpectator()||player.distanceToSqr(this)>48*48)continue;
   Vec3 toward=getBoundingBox().getCenter().subtract(player.getEyePosition()).normalize();
   if(player.getLookAngle().dot(toward)>=.20&&player.hasLineOfSight(this)){observed=true;break;}
  }
  if(observed)unseenTicks=0;
  else if((unseenTicks+=10)>=100)discard();
 }
 @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);out.putInt("WisdomFallenLifetime",fallenLifetime);out.putBoolean("WisdomFallenLanded",fallenLanded);out.store("WisdomFlopVelocity",Vec3.CODEC,flopVelocity);out.putDouble("WormClock",clock);out.putDouble("WormHeading",heading);out.store("WormTrail",Vec3.CODEC.listOf(),trail);}
 @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);fallenLifetime=in.getIntOr("WisdomFallenLifetime",-1);fallenLanded=in.getBooleanOr("WisdomFallenLanded",false);flopVelocity=in.read("WisdomFlopVelocity",Vec3.CODEC).orElse(Vec3.ZERO);getAttribute(Attributes.MAX_HEALTH).setBaseValue(1);setHealth(Math.min(getHealth(),getMaxHealth()));clock=in.getDoubleOr("WormClock",0);heading=in.getDoubleOr("WormHeading",0);targetHeading=heading;trail.clear();trail.addAll(in.read("WormTrail",Vec3.CODEC.listOf()).orElse(List.of()));lastHead=position();setSilent(true);}
}
