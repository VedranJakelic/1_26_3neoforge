package net.beamex.shamaschizm.serpent;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Server-owned swimming, tethered tail and capture. No vanilla movement goals. */
public final class SeaSerpent extends PathfinderMob {
 public static final int MAIN_SEGMENTS=3, TAIL_SEGMENTS=16, LURE=1+MAIN_SEGMENTS+TAIL_SEGMENTS, NODES=LURE+1, WEAPONS=10;
 public static final double[] SCALE=new double[NODES];
 public static final double[] LENGTH=new double[NODES];
 private static final List<EntityDataAccessor<Vector3fc>> POINTS=new ArrayList<>();
 private static final List<EntityDataAccessor<ItemStack>> ITEMS=new ArrayList<>();
 private static final EntityDataAccessor<Integer> HELD=SynchedEntityData.defineId(SeaSerpent.class,EntityDataSerializers.INT);
 private static final EntityDataAccessor<Integer> BITE=SynchedEntityData.defineId(SeaSerpent.class,EntityDataSerializers.INT);
 private static final EntityDataAccessor<Boolean> CORPSE=SynchedEntityData.defineId(SeaSerpent.class,EntityDataSerializers.BOOLEAN);
 static {
  Arrays.fill(SCALE,1);SCALE[2]=1.10;for(int i=0;i<TAIL_SEGMENTS;i++)SCALE[1+MAIN_SEGMENTS+i]=.94-(.94-.22)*i/(TAIL_SEGMENTS-1.0);
  for(int i=0;i<NODES;i++){POINTS.add(SynchedEntityData.defineId(SeaSerpent.class,EntityDataSerializers.VECTOR3));if(i>0)LENGTH[i]=i==LURE?.35:2.05*(SCALE[i-1]+SCALE[i])*.5;}
  for(int i=0;i<WEAPONS;i++)ITEMS.add(SynchedEntityData.defineId(SeaSerpent.class,EntityDataSerializers.ITEM_STACK));
 }
 private final SerpentPart[] parts=new SerpentPart[NODES-1+WEAPONS];
 private final Vec3[] nodes=new Vec3[NODES];
 private final Vector3f[] previous=new Vector3f[NODES],current=new Vector3f[NODES];
 private final List<ItemStack> stomach=new ArrayList<>();
 private boolean initialized,clientReady;
 private int corpseTicks,attackCooldown,holdTicks,escapeScore,lastPress=-100,roamTicks,idleTicks=200,breachTicks;
 private Vec3 velocity=Vec3.ZERO,forward=new Vec3(0,0,-1),goal,anchor;
 private UUID victim;
 private Player prey;
 private ItemEntity food;
 private int foodBiteTick;
 private float clientBite,previousClientBite;
 private int lastServerBite;
 private Vec3 previousPreyPosition;
 private int previousPreyId=-1;
 private Vec3 breachGoal;
 private Vec3 diveGoal,diveWaypoint;
 private int diveSearchTicks;
 private int pursuitUntil;
 private Vec3 lastPreyWater;
 public SeaSerpent(EntityType<? extends PathfinderMob> type,Level level){
  super(type,level);setPersistenceRequired();setNoGravity(true);xpReward=150;
  for(int i=1;i<NODES;i++)parts[i-1]=new SerpentPart(this,i,(float)(i==LURE?.6:1.45*SCALE[i]),(float)(i==LURE?1.8:1.4*SCALE[i]));
  for(int i=0;i<WEAPONS;i++)parts[NODES-1+i]=new SerpentPart(this,NODES+i,.45F,.7F);
 }
 public static AttributeSupplier.Builder attributes(){return createMobAttributes().add(Attributes.MAX_HEALTH,160).add(Attributes.ATTACK_DAMAGE,12).add(Attributes.MOVEMENT_SPEED,0).add(Attributes.FOLLOW_RANGE,64).add(Attributes.KNOCKBACK_RESISTANCE,1);}
 @Override protected void registerGoals(){}
 @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(HELD,-1);b.define(BITE,0);b.define(CORPSE,false);for(var p:POINTS)b.define(p,new Vector3f());for(var p:ITEMS)b.define(p,ItemStack.EMPTY);}
 @Override public boolean isMultipartEntity(){return true;}
 @Override public SerpentPart[] getParts(){return parts;}
 @Override public void setId(int id){super.setId(id);if(parts!=null)for(int i=0;i<parts.length;i++)if(parts[i]!=null)parts[i].setId(id+i+1);}
 @Override public void travel(Vec3 input){}
 @Override public boolean isPushable(){return false;}
 @Override public void push(Entity other){}
 @Override public void push(double x,double y,double z){}
 @Override public boolean removeWhenFarAway(double d){return false;}
 @Override public void checkDespawn(){} // Persistent encounter, including peaceful-mode changes.
 @Override public boolean canBreatheUnderwater(){return true;}
 public boolean corpse(){return entityData.get(CORPSE);}
 public int heldId(){return entityData.get(HELD);}
 public float biteTime(float partial){return level().isClientSide()?(previousClientBite+(clientBite-previousClientBite)*partial)/20F:(entityData.get(BITE)+partial)/20F;}
 public ItemStack weapon(int i){return entityData.get(ITEMS.get(i));}
 public Vector3f point(int i,float partial){return clientReady?new Vector3f(previous[i]).lerp(current[i],partial):new Vector3f(entityData.get(POINTS.get(i)));}
 public Vec3 weaponPoint(int i){
  int segment=1+i;if(nodes[segment]==null)return position();
  Vec3 back=nodes[segment+1].subtract(nodes[segment-1]).normalize();
  Vec3 right=new Vec3(back.z,0,-back.x).normalize();if(right.lengthSqr()<1e-8)right=new Vec3(1,0,0);
  Vec3 up=back.cross(right).normalize();
  return nodes[segment].add(right.scale(((i%2)*2-1)*.28*SCALE[segment])).add(up.scale(.75*SCALE[segment]));
 }
 public boolean takeWeapon(Player player,int i){
  if(i<0||i>=WEAPONS||weapon(i).isEmpty()||player.isSpectator()||player.distanceToSqr(weaponPoint(i))>16)return false;
  if(level() instanceof ServerLevel){ItemStack stack=weapon(i).copy();if(!player.getInventory().add(stack))return false;entityData.set(ITEMS.get(i),ItemStack.EMPTY);playSound(SoundEvents.ITEM_PICKUP,.45F,1);}
  return true;
 }
 @Override public void tick(){
  super.tick();if(isRemoved())return;
  if(level() instanceof ServerLevel server){
   if(nodes[0]==null||nodes[0].distanceToSqr(position())>64)seed();
   if(!initialized){initialized=true;seedWeapons(server);}
   if(corpse())sink(server);else if(isAlive())swim(server);
   if(isRemoved())return;
   solveBody(server);publish();
   // Ordinary pickup range; weapons can also be right-clicked on their own small hitboxes.
   for(var p:server.players())if(p.isAlive()&&!p.isSpectator())for(int i=0;i<WEAPONS;i++)if(p.distanceToSqr(weaponPoint(i))<1.4*1.4)takeWeapon(p,i);
  }else{
   int serverBite=entityData.get(BITE);previousClientBite=clientBite;
   if(serverBite>0&&(lastServerBite==0||serverBite<lastServerBite-3)){
    clientBite=Math.min(serverBite,2);previousClientBite=Math.max(0,clientBite-1);
   }else if(clientBite>0){
    clientBite=Math.min(29,clientBite+1);
    if(heldId()>=0)clientBite=Math.min(25,clientBite);
    else if(clientBite>=29&&serverBite==0){clientBite=0;previousClientBite=0;}
   }
   lastServerBite=serverBite;
   for(int i=0;i<NODES;i++){Vector3f p=new Vector3f(entityData.get(POINTS.get(i)));previous[i]=clientReady?current[i]:p;current[i]=p;nodes[i]=position().add(p.x,p.y,p.z);}clientReady=true;
  }
  updateParts();
 }
 private void seed(){
  forward=Vec3.directionFromRotation(0,getYRot());nodes[0]=position();
  for(int i=1;i<NODES;i++)nodes[i]=nodes[i-1].subtract(forward.scale(LENGTH[i]));
  anchor=nodes[LURE];
 }
 private void seedWeapons(ServerLevel server){
  int count=4+random.nextInt(7);
  for(int i=0;i<count;i++){
   ItemStack stack=new ItemStack(random.nextBoolean()?Items.ARROW:switch(random.nextInt(3)){case 0->Items.IRON_SPEAR;case 1->Items.STONE_SPEAR;default->Items.COPPER_SPEAR;});
   if(stack.isDamageableItem()){
    stack.setDamageValue(random.nextInt(stack.getMaxDamage()));
    if(random.nextFloat()<.06F)stack=EnchantmentHelper.enchantItem(random,stack,30,server.registryAccess(),Optional.empty());
   }
   entityData.set(ITEMS.get(i),stack);
  }
 }
 private boolean water(BlockPos pos){return level().hasChunkAt(pos)&&level().getFluidState(pos).is(FluidTags.WATER);}
 /** Find a real water surface near a column, without touching unloaded chunks. */
 private double surface(Vec3 near){
  BlockPos p=BlockPos.containing(near);if(!level().hasChunkAt(p))return Double.NaN;
  if(!water(p)){for(int k=0;k<48&&p.getY()>level().getMinY();k++,p=p.below())if(water(p))break;}
  if(!water(p))return Double.NaN;
  for(int k=0;k<96&&water(p.above());k++)p=p.above();
  return !water(p.above())?p.getY()+1:Double.NaN;
 }
 private double bottom(Vec3 near){BlockPos p=BlockPos.containing(near);for(int i=0;i<64&&water(p.below());i++)p=p.below();return p.getY()+1.1;}
 private boolean targetable(Player p){return p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&distanceToSqr(p)<64*64;}
 private record Intercept(Vec3 point,int ticks){}
 private Intercept interception(Player p){
  if(p.isInWater()||p.getDeltaMovement().y>=-.08)return null;
  Vec3 projected=p.position();double vy=p.getDeltaMovement().y;
  for(int t=1;t<=48;t++){
   Vec3 next=projected.add(p.getDeltaMovement().x,vy,p.getDeltaMovement().z);
   if(!level().hasChunkAt(BlockPos.containing(next)))return null;
   if(water(BlockPos.containing(next))){
    // Keep a committed leap alive right through the last pre-splash ticks.
    if(p.getY()-next.y<2.5&&!(prey==p&&breachTicks>0))return null;
    double waterTop=surface(next);
    return new Intercept(new Vec3(next.x,waterTop+1.35,next.z),Math.max(1,t-1));
   }
   if(level().clip(new ClipContext(projected,next,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p)).getType()==HitResult.Type.BLOCK)return null;
   projected=next;vy=(vy-.08)*.98;
  }
  return null;
 }
 private void swim(ServerLevel server){
  if(attackCooldown>0)attackCooldown--;
  if(entityData.get(BITE)>0){int n=entityData.get(BITE)+1;entityData.set(BITE,heldId()>=0?Math.min(25,n):n>29?0:n);}
  if(--idleTicks<=0){playSound(Serpents.IDLE[random.nextInt(3)],.55F,.85F+random.nextFloat()*.15F);idleTicks=400+random.nextInt(600);}
  Player held=victim==null?null:server.getPlayerByUUID(victim);
  if(victim!=null&&(held==null||!targetable(held))){release();held=null;}
  if(held==null)updatePrey(server);
  double top=surface(position());if(Double.isNaN(top))top=getY()+3;
  if(held!=null||prey!=null){food=null;foodBiteTick=0;}
  if(held==null&&prey==null&&(food==null||!food.isAlive())&&tickCount%10==0){
   food=null;double best=144;
   for(var item:server.getEntitiesOfClass(ItemEntity.class,getBoundingBox().inflate(12),e->e.isAlive()&&!e.getItem().isEmpty())){
    double d=distanceToSqr(item);if(d<best&&hasLineOfSight(item)){best=d;food=item;}
   }
  }
  Vec3 desired;
  int interceptTicks=0;
  if(held!=null){
   if(diveGoal==null||--diveSearchTicks<=0)findDive(server);
   if(diveWaypoint!=null&&position().distanceToSqr(diveWaypoint)<2.25)diveWaypoint=null;
   desired=diveWaypoint!=null?diveWaypoint:diveGoal;
   if(position().distanceToSqr(desired)<2.25)desired=desired.add(Math.sin(tickCount*.32)*.55,.1*Math.sin(tickCount*.2),Math.cos(tickCount*.27)*.55);
   holdTicks++;if(tickCount%10==0)escapeScore=Math.max(0,escapeScore-1);
  }else if(prey!=null&&targetable(prey)){
   Intercept intercept=interception(prey);
   if(intercept!=null){breachGoal=intercept.point();desired=breachGoal.subtract(forward.scale(1.25)).add(0,-.35,0);interceptTicks=intercept.ticks();breachTicks=20;}
   else if(prey.isInWater()||nearWater(prey)){breachTicks=0;desired=prey.position().add(0,.4,0);}
   else if(breachTicks>0&&breachGoal!=null){desired=breachGoal.subtract(forward.scale(1.25)).add(0,-.35,0);interceptTicks=3;}
   else{breachTicks=0;desired=lastPreyWater!=null?lastPreyWater:position();}
  }else if(food!=null&&food.isAlive()){
   desired=food.position().add(food.getDeltaMovement().scale(4)).subtract(forward.scale(1.25)).add(0,-.35,0);
  }else{
   if(goal==null||--roamTicks<=0||position().distanceToSqr(goal)<4){
    double angle=random.nextDouble()*Math.PI*2;goal=position().add(Math.sin(angle)*10,0,Math.cos(angle)*10);
    double surface=surface(goal);goal=new Vec3(goal.x,Math.max(bottom(position()),(Double.isNaN(surface)?top:surface)-10-random.nextDouble()*3),goal.z);roamTicks=100+random.nextInt(100);
   }desired=goal;
  }
  boolean submerged=water(blockPosition());
  double speed=held!=null?.52:prey!=null?(breachTicks>0?1.6:.72):food!=null?.18:.11;
  if(interceptTicks>0){double deadline=Math.max(2,interceptTicks-2);speed=Math.min(2.8,Math.max(1.6,position().distanceTo(desired)/deadline));}
  if(foodBiteTick>0)speed=.18;
  Vec3 aim=desired.subtract(position());if(aim.lengthSqr()>1e-6)aim=aim.normalize();
  Vec3 side=new Vec3(-aim.z,0,aim.x).scale(Math.sin(tickCount*.14)*.08);
  Vec3 heading=velocity.lengthSqr()>.0001?velocity.normalize():forward;
  boolean pursuit=held==null&&prey!=null&&breachTicks==0;
  if(pursuit){
   speed=SerpentPursuit.speed(position().distanceTo(desired),heading.dot(aim));
   side=side.scale(Math.min(1,position().distanceTo(desired)/8));
  }
  Vec3 wanted=aim.scale(speed).add(side);
  // Brake while turning back after a pass; do not orbit forever at attack speed.
  Vec3 steered=turnToward(heading,wanted.normalize(),breachTicks>0?.30:.075);
  double acceleration=breachTicks>0?.45:pursuit?SerpentPursuit.acceleration(velocity.length(),wanted.length()):held!=null?.14:.07;
  velocity=steered.scale(velocity.length()+(wanted.length()-velocity.length())*acceleration);
  if(foodBiteTick>0&&food!=null&&food.isAlive()){
   Vec3 tracking=food.getDeltaMovement().add(food.position().subtract(mouth()).scale(.22));
   if(tracking.lengthSqr()>.24*.24)tracking=tracking.normalize().scale(.24);
   velocity=velocity.lerp(tracking,.45);
  }
  if(!submerged)velocity=velocity.add(0,-.055,0);if(breachTicks>0)breachTicks--;
  if(!server.hasChunkAt(BlockPos.containing(position().add(velocity))))velocity=Vec3.ZERO;
  if(submerged&&breachTicks==0&&!water(BlockPos.containing(position().add(velocity.scale(4))))) {velocity=velocity.scale(.5).add(0,-.04,0);goal=null;}
  Vec3 oldMouth=mouth();Vec3 old=position(),oldForward=forward;move(MoverType.SELF,velocity);Vec3 actual=position().subtract(old);
  if(actual.lengthSqr()>.00001&&foodBiteTick==0)forward=turnToward(forward,actual.normalize(),breachTicks>0?.30:.12);
  if(held!=null&&holdPosition(server,held)==null){
   // Keep the last safe head pose rather than pushing a captive into terrain.
   setPos(old);forward=oldForward;velocity=Vec3.ZERO;diveSearchTicks=Math.min(diveSearchTicks,10);
  }
  if(horizontalCollision){goal=null;velocity=new Vec3(-velocity.z,velocity.y,velocity.x);if(held!=null)diveSearchTicks=Math.min(diveSearchTicks,10);}
  setDeltaMovement(Vec3.ZERO);setAirSupply(getMaxAirSupply());
  if(held!=null)hold(server,held);
  else if(prey!=null&&targetable(prey)&&attackCooldown==0&&biteContact(prey,oldMouth,mouth())&&hasLineOfSight(prey)){
   playSound(SoundEvents.EVOKER_FANGS_ATTACK,1F,.8F);entityData.set(BITE,1);attackCooldown=30;
   if(prey.hurtServer(server,damageSources().mobAttack(this),12)&&prey.isAlive()){
    victim=prey.getUUID();entityData.set(HELD,prey.getId());diveGoal=null;diveWaypoint=null;diveSearchTicks=0;holdTicks=0;escapeScore=0;lastPress=tickCount-2;playSound(Serpents.GRAB,1.2F,1F);
   }
  }
  eat(server);
  if(prey!=null){previousPreyId=prey.getId();previousPreyPosition=prey.position();}else{previousPreyId=-1;previousPreyPosition=null;}
 }
 private boolean nearWater(Player p){
  BlockPos at=p.blockPosition();
  return water(at)||water(at.below())||water(at.below(2));
 }
 private void updatePrey(ServerLevel server){
  if(prey!=null&&!targetable(prey)){prey=null;pursuitUntil=0;lastPreyWater=null;breachTicks=0;breachGoal=null;}
  if(prey!=null){
   if(prey.isInWater()||nearWater(prey)||interception(prey)!=null){
    pursuitUntil=tickCount+300;lastPreyWater=prey.position().add(0,.4,0);return;
   }
   if(tickCount<=pursuitUntil)return;
   prey=null;breachTicks=0;breachGoal=null;lastPreyWater=null;
  }
  double nearest=64*64;
  for(var player:server.players())if(targetable(player)&&(player.isInWater()||nearWater(player)||interception(player)!=null)&&distanceToSqr(player)<nearest){
   prey=player;nearest=distanceToSqr(player);
  }
  if(prey!=null){pursuitUntil=tickCount+300;lastPreyWater=prey.position().add(0,.4,0);}
 }
 private boolean biteContact(Player player,Vec3 from,Vec3 to){
  if(player.distanceToSqr(to)<2.8*2.8)return true;
  Vec3 oldPlayer=previousPreyId==player.getId()&&previousPreyPosition!=null?previousPreyPosition:player.position().subtract(player.getDeltaMovement());
  AABB relative=player.getBoundingBox().move(player.position().scale(-1)).inflate(.8);
  return relative.clip(from.subtract(oldPlayer),to.subtract(player.position())).isPresent();
 }
 private Vec3 mouth(){return position().add(forward.scale(1.25)).add(0,.35,0);}
 private void hold(ServerLevel server,Player player){
  Vec3 target=holdPosition(server,player);
  player.setDeltaMovement(Vec3.ZERO);player.fallDistance=0;
  if(target!=null&&player instanceof ServerPlayer sp)sp.teleportTo(server,target.x,target.y,target.z,Set.of(),player.getYRot(),player.getXRot(),false);
  else{velocity=Vec3.ZERO;diveSearchTicks=Math.min(diveSearchTicks,10);}
  if(holdTicks%20==0)player.hurtServer(server,damageSources().mobAttack(this),2);
  if(tickCount%3==0){server.sendParticles(ParticleTypes.BUBBLE,mouth().x,mouth().y,mouth().z,8,.3,.25,.3,.03);server.sendParticles(ParticleTypes.BUBBLE,player.getX(),player.getEyeY(),player.getZ(),6,.18,.2,.18,.02);}
 }
 private Vec3 holdPosition(ServerLevel server,Player player){
  Vec3 mouthFeet=mouth().add(0,-.35,0);
  // Small adjustments keep the player beside the jaws without intersecting the seabed.
  for(double rise:new double[]{0,.4,.8,1.2})for(double back:new double[]{0,.35,.7}){
   Vec3 candidate=mouthFeet.add(0,rise,0).subtract(forward.scale(back));
   Vec3 move=candidate.subtract(player.position());
   if(move.lengthSqr()>36)continue;
   int steps=Math.max(1,(int)Math.ceil(move.length()/.3));boolean clear=true;
   for(int i=1;i<=steps;i++)if(!server.noCollision(player,player.getBoundingBox().move(move.scale(i/(double)steps)))){clear=false;break;}
   if(clear)return candidate;
  }
  return null;
 }
 private record DiveSpot(Vec3 bottom,Vec3 approach){}
 private void findDive(ServerLevel server){
  diveSearchTicks=40;
  Vec3 origin=position();List<DiveSpot> candidates=new ArrayList<>();
  // Four-block sampling grid within 24 blocks; only loaded water columns are examined.
  for(int dx=-24;dx<=24;dx+=4)for(int dz=-24;dz<=24;dz+=4){
   if(dx*dx+dz*dz>24*24)continue;
   Vec3 approach=origin.add(dx,0,dz);BlockPos p=BlockPos.containing(approach);
   if(!water(p))continue;
   int steps=0;while(steps++<128&&p.getY()>server.getMinY()+1&&water(p.below()))p=p.below();
   Vec3 deepest=new Vec3(approach.x,p.getY()+2.6,approach.z);
   if(deepest.y>origin.y+.5)continue;
   candidates.add(new DiveSpot(deepest,approach));
  }
  candidates.sort(Comparator.comparingDouble((DiveSpot c)->c.bottom.y).thenComparingDouble(c->c.bottom.distanceToSqr(origin)));
  for(DiveSpot candidate:candidates){
   if(clearSwim(server,origin,candidate.bottom)){
    diveGoal=candidate.bottom;diveWaypoint=null;return;
   }
   // Approach the deeper column horizontally before descending past a shelf.
   if(clearSwim(server,origin,candidate.approach)&&clearSwim(server,candidate.approach,candidate.bottom)){
    diveGoal=candidate.bottom;diveWaypoint=candidate.approach;return;
   }
  }
  // No safe deeper route: maintain the grip here and try again shortly.
  diveGoal=origin;diveWaypoint=null;diveSearchTicks=20;
 }
 private boolean clearSwim(ServerLevel server,Vec3 from,Vec3 to){
  Vec3 delta=to.subtract(from);int steps=Math.max(1,(int)Math.ceil(delta.length()/.75));
  for(int i=0;i<=steps;i++){
   Vec3 p=from.add(delta.scale(i/(double)steps));BlockPos block=BlockPos.containing(p);
   if(!server.hasChunksAt(block.offset(-1,-2,-1),block.offset(1,2,1))||!water(block)||!water(block.above()))return false;
   // Clearance for both head and captive, including below a diving mouth.
   if(!server.noCollision(new AABB(p.add(-.95,-1.35,-.95),p.add(.95,2.1,.95))))return false;
  }
  return true;
 }
 public void escapePress(Player player){
  if(corpse()||victim==null||!victim.equals(player.getUUID())||distanceToSqr(player)>36||tickCount-lastPress<2)return;
  lastPress=tickCount;if(++escapeScore>=12){release();player.setDeltaMovement(forward.scale(1.2).add(0,.35,0));}
 }
 private void release(){breachTicks=0;breachGoal=null;pursuitUntil=tickCount+300;diveGoal=null;diveWaypoint=null;diveSearchTicks=0;victim=null;entityData.set(HELD,-1);escapeScore=0;holdTicks=0;attackCooldown=100;}
 private void eat(ServerLevel server){
  if(victim!=null||prey!=null||food==null||!food.isAlive()){foodBiteTick=0;return;}
  double distance=food.position().distanceToSqr(mouth());
  if(foodBiteTick==0){
   if(distance>1.2*1.2||!hasLineOfSight(food)||entityData.get(BITE)>0)return;
   foodBiteTick=1;entityData.set(BITE,1);playSound(SoundEvents.EVOKER_FANGS_ATTACK,.65F,.85F);return;
  }
  foodBiteTick++;
  if(foodBiteTick>=10&&distance<1.6*1.6&&hasLineOfSight(food)){
   Vec3 pull=food.getDeltaMovement().scale(.65).add(mouth().subtract(food.position()).scale(.12));
   if(pull.lengthSqr()>.24*.24)pull=pull.normalize().scale(.24);food.setDeltaMovement(pull);
  }
  // Swallow only at the closing part of the authored bite, never at the body.
  if(foodBiteTick>=24&&foodBiteTick<=29&&distance<=1.35*1.35&&hasLineOfSight(food)){
   ItemStack remaining=food.getItem().copy();
   for(ItemStack saved:stomach)if(ItemStack.isSameItemSameComponents(saved,remaining)&&saved.getCount()<saved.getMaxStackSize()){int n=Math.min(saved.getMaxStackSize()-saved.getCount(),remaining.getCount());saved.grow(n);remaining.shrink(n);if(remaining.isEmpty())break;}
   if(!remaining.isEmpty())stomach.add(remaining);food.discard();food=null;
  }
  if(foodBiteTick>=29)foodBiteTick=0;
 }
 private static Vec3 turnToward(Vec3 from,Vec3 to,double maxAngle){
  if(to.lengthSqr()<1e-9)return from;
  double angle=Math.acos(Math.max(-1,Math.min(1,from.dot(to))));if(angle<=maxAngle)return to;
  Vec3 perpendicular=to.subtract(from.scale(from.dot(to)));
  if(perpendicular.lengthSqr()<1e-8)perpendicular=from.cross(Math.abs(from.y)<.9?new Vec3(0,1,0):new Vec3(1,0,0));
  return from.scale(Math.cos(maxAngle)).add(perpendicular.normalize().scale(Math.sin(maxAngle))).normalize();
 }
 private void solveBody(ServerLevel server){
  nodes[0]=position();
  if(anchor==null)anchor=nodes[LURE];
  if(!corpse()){
   double top=surface(nodes[LURE]);if(Double.isNaN(top))top=surface(position());
   Vec3 desired=nodes[LURE];
   if(!Double.isNaN(top))desired=new Vec3(desired.x,top-3.8,desired.z);
   // Slow surface lure; the submerged head pulls its horizontal position along.
   Vec3 behind=position().subtract(new Vec3(forward.x,0,forward.z).normalize().scale(7));
   desired=new Vec3(MthLerp(desired.x,behind.x,.025),desired.y,MthLerp(desired.z,behind.z,.025));
   anchor=anchor.lerp(desired,.035);
  }else {
   Vec3 candidate=nodes[LURE].add(0,-.025,0);
   anchor=server.noCollision(new AABB(candidate.add(-.2,0,-.2),candidate.add(.2,.4,.2)))?candidate:nodes[LURE];
  }
  for(int i=1;i<LURE;i++){
   Vec3 tangent=nodes[i].subtract(nodes[i-1]);Vec3 side=new Vec3(-tangent.z,0,tangent.x).normalize();
   Vec3 candidate=nodes[i].add(side.scale(corpse()?0:Math.sin(tickCount*.14-i*.65)*.025));
   if(corpse())candidate=candidate.add(0,-.025,0);
   if(server.noCollision(new AABB(candidate.add(-.15,-.15,-.15),candidate.add(.15,.15,.15))))nodes[i]=candidate;
  }
  // Bidirectional length constraints keep the tapered chain connected to both endpoints.
  for(int pass=0;pass<12;pass++){
   nodes[LURE]=anchor;
   for(int i=LURE-1;i>=0;i--)nodes[i]=constrain(nodes[i],nodes[i+1],LENGTH[i+1]);
   nodes[0]=position();
   for(int i=1;i<NODES;i++){
    Vec3 direction=nodes[i].subtract(nodes[i-1]).normalize();
    if(i==1&&!corpse())direction=forward.scale(-1);
    if(i>1)direction=turnToward(nodes[i-1].subtract(nodes[i-2]).normalize(),direction,.32);
    nodes[i]=nodes[i-1].add(direction.scale(LENGTH[i]));
   }
  }
  anchor=nodes[LURE];
 }
 private static double MthLerp(double a,double b,double t){return a+(b-a)*t;}
 private static Vec3 constrain(Vec3 p,Vec3 fixed,double length){Vec3 d=p.subtract(fixed);return fixed.add(d.lengthSqr()<1e-10?new Vec3(0,0,length):d.normalize().scale(length));}
 private void publish(){for(int i=0;i<NODES;i++){Vec3 d=nodes[i].subtract(position());entityData.set(POINTS.get(i),new Vector3f((float)d.x,(float)d.y,(float)d.z));}}
 private void updateParts(){
  for(int i=1;i<NODES;i++)if(nodes[i]!=null){parts[i-1].setOldPosAndRot();Vec3 p=nodes[i];parts[i-1].setPos(p.x,p.y-(i==LURE?0:.65*SCALE[i]),p.z);}
  for(int i=0;i<WEAPONS;i++){Vec3 p=weaponPoint(i);parts[NODES-1+i].setOldPosAndRot();parts[NODES-1+i].setPos(p);}
 }
 @Override public void die(DamageSource source){if(corpse())return;entityData.set(CORPSE,true);release();super.die(source);if(level() instanceof ServerLevel server&&!stomach.isEmpty()){for(ItemStack item:stomach)spawnAtLocation(server,item.copy());stomach.clear();}}
 @Override protected void dropCustomDeathLoot(ServerLevel server,DamageSource source,boolean hit){super.dropCustomDeathLoot(server,source,hit);for(ItemStack s:stomach)spawnAtLocation(server,s.copy());stomach.clear();}
 @Override protected void tickDeath(){deathTime=0;}
 private void sink(ServerLevel server){
  corpseTicks++;move(MoverType.SELF,new Vec3(0,-.025,0));
  if(corpseTicks>=2400){for(int i=0;i<WEAPONS;i++)if(!weapon(i).isEmpty()){spawnAtLocation(server,weapon(i).copy());entityData.set(ITEMS.get(i),ItemStack.EMPTY);}spawnAtLocation(server,new ItemStack(Items.ROTTEN_FLESH,5+random.nextInt(11)));discard();}
 }
 @Override protected void addAdditionalSaveData(ValueOutput out){
  super.addAdditionalSaveData(out);out.putBoolean("SerpentInitialized",initialized);out.putInt("SerpentCorpseTicks",corpseTicks);out.putBoolean("SerpentCorpse",corpse());
  out.store("SerpentStomach",ItemStack.CODEC.listOf(),stomach);
  out.store("SerpentWeapons",ItemStack.OPTIONAL_CODEC.listOf(),ITEMS.stream().map(entityData::get).toList());
  if(nodes[0]!=null)out.store("SerpentNodes",Vec3.CODEC.listOf(),Arrays.asList(nodes));
 }
 @Override protected void readAdditionalSaveData(ValueInput in){
  super.readAdditionalSaveData(in);initialized=in.getBooleanOr("SerpentInitialized",false);corpseTicks=in.getIntOr("SerpentCorpseTicks",0);entityData.set(CORPSE,in.getBooleanOr("SerpentCorpse",false));
  stomach.clear();stomach.addAll(in.read("SerpentStomach",ItemStack.CODEC.listOf()).orElse(List.of()));
  var items=in.read("SerpentWeapons",ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of());for(int i=0;i<Math.min(WEAPONS,items.size());i++)entityData.set(ITEMS.get(i),items.get(i));
  var points=in.read("SerpentNodes",Vec3.CODEC.listOf()).orElse(List.of());if(points.size()>=2){forward=points.get(0).subtract(points.get(1)).normalize();setYRot((float)Math.toDegrees(Math.atan2(-forward.x,forward.z)));}if(points.size()==NODES){for(int i=0;i<NODES;i++)nodes[i]=points.get(i);anchor=nodes[LURE];}
  setPersistenceRequired();setNoGravity(true);release();
 }
}
