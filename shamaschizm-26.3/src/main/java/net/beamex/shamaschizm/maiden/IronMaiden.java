package net.beamex.shamaschizm.maiden;
import java.util.UUID;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import net.minecraft.tags.DamageTypeTags;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import static net.beamex.shamaschizm.maiden.MaidenMotion.*;
/** Persistent mimic. Navigation steers; the rapid lid stroke supplies each forward hop. */
public final class IronMaiden extends PathfinderMob {
 private static final EntityDataAccessor<Integer> STATE=SynchedEntityData.defineId(IronMaiden.class,EntityDataSerializers.INT),CYCLE=SynchedEntityData.defineId(IronMaiden.class,EntityDataSerializers.INT),VARIANT=SynchedEntityData.defineId(IronMaiden.class,EntityDataSerializers.INT);
 private static final EntityDataAccessor<Long> START=SynchedEntityData.defineId(IronMaiden.class,EntityDataSerializers.LONG);
 private static final EntityDataAccessor<Boolean> BONES=SynchedEntityData.defineId(IronMaiden.class,EntityDataSerializers.BOOLEAN);
 private static final EntityDataAccessor<Vector3fc> CLOSE_FROM=SynchedEntityData.defineId(IronMaiden.class,EntityDataSerializers.VECTOR3);
 private UUID quarry;private long wakeAt=-1;private int lostTicks;private boolean launched,clanged,releasing;private Vec3 previous;private int repathIn;private boolean stepHop;
 public IronMaiden(EntityType<? extends PathfinderMob> type,Level level){super(type,level);setPersistenceRequired();xpReward=5;
  moveControl=new net.minecraft.world.entity.ai.control.MoveControl(this){@Override public void tick(){}};}
 public static AttributeSupplier.Builder attributes(){return createMobAttributes().add(Attributes.MAX_HEALTH,24).add(Attributes.MOVEMENT_SPEED,.25).add(Attributes.FOLLOW_RANGE,28).add(Attributes.KNOCKBACK_RESISTANCE,1).add(Attributes.STEP_HEIGHT,1.0);}
 @Override protected void registerGoals(){}
 @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(STATE,DORMANT);b.define(START,0L);b.define(CYCLE,MIN_CYCLE);b.define(VARIANT,0);b.define(BONES,false);b.define(CLOSE_FROM,new Vector3f(-145,145,.4F));}
 /** Only called by the successful player-spawn hooks, never on reload or portal travel. */
 public void faceSpawner(Player player){
  if(player==null||player.level()!=level())return;
  Vec3 d=player.position().subtract(position());
  float yaw=d.horizontalDistanceSqr()>1e-8?(float)Math.toDegrees(Math.atan2(-d.x,d.z)):player.getYRot()+180;
  setYRot(yaw);yBodyRot=yBodyRotO=yHeadRot=yHeadRotO=yaw;setOldPosAndRot();syncPosition=true;
 }
 @Override protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source){return net.minecraft.sounds.SoundEvents.IRON_GOLEM_HURT;}
 public int state(){return entityData.get(STATE);}public int cycle(){return entityData.get(CYCLE);}public int variant(){return entityData.get(VARIANT);}
 public float phaseAge(float partial){return Math.max(0,level().getGameTime()-entityData.get(START)+partial);}
 public MaidenMotion.Pose pose(float partial){
  var p=MaidenMotion.pose(state(),phaseAge(partial),cycle(),variant());
  if(state()==SWALLOWING||state()==CLOSING){float f=1-smooth(phaseAge(partial)/(state()==SWALLOWING?7:22));var start=entityData.get(CLOSE_FROM);return new MaidenMotion.Pose(start.x()*f,start.y()*f,p.lidPitch(),state()==CLOSING?start.z()+(1-start.z())*(1-f):start.z(),0,0);}return p;
 }
 public boolean skeletonInside(){return entityData.get(BONES);}
 public boolean holds(Entity e){return !releasing&&isAlive()&&hasPassenger(e)&&(state()==SWALLOWING||state()==DIGESTING);}
 @Override public boolean removeWhenFarAway(double d){return false;}
 @Override public void checkDespawn(){}
 @Override public boolean isPushable(){return false;}
 @Override public void push(Entity e){}
 @Override public boolean canBeCollidedWith(Entity e){return isAlive()&&!hasPassenger(e);}
 @Override public void travel(Vec3 ignored){}
 @Override public boolean causeFallDamage(double d,float m,DamageSource s){return false;}
 @Override protected boolean canAddPassenger(Entity e){return e instanceof Player&&getPassengers().isEmpty();}
 @Override public LivingEntity getControllingPassenger(){return null;}
 @Override public Vec3 getPassengerRidingPosition(Entity rider){return position().add(0,.12,0);}
 @Override protected void positionRider(Entity rider,Entity.MoveFunction move){if(hasPassenger(rider)){var at=getPassengerRidingPosition(rider);move.accept(rider,at.x,at.y,at.z);}}
 @Override public Vec3 getDismountLocationForPassenger(LivingEntity passenger){
  for(int i=0;i<8;i++){double a=Math.toRadians(getYRot()+i*45);Vec3 at=position().add(-Math.sin(a)*1.4,.1,Math.cos(a)*1.4);if(level().noCollision(passenger,passenger.getBoundingBox().move(at.subtract(passenger.position()))))return at;}return position().add(0,getBbHeight()+.1,0);
 }
 public void release(){releasing=true;ejectPassengers();releasing=false;}
 private void phase(int state){if(state==SWALLOWING||state==CLOSING){var p=pose(0);entityData.set(CLOSE_FROM,new Vector3f(p.left(),p.right(),p.darkness()));}entityData.set(STATE,state);entityData.set(START,level().getGameTime());launched=clanged=false;if(state!=HUNTING)getNavigation().stop();}
 private static boolean eligible(Player p){return p.isAlive()&&!p.isCreative()&&!p.isSpectator();}
 public void touch(Player player){
  if(!(level() instanceof ServerLevel server)||!eligible(player)||server.getDifficulty()==Difficulty.PEACEFUL)return;
  if(state()!=DORMANT&&state()!=CLOSING)return;quarry=player.getUUID();open(server);
  for(var other:server.getEntitiesOfClass(IronMaiden.class,getBoundingBox().inflate(12))){if(other==this||other.state()!=DORMANT||other.distanceToSqr(this)>144)continue;other.quarry=quarry;long alarm=server.getGameTime()+random.nextInt(20);if(other.wakeAt<0||alarm<other.wakeAt)other.wakeAt=alarm;}
 }
 private void open(ServerLevel server){wakeAt=-1;lostTicks=0;phase(OPENING);setDeltaMovement(new Vec3(0,getDeltaMovement().y,0));entityData.set(BONES,random.nextFloat()<.10F);server.playSound(null,getX(),getY()+1,getZ(),MaidenRegistration.OPEN,net.minecraft.sounds.SoundSource.HOSTILE,1.8F,.96F+random.nextFloat()*.08F);}
 private void clang(ServerLevel server){server.playSound(null,getX(),getY()+1,getZ(),MaidenRegistration.CLANG,net.minecraft.sounds.SoundSource.HOSTILE,3.0F,.92F+random.nextFloat()*.16F);}
 @Override public InteractionResult interact(Player player,InteractionHand hand,Vec3 location){touch(player);return InteractionResult.SUCCESS;}
 @Override public void tick(){
  super.tick();if(!(level() instanceof ServerLevel server))return;if(!isAlive()){release();return;}
  if(previous!=null&&previous.distanceToSqr(position())>16){getNavigation().stop();lostTicks=0;repathIn=0;stepHop=false;}previous=position();
  if(server.getDifficulty()==Difficulty.PEACEFUL){release();if(state()!=DORMANT)phase(DORMANT);wakeAt=-1;}
  if(isNoAi()){setDeltaMovement(Vec3.ZERO);return;}
  float age=phaseAge(0);
  if(state()==DORMANT){if(wakeAt>=0&&server.getGameTime()>=wakeAt)open(server);else for(var p:server.getEntitiesOfClass(Player.class,getBoundingBox().inflate(.08),IronMaiden::eligible)){touch(p);break;}}
  else if(state()==OPENING){if(skeletonInside()&&age>=10F*WAKE_TICKS/65F){releaseBones(server);entityData.set(BONES,false);}if(age>=WAKE_TICKS)beginCycle();}
  else if(state()==HUNTING)hunt(server,age);
  else if(state()==SWALLOWING||state()==DIGESTING){
   setDeltaMovement(new Vec3(0,getDeltaMovement().y,0));
   if(!(getFirstPassenger() instanceof Player p)||!eligible(p)||p.level()!=level()||p.distanceToSqr(this)>16){release();phase(CLOSING);}
   else if(state()==SWALLOWING&&age>=7){clang(server);phase(DIGESTING);}
   else if(state()==DIGESTING&&age>=20&&((int)age)%20==0)p.hurtServer(server,damageSources().inWall(),2);
  }else if(state()==CLOSING&&age>=22){clang(server);phase(DORMANT);quarry=null;}
  if(state()==HUNTING&&pose(0).aperture()>.38F)trySwallow(server,position());
  Vec3 moveStart=position();
  Vec3 v=getDeltaMovement();if(state()!=HUNTING)v=new Vec3(0,v.y,0);if(!isNoGravity())v=v.add(0,-.08,0);move(MoverType.SELF,v);double drag=onGround()?.58:.96;
  setDeltaMovement(horizontalCollision?0:v.x*drag,verticalCollision?0:v.y*.98,horizontalCollision?0:v.z*drag);
  if(state()==HUNTING&&pose(0).aperture()>.38F)trySwallow(server,moveStart);previous=position();
 }
 private void beginCycle(){entityData.set(CYCLE,MIN_CYCLE+random.nextInt(MAX_CYCLE-MIN_CYCLE+1));entityData.set(VARIANT,random.nextInt(6));phase(HUNTING);}
 private void hunt(ServerLevel server,float age){
  Player player=quarry==null?null:server.getPlayerByUUID(quarry);
  boolean viable=player!=null&&eligible(player)&&distanceToSqr(player)<28*28;
  float t=age/cycle();
  if(viable){
   // Repath independently of flap timing; keep the route around corners instead of abandoning it.
   if(--repathIn<=0){getNavigation().moveTo(player,1);repathIn=6;}
   var path=getNavigation().getPath();boolean routed=path!=null&&!path.isDone();
   if(hasLineOfSight(player)||routed)lostTicks=0;else lostTicks++;
   Vec3 aim=routed?path.getNextEntityPos(this):player.position();
   // Only lead the target on a clear, nearby approach, never through a wall.
   if(distanceToSqr(player)<9&&hasLineOfSight(player)
      &&server.noCollision(this,getBoundingBox().expandTowards(player.position().subtract(position())))){
    Vec3 lead=player.getDeltaMovement().multiply(1,0,1).scale(3);
    if(lead.lengthSqr()>.36)lead=lead.normalize().scale(.6);
    Vec3 predicted=player.position().add(lead);
    if(server.noCollision(this,getBoundingBox().move(predicted.subtract(position()))))aim=predicted;
    else aim=player.position();
   }
   Vec3 d=aim.subtract(position());double distance=d.horizontalDistance();
   if(distance>1e-4)setYRot(net.minecraft.util.Mth.approachDegrees(getYRot(),(float)Math.toDegrees(Math.atan2(-d.x,d.z)),onGround()?32:8));
   double yaw=Math.toRadians(getYRot());Vec3 forward=new Vec3(-Math.sin(yaw),0,Math.cos(yaw));
   if(onGround())stepHop=false;
   if(!launched&&t>=.16F&&t<.72F&&onGround()){
    // A short vertical impulse cannot clear a full block. Check headroom before the higher hop.
    Vec3 probe=forward.scale(.65);
    boolean obstacle=!server.noCollision(this,getBoundingBox().move(probe));
    stepHop=(d.y>.4||obstacle)&&server.noCollision(this,getBoundingBox().move(0,1.1,0))
      &&server.noCollision(this,getBoundingBox().move(probe.x,1.1,probe.z));
    double speed=stepHop?.23:MaidenPursuit.hopSpeed(distance);
    setDeltaMovement(forward.scale(speed).add(0,stepHop?.58:.24+(variant()%3)*.015,0));
    launched=true;syncVelocity=true;
   }else if(!onGround()){
    // Modest air steering follows the current route and resumes forward motion after a step collision.
    Vec3 v=getDeltaMovement(),wanted=forward.scale(stepHop?.23:MaidenPursuit.hopSpeed(distance));
    setDeltaMovement(v.x*.8+wanted.x*.2,v.y,v.z*.8+wanted.z*.2);
   }
  }else lostTicks++;
  if(lostTicks>=80){phase(CLOSING);quarry=null;return;}
  if(!clanged&&t>=.32F){clanged=true;clang(server);}yBodyRot=getYRot();yHeadRot=getYRot();if(age>=cycle())beginCycle();
 }
 private void trySwallow(ServerLevel server,Vec3 moveStart){
  // Sweep only this collision-resolved movement, never the gap across a portal teleport.
  if(moveStart.distanceToSqr(position())>4)return;
  var bounds=getBoundingBox().expandTowards(moveStart.subtract(position())).inflate(.8,.6,.8);
  for(var p:server.getEntitiesOfClass(Player.class,bounds,IronMaiden::eligible)){
   if(p.isPassenger()||p.isSleeping())continue;
   Vec3 center=p.getBoundingBox().getCenter(),a=center.subtract(moveStart),b=center.subtract(position());
   if(!MaidenPursuit.crossesMouth(getYRot(),a.x,a.y,a.z,b.x,b.y,b.z,p.getBbWidth()/2,p.getBbHeight()/2))continue;
   if(server.clip(new ClipContext(position().add(0,1,0),center,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this)).getType()!=HitResult.Type.MISS)continue;
   if(p.startRiding(this,true,true)){getNavigation().stop();phase(SWALLOWING);setDeltaMovement(Vec3.ZERO);break;}
  }
 }
 @Override public boolean hurtServer(ServerLevel server,DamageSource source,float amount){
  if(source.is(DamageTypeTags.BYPASSES_INVULNERABILITY))return super.hurtServer(server,source,amount);
  if(source.getEntity() instanceof Player p)touch(p);
  // The authored rest pose has a hairline seam; closed lids still seal the armored shell.
  if(pose(0).aperture()<.08F && (source.getDirectEntity()==null||!holds(source.getDirectEntity())))return false;
  Vec3 from,to;
  if(source.getDirectEntity() instanceof Projectile p){Vec3 v=p.getDeltaMovement();if(v.lengthSqr()<1e-8)return false;from=p.position().subtract(v.normalize().scale(2));to=p.position().add(v.normalize().scale(3));}
  else if(source.getDirectEntity() instanceof LivingEntity a){from=a.getEyePosition();to=from.add(a.getLookAngle().scale(6));}else return false;
  int hit=MaidenGeometry.hit(from.subtract(position()),to.subtract(position()),MaidenMotion.matrices(getYRot(),pose(0)));if(hit!=1&&hit!=2)return false;
  return super.hurtServer(server,source,amount);
 }
 private void releaseBones(ServerLevel server){for(int i=0;i<MaidenBones.PARTS.length;i++){var b=MaidenRegistration.BONE.create(server,EntitySpawnReason.TRIGGERED);if(b==null)continue;b.configure(i,position().add(MaidenBones.center(i).yRot((float)Math.toRadians(180-getYRot()))),getYRot());server.addFreshEntity(b);}}
 @Override public void die(DamageSource s){release();super.die(s);}
 @Override protected void dropCustomDeathLoot(ServerLevel l,DamageSource s,boolean playerKill){super.dropCustomDeathLoot(l,s,playerKill);for(Item item:new Item[]{Items.ROTTEN_FLESH,Items.BONE,Items.IRON_INGOT}){int n=random.nextInt(3);if(n>0)spawnAtLocation(l,new ItemStack(item,n));}}
 @Override protected void addAdditionalSaveData(ValueOutput o){super.addAdditionalSaveData(o);o.putInt("MaidenState",state());o.putInt("MaidenAge",(int)phaseAge(0));o.putInt("MaidenCycle",cycle());o.putInt("MaidenVariant",variant());o.putBoolean("MaidenBones",skeletonInside());o.putBoolean("MaidenLaunched",launched);o.putBoolean("MaidenClanged",clanged);o.putInt("MaidenLost",lostTicks);o.putLong("MaidenWakeDelay",wakeAt<0?-1:Math.max(0,wakeAt-level().getGameTime()));if(quarry!=null)o.putString("MaidenQuarry",quarry.toString());var c=entityData.get(CLOSE_FROM);o.store("MaidenCloseFrom",Vec3.CODEC,new Vec3(c.x(),c.y(),c.z()));}
 @Override protected void readAdditionalSaveData(ValueInput i){super.readAdditionalSaveData(i);setPersistenceRequired();entityData.set(STATE,Math.clamp(i.getIntOr("MaidenState",0),0,5));entityData.set(START,level().getGameTime()-Math.max(0,i.getIntOr("MaidenAge",0)));entityData.set(CYCLE,Math.clamp(i.getIntOr("MaidenCycle",MIN_CYCLE),MIN_CYCLE,MAX_CYCLE));entityData.set(VARIANT,Math.clamp(i.getIntOr("MaidenVariant",0),0,5));entityData.set(BONES,i.getBooleanOr("MaidenBones",false));launched=i.getBooleanOr("MaidenLaunched",false);clanged=i.getBooleanOr("MaidenClanged",false);lostTicks=i.getIntOr("MaidenLost",0);long delay=i.getLongOr("MaidenWakeDelay",-1);wakeAt=delay<0?-1:level().getGameTime()+Math.min(19,delay);try{quarry=UUID.fromString(i.getStringOr("MaidenQuarry",""));}catch(IllegalArgumentException e){quarry=null;}var c=i.read("MaidenCloseFrom",Vec3.CODEC).orElse(new Vec3(-145,145,.4));entityData.set(CLOSE_FROM,new Vector3f((float)c.x,(float)c.y,(float)c.z));}
}
