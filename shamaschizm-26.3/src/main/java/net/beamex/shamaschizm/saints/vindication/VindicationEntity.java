package net.beamex.shamaschizm.saints.vindication;
import java.util.*;
import net.beamex.shamaschizm.saints.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.tags.ItemTags;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import org.joml.Vector3f;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
public final class VindicationEntity extends Mob {
 private static final EntityDataAccessor<Integer> AGE=SynchedEntityData.defineId(VindicationEntity.class,EntityDataSerializers.INT);
 private static final EntityDataAccessor<Integer> MAIN_SAINT=SynchedEntityData.defineId(VindicationEntity.class,EntityDataSerializers.INT);
 private static final EntityDataAccessor<Integer> OUTER_FLASH=SynchedEntityData.defineId(VindicationEntity.class,EntityDataSerializers.INT);
 private static final EntityDataAccessor<Integer> BONE_FLASH=SynchedEntityData.defineId(VindicationEntity.class,EntityDataSerializers.INT);
 private static final EntityDataAccessor<Integer> SPELL_FLASH=SynchedEntityData.defineId(VindicationEntity.class,EntityDataSerializers.INT);
 private static final EntityDataAccessor<Boolean> CHARGING=SynchedEntityData.defineId(VindicationEntity.class,EntityDataSerializers.BOOLEAN);
 private static final EntityDataAccessor<Integer> BEAM_TICKS=SynchedEntityData.defineId(VindicationEntity.class,EntityDataSerializers.INT);
 private static final List<EntityDataAccessor<org.joml.Vector3fc>> BEAMS=List.of(
  SynchedEntityData.defineId(VindicationEntity.class,EntityDataSerializers.VECTOR3),
  SynchedEntityData.defineId(VindicationEntity.class,EntityDataSerializers.VECTOR3),
  SynchedEntityData.defineId(VindicationEntity.class,EntityDataSerializers.VECTOR3));
 private int nextBlaze,nextFire;private long lastRingSound=-100;private boolean deathBurstHandled,introPlayed;
 private static final float BLAZE_PITCH=(float)Math.pow(2,-7.0/12.0);
 private final ServerBossEvent bar=new ServerBossEvent(UUID.randomUUID(),Component.literal("Vindication of the Templars"),BossEvent.BossBarColor.BLUE,BossEvent.BossBarOverlay.PROGRESS);
 private UUID encounter;private double baseY,anchorX,anchorZ;private boolean configured;
 public VindicationEntity(EntityType<? extends VindicationEntity> type,Level level){super(type,level);setNoGravity(true);noPhysics=true;setPersistenceRequired();setNoAi(true);xpReward=500;}
 @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(AGE,0);b.define(MAIN_SAINT,3);b.define(OUTER_FLASH,0);b.define(BONE_FLASH,0);b.define(SPELL_FLASH,0);b.define(CHARGING,false);b.define(BEAM_TICKS,0);for(var key:BEAMS)b.define(key,new Vector3f());}
 public void beginGroundedPrelude(){entityData.set(AGE,-40);setPermanentlyInvulnerable(true);setGlowingTag(false);}
 public int phaseAge(){return entityData.get(AGE);}
 private final VindicationAnimationClock animationClock=new VindicationAnimationClock();
 /** Rendering only; combat and ring collision continue using the authoritative age. */
 public float visualPhaseAge(float partial){
  if(!level().isClientSide())return phaseAge()+partial;
  float age=animationClock.value(phaseAge(),partial);
  // Never reveal the completed form before the server has finished the rise.
  return phaseAge()<40?Math.min(age,39.999F):age;
 }
 @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){
  super.onSyncedDataUpdated(key);
  if(AGE.equals(key)&&level().isClientSide()&&animationClock!=null)
   animationClock.sample(phaseAge(),tickCount);
 }
 public boolean isCharging(){return entityData.get(CHARGING);}
 public int beamTicks(){return entityData.get(BEAM_TICKS);}
 public Vec3 beam(int i){org.joml.Vector3fc v=entityData.get(BEAMS.get(i));return new Vec3(v.x(),v.y(),v.z());}
 public float chargeProgress(){return beamTicks()>0?1:Math.max(0,Math.min(1,((phaseAge()-40)%60-40)/20F));}
 @Override protected SoundEvent getHurtSound(DamageSource source){return SoundEvents.BLAZE_DEATH;}
 @Override protected SoundEvent getDeathSound(){return SoundEvents.ENDER_DRAGON_DEATH;}
 @Override public float getVoicePitch(){return 1;}
 @Override public void playSound(SoundEvent sound,float volume,float pitch){
  super.playSound(sound,volume,pitch);
  if(!isSilent() && level() instanceof ServerLevel server)
   net.beamex.shamaschizm.saints.audio.VindicationEcho.send(server,sound,center(),volume,pitch);
 }
 public void setMainSaint(int role){entityData.set(MAIN_SAINT,Math.max(0,Math.min(3,role)));}
 public int mainSaint(){return entityData.get(MAIN_SAINT);}
 public int roleForSlot(int slot){if(slot==0)return mainSaint();int index=1;for(int role=0;role<4;role++)if(role!=mainSaint()){if(index++==slot)return role;}return mainSaint();}
 public int slotForRole(int role){for(int slot=0;slot<4;slot++)if(roleForSlot(slot)==role)return slot;return 1;}
 public boolean outerRingFlashing(){return entityData.get(OUTER_FLASH)>0;}
 public boolean boneRingFlashing(){return entityData.get(BONE_FLASH)>0;}
 public boolean spellRingFlashing(){return entityData.get(SPELL_FLASH)>0;}
 public void ringHit(Vec3 impact){
  if(!(level() instanceof ServerLevel))return;
  int ring=RingGeometry.ringAt(impact.subtract(center()),phaseAge()-40);
  if(ring==0)entityData.set(BONE_FLASH,6);
  else if(ring==1)entityData.set(SPELL_FLASH,6);
  else if(ring==2)entityData.set(OUTER_FLASH,6);
  if(level().getGameTime()-lastRingSound>=4){lastRingSound=level().getGameTime();playSound(SoundEvents.BLAZE_HURT,1,1);}
 }
 public Vec3 center(){return position().add(0,.5,0);}
 public void configure(UUID id,Vec3 ground){encounter=id;anchorX=ground.x;anchorZ=ground.z;baseY=ground.y+.5;configured=true;setPos(anchorX,baseY,anchorZ);setHealth(33);}
 @Override public boolean removeWhenFarAway(double d){return false;}
 @Override public boolean isPushable(){return false;}
 @Override public void push(Entity e){}
 @Override public void startSeenByPlayer(ServerPlayer p){super.startSeenByPlayer(p);bar.addPlayer(p);}
 @Override public void stopSeenByPlayer(ServerPlayer p){super.stopSeenByPlayer(p);bar.removePlayer(p);}
 @Override public void tick(){
  super.tick();setDeltaMovement(Vec3.ZERO);
  if(level().isClientSide())animationClock.tick(phaseAge(),tickCount);
  if(!(level() instanceof ServerLevel level)||!isAlive())return;
  if(!configured){baseY=getY();anchorX=getX();anchorZ=getZ();configured=true;}
  if(entityData.get(OUTER_FLASH)>0)entityData.set(OUTER_FLASH,entityData.get(OUTER_FLASH)-1);
  if(entityData.get(BONE_FLASH)>0)entityData.set(BONE_FLASH,entityData.get(BONE_FLASH)-1);
  if(entityData.get(SPELL_FLASH)>0)entityData.set(SPELL_FLASH,entityData.get(SPELL_FLASH)-1);
  int age=phaseAge()+1;entityData.set(AGE,age);
  if(!introPlayed && age>0){introPlayed=true;playSound(SoundEvents.ALLAY_DEATH,4,1);}
  setPermanentlyInvulnerable(age<40);
  if(age>0 && age>=nextBlaze){playSound(SoundEvents.BLAZE_AMBIENT,.9F,BLAZE_PITCH);nextBlaze=age+100+level.getRandom().nextInt(61);}
  if(age>0 && age>=nextFire){playSound(SoundEvents.FIRE_AMBIENT,.65F,1);nextFire=age+12+level.getRandom().nextInt(13);}
  if(beamTicks()>0)entityData.set(BEAM_TICKS,beamTicks()-1);
  boolean charging=age>40 && (age-40)%60>=40 && level.players().stream().anyMatch(p->p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&distanceToSqr(p)<48*48);
  entityData.set(CHARGING,charging);if(charging)drawFlames(level);
  if(age<=40)setPos(getX(),baseY+3.0*Math.max(0,Math.min(1,age/40.0)),getZ());
  else {
   // Fixed altitude: horizontal orbit only, about half a block per second.
   double angle=(age-40)*.006;double radius=Math.min(4,(age-40)*.025);
   Vec3 goal=new Vec3(anchorX+Math.cos(angle)*radius,baseY+3,anchorZ+Math.sin(angle)*radius);
   if(level.noCollision(this,getBoundingBox().move(goal.subtract(position()))))setPos(goal.x,goal.y,goal.z);
   if(age%60==40)fireVolley(level);
  }
  bar.setProgress(getHealth()/33F);
  if(age>=40){level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,getX(),getY()+.5,getZ(),8,.24,.24,.24,.008);}
 }
 @Override public boolean hurtServer(ServerLevel level,DamageSource source,float damage){
  if(phaseAge()<40)return false;
  Entity direct=source.getDirectEntity();
  if(direct instanceof Player player){
   var ring=VindicationMelee.trace(player);
   if(ring!=null){ring.boss().ringHit(ring.point());return false;}
  }
  ItemStack weapon=ItemStack.EMPTY;
  if(direct instanceof AbstractArrow arrow)weapon=arrow.getWeaponItem();
  else if(direct instanceof LivingEntity living)weapon=living.getMainHandItem();
  if(weapon!=null && (weapon.is(ItemTags.SPEARS)||weapon.is(VindicationRegistration.SPEAR)))damage*=3;
  boolean result=super.hurtServer(level,source,damage);bar.setProgress(getHealth()/33F);return result;
 }
 private void fireVolley(ServerLevel level){
  List<ServerPlayer> players=level.players().stream().filter(p->p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&distanceToSqr(p)<48*48).sorted(Comparator.comparingDouble(this::distanceToSqr)).toList();
  if(players.isEmpty())return;
  Vec3 main=players.get(0).position().add(0,.6,0);List<Vec3> aims=new ArrayList<>();
  for(int i=0;i<3;i++){
   Vec3 aim=i<players.size()?players.get(i).position().add(0,.6,0):main.add(i==1?3:-3,-.5,i==1?1:-1);
   for(Vec3 prior:aims)if(aim.distanceToSqr(prior)<4)aim=aim.add(i==1?3:-3,0,2);
   aims.add(aim);Vec3 beam=laser(level,aim).subtract(center());entityData.set(BEAMS.get(i),new Vector3f((float)beam.x,(float)beam.y,(float)beam.z));
  }
  entityData.set(BEAM_TICKS,10);
 }
 private Vec3 laser(ServerLevel level,Vec3 aim){
  Vec3 from=center(),end=from.add(aim.subtract(from).normalize().scale(48));
  BlockHitResult hit=level.clip(new ClipContext(from,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
  Vec3 to=hit.getType()==HitResult.Type.MISS?end:hit.getLocation();
  for(Player p:level.getEntitiesOfClass(Player.class,new AABB(from,to).inflate(.3),p->p.isAlive()&&!p.isCreative()&&!p.isSpectator())){
   var box=p.getBoundingBox().inflate(.18);if(box.contains(from)||box.clip(from,to).isPresent())p.hurtServer(level,damageSources().indirectMagic(this,this),6);
  }
  if(hit.getType()==HitResult.Type.BLOCK){
   // Place fire on the impacted block's top if free, otherwise on the hit face.
   BlockPos pos=hit.getBlockPos().above();if(!level.getBlockState(pos).isAir())pos=hit.getBlockPos().relative(hit.getDirection());
   var fire=VindicationRegistration.SOUL_FIRE.defaultBlockState();
   if(level.getBlockState(pos).isAir()&&fire.canSurvive(level,pos))level.setBlock(pos,fire,3);
  }
  return to;
 }
 private void drawFlames(ServerLevel level){
  for(int i=0;i<8;i++){
   Vec3 normal=new Vec3(level.getRandom().nextDouble()*2-1,level.getRandom().nextDouble()*2-1,level.getRandom().nextDouble()*2-1).normalize();
   Vec3 from=center().add(normal.scale(1.4+level.getRandom().nextDouble())),motion=center().subtract(from).scale(.13);
   level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,from.x,from.y,from.z,0,motion.x,motion.y,motion.z,1);
  }
 }
 @Override public void die(DamageSource source){
  if(deathBurstHandled)return;deathBurstHandled=true;
  if(level() instanceof ServerLevel level){
   // Burst before XP and coffin loot appear, so the rewards cannot be caught in the blast.
   level.explode(this,getX(),getY()+.5,getZ(),3.0F,false,Level.ExplosionInteraction.NONE);
   level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,getX(),getY()+.5,getZ(),140,1,1,1,.1);
   level.sendParticles(ParticleTypes.LARGE_SMOKE,getX(),getY()+.5,getZ(),100,1,1,1,.08);
  }
  entityData.set(CHARGING,false);entityData.set(BEAM_TICKS,0);
  super.die(source);bar.removeAllPlayers();
  if(level() instanceof ServerLevel level && encounter!=null){
   net.beamex.shamaschizm.saints.audio.ArenaAudio.send(level,encounter,position(),false,false);
   SaintsDeathData.get(level).recordVindicationDefeated(encounter);
   if(level.getEntity(encounter) instanceof CatacombEncounterEntity controller)controller.vindicationDefeated();
  }
 }
 @Override protected void addAdditionalSaveData(ValueOutput o){super.addAdditionalSaveData(o);o.putInt("MainSaint",mainSaint());o.putBoolean("DeathBurstHandled",deathBurstHandled);o.putBoolean("IntroPlayed",introPlayed);if(encounter!=null)o.putString("Encounter",encounter.toString());o.putInt("RiseAge",phaseAge());o.putDouble("BaseY",baseY);o.putDouble("AnchorX",anchorX);o.putDouble("AnchorZ",anchorZ);o.putBoolean("Configured",configured);}
 @Override protected void readAdditionalSaveData(ValueInput i){super.readAdditionalSaveData(i);setMainSaint(i.getIntOr("MainSaint",3));deathBurstHandled=i.getBooleanOr("DeathBurstHandled",false);try{encounter=UUID.fromString(i.getStringOr("Encounter",""));}catch(IllegalArgumentException e){encounter=null;}entityData.set(AGE,i.getIntOr("RiseAge",0));setGlowingTag(false);baseY=i.getDoubleOr("BaseY",getY());anchorX=i.getDoubleOr("AnchorX",getX());anchorZ=i.getDoubleOr("AnchorZ",getZ());configured=i.getBooleanOr("Configured",false);introPlayed=i.getBooleanOr("IntroPlayed",phaseAge()>0);}
}
