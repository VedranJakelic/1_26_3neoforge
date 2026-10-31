package net.beamex.shamaschizm.maiden;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector3fc;
/** Server-owned loose rigid pieces, replicated by the existing living-entity portal renderer. */
public final class MaidenBone extends PathfinderMob {
 private static final EntityDataAccessor<Integer> PART=SynchedEntityData.defineId(MaidenBone.class,EntityDataSerializers.INT);
 private static final EntityDataAccessor<Vector3fc> ANGLES=SynchedEntityData.defineId(MaidenBone.class,EntityDataSerializers.VECTOR3);
 private Vec3 spin=Vec3.ZERO;private int remaining=100;private boolean settled;
 public MaidenBone(EntityType<? extends PathfinderMob> type,Level l){super(type,l);setSilent(true);setPersistenceRequired();}
 public static AttributeSupplier.Builder attributes(){return createMobAttributes().add(Attributes.MAX_HEALTH,1).add(Attributes.MOVEMENT_SPEED,0);}
 @Override protected void registerGoals(){}
 @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(PART,0);b.define(ANGLES,new Vector3f());}
 public int part(){return entityData.get(PART);}public Vector3fc angles(){return entityData.get(ANGLES);}
 public void configure(int part,Vec3 center,float yaw){entityData.set(PART,part);refreshDimensions();setPos(center.add(0,-getBbHeight()/2,0));entityData.set(ANGLES,new Vector3f(0,(float)Math.toRadians(180-yaw),0));setDeltaMovement(new Vec3((random.nextDouble()-.5)*.16,.03+random.nextDouble()*.1,-.06-random.nextDouble()*.13).yRot((float)Math.toRadians(180-yaw)));spin=new Vec3((random.nextDouble()-.5)*.22,(random.nextDouble()-.5)*.18,(random.nextDouble()-.5)*.22);remaining=80+random.nextInt(41);}
 @Override protected EntityDimensions getDefaultDimensions(Pose pose){var b=MaidenBones.PARTS[part()];float width=Math.max(b[3]-b[0],b[4]-b[1])/16;float height=(b[5]-b[2])/16;return EntityDimensions.fixed(width,height);}
 @Override public void travel(Vec3 input){}
 @Override public void tick(){
  super.tick();if(!(level() instanceof ServerLevel))return;if(--remaining<=0){discard();return;}
  if(settled&&level().noCollision(this,getBoundingBox().move(0,-.04,0)))settled=false;if(settled)return;
  Vec3 v=getDeltaMovement().add(0,-.04,0);move(MoverType.SELF,v);var a=angles();entityData.set(ANGLES,new Vector3f(a.x()+(float)spin.x,a.y()+(float)spin.y,a.z()+(float)spin.z));
  if(onGround()){v=new Vec3(v.x*.55,Math.abs(v.y)>.08?-v.y*.22:0,v.z*.55);spin=spin.scale(.55);if(v.lengthSqr()<.001){settled=true;v=Vec3.ZERO;entityData.set(ANGLES,new Vector3f((float)Math.PI/2,a.y(),0));}}
  else v=new Vec3(horizontalCollision?-v.x*.2:v.x*.98,verticalCollision?Math.max(0,-v.y*.15):v.y*.98,horizontalCollision?-v.z*.2:v.z*.98);
  setDeltaMovement(v);
 }
 @Override public boolean isPickable(){return false;}
 @Override public boolean isPushable(){return false;}
 @Override public boolean canBeCollidedWith(Entity e){return false;}
 @Override public boolean canCollideWith(Entity e){return false;}
 @Override public void push(Entity e){}
 @Override public void checkDespawn(){}
 @Override public boolean hurtServer(ServerLevel l,DamageSource s,float a){return false;}
 @Override public boolean causeFallDamage(double d,float m,DamageSource s){return false;}
 @Override protected void addAdditionalSaveData(ValueOutput o){super.addAdditionalSaveData(o);o.putInt("BonePart",part());o.putInt("Remaining",remaining);o.store("Spin",Vec3.CODEC,spin);var a=angles();o.store("Angles",Vec3.CODEC,new Vec3(a.x(),a.y(),a.z()));o.putBoolean("Settled",settled);}
 @Override protected void readAdditionalSaveData(ValueInput i){super.readAdditionalSaveData(i);entityData.set(PART,Math.clamp(i.getIntOr("BonePart",0),0,10));refreshDimensions();remaining=Math.clamp(i.getIntOr("Remaining",100),1,120);spin=i.read("Spin",Vec3.CODEC).orElse(Vec3.ZERO);var a=i.read("Angles",Vec3.CODEC).orElse(Vec3.ZERO);entityData.set(ANGLES,new Vector3f((float)a.x,(float)a.y,(float)a.z));settled=i.getBooleanOr("Settled",false);}
}
