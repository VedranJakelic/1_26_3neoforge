package net.beamex.shamaschizm.saints.vindication;
import java.util.UUID;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;
/** One server-owned loose bone. Terrain collision only, never an attack target. */
public final class SaintBoneEntity extends Entity {
 private static final EntityDataAccessor<Integer> CORPSE=SynchedEntityData.defineId(SaintBoneEntity.class,EntityDataSerializers.INT);
 private static final EntityDataAccessor<Integer> PART=SynchedEntityData.defineId(SaintBoneEntity.class,EntityDataSerializers.INT);
 private static final EntityDataAccessor<Integer> ROLE=SynchedEntityData.defineId(SaintBoneEntity.class,EntityDataSerializers.INT);
 private static final EntityDataAccessor<Integer> BOSS=SynchedEntityData.defineId(SaintBoneEntity.class,EntityDataSerializers.INT);
 private static final EntityDataAccessor<Integer> SLOT=SynchedEntityData.defineId(SaintBoneEntity.class,EntityDataSerializers.INT);
 private static final EntityDataAccessor<org.joml.Vector3fc> ANGLES=SynchedEntityData.defineId(SaintBoneEntity.class,EntityDataSerializers.VECTOR3);
 private UUID encounter,bossId;private int physicsTicks;private boolean settled;
 public SaintBoneEntity(EntityType<? extends SaintBoneEntity> type,Level level){super(type,level);}
 @Override protected void defineSynchedData(SynchedEntityData.Builder b){b.define(CORPSE,-1);b.define(PART,0);b.define(ROLE,0);b.define(BOSS,-1);b.define(SLOT,1);b.define(ANGLES,new org.joml.Vector3f());}
 public void setCorpse(int id){entityData.set(CORPSE,id);}
 public int corpseId(){return entityData.get(CORPSE);}
 public int part(){return entityData.get(PART);}public int role(){return entityData.get(ROLE);}public int bossEntityId(){return entityData.get(BOSS);}public int slot(){return entityData.get(SLOT);}
 public org.joml.Vector3fc angles(){return entityData.get(ANGLES);}
 public boolean belongsTo(UUID id){return id.equals(encounter);}
 public void configure(UUID id,int role,int part,Vec3 at,float yaw){
  encounter=id;entityData.set(ROLE,role);entityData.set(PART,part);refreshDimensions();setPos(at);
  entityData.set(ANGLES,new org.joml.Vector3f(0,(float)Math.toRadians(-yaw),0));
  setDeltaMovement((random.nextDouble()-.5)*.18,.12+random.nextDouble()*.13,(random.nextDouble()-.5)*.18);
 }
 public void gather(VindicationEntity boss,int slot){bossId=boss.getUUID();entityData.set(BOSS,boss.getId());entityData.set(SLOT,slot);setDeltaMovement(Vec3.ZERO);noPhysics=true;}
 @Override public void tick(){
  super.tick();if(!(level() instanceof ServerLevel l))return;
  if(encounter!=null && l.getEntity(encounter) instanceof net.beamex.shamaschizm.saints.CatacombEncounterEntity c && c.isRewarded()){discard();return;}
  if(bossId!=null){
   if(l.getEntity(bossId) instanceof VindicationEntity boss){
    entityData.set(BOSS,boss.getId());if(!boss.isAlive()||boss.phaseAge()>=40)discard();
   }else if(encounter!=null && net.beamex.shamaschizm.experimental.BossGardenLocks.get(l).encounterBeaten(encounter))discard();
   return;
  }
  if(tickCount%10==0 && encounter!=null && l.getEntity(encounter) instanceof net.beamex.shamaschizm.saints.CatacombEncounterEntity controller){
   UUID id=controller.finalBossId();
   if(id!=null && l.getEntity(id) instanceof VindicationEntity boss){gather(boss,boss.slotForRole(role()));return;}
   if(controller.isRewarded()){discard();return;}
  }
  if(settled){
   if(tickCount%20!=0 || !l.noCollision(this,getBoundingBox().move(0,-.05,0)))return;
   settled=false;
  }
  Vec3 before=getDeltaMovement().add(0,-.04,0);move(MoverType.SELF,before);physicsTicks++;
  var a=angles();float speed=(float)Math.min(1,before.length()*4);
  entityData.set(ANGLES,new org.joml.Vector3f(a.x()+(.04F+part()*.004F)*speed,a.y()+.045F*speed,a.z()+.055F*speed));
  if(onGround()){
   setDeltaMovement(before.x*.58,Math.abs(before.y)>.09?-before.y*.22:0,before.z*.58);
   if(physicsTicks>12 && getDeltaMovement().lengthSqr()<.002){
    settled=true;setDeltaMovement(Vec3.ZERO);entityData.set(ANGLES,new org.joml.Vector3f((float)Math.PI/2,a.y(),0));
   }
  }else setDeltaMovement(before.x*.98,before.y*.98,before.z*.98);
  // Once settled, there is no ongoing physics work.
 }
 @Override public EntityDimensions getDimensions(Pose pose){return part()==0?EntityDimensions.fixed(.5F,.5F):EntityDimensions.fixed(.3F,.15F);}
 @Override public boolean isPickable(){return false;}
 @Override public boolean isPushable(){return false;}
 @Override public boolean canBeCollidedWith(Entity other){return false;}
 @Override public boolean canCollideWith(Entity other){return false;}
 @Override public void push(Entity other){}
 @Override public boolean hurtServer(ServerLevel l,DamageSource s,float amount){return false;}
 @Override protected void addAdditionalSaveData(ValueOutput o){
  if(encounter!=null)o.putString("Encounter",encounter.toString());if(bossId!=null)o.putString("Boss",bossId.toString());
  o.putInt("Part",part());o.putInt("Role",role());o.putInt("Slot",slot());o.putInt("PhysicsTicks",physicsTicks);o.putBoolean("Settled",settled);
  var a=angles();o.putFloat("RX",a.x());o.putFloat("RY",a.y());o.putFloat("RZ",a.z());
 }
 @Override protected void readAdditionalSaveData(ValueInput i){
  try{encounter=UUID.fromString(i.getStringOr("Encounter",""));}catch(IllegalArgumentException e){encounter=null;}
  try{bossId=UUID.fromString(i.getStringOr("Boss",""));}catch(IllegalArgumentException e){bossId=null;}
  entityData.set(PART,Math.max(0,Math.min(10,i.getIntOr("Part",0))));entityData.set(ROLE,Math.max(0,Math.min(3,i.getIntOr("Role",0))));entityData.set(SLOT,i.getIntOr("Slot",1));
  refreshDimensions();physicsTicks=i.getIntOr("PhysicsTicks",0);settled=i.getBooleanOr("Settled",false);noPhysics=bossId!=null;
  entityData.set(ANGLES,new org.joml.Vector3f(i.getFloatOr("RX",0),i.getFloatOr("RY",0),i.getFloatOr("RZ",0)));
 }
}
