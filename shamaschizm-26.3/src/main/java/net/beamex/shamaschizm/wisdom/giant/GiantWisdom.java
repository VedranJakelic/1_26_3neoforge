package net.beamex.shamaschizm.wisdom.giant;

import java.util.*;
import net.beamex.shamaschizm.wisdom.*;
import net.beamex.shamaschizm.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.*;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** A stationary-rooted encounter. The brain moves; the last rope node never does. */
public final class GiantWisdom extends TaintedWisdom {
    public static final double TETHER = 6, QUAKE_RADIUS = 8, DEATH_RADIUS = 24;
    private static final EntityDataAccessor<Integer> WORM_SEED = SynchedEntityData.defineId(GiantWisdom.class, EntityDataSerializers.INT);
    private BlockPos anchorBlock;
    private Vec3 anchor, destination;
    private int quakeCooldown, warning, summonCooldown, angryTicks, wander;
    private boolean lightningDone;
    private WisdomAttachments.Anchor[] giantAnchors;
    private int cachedSeed;
    private final List<UUID> brood = new ArrayList<>();
    public GiantWisdom(EntityType<? extends Monster> type, Level level) {
        super(type,level);setPersistenceRequired();xpReward=200;
        if(!level.isClientSide())entityData.set(WORM_SEED,random.nextInt());
    }
    public static AttributeSupplier.Builder giantAttributes(){return TaintedWisdom.attributes()
        .add(Attributes.MAX_HEALTH,50).add(Attributes.KNOCKBACK_RESISTANCE,1);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(WORM_SEED,0);}
    public void tether(BlockPos block, Vec3 surface){anchorBlock=block.immutable();anchor=surface;destination=null;}
    public Vec3 root(){return anchor==null?position():anchor;}
    @Override public boolean removeWhenFarAway(double d){return false;}
    @Override public void checkDespawn(){} // Also survives peaceful; this is a placed encounter.
    @Override public boolean canUsePortal(boolean passenger){return false;}
    @Override public boolean teleportTo(ServerLevel level,double x,double y,double z,Set<Relative> flags,float yaw,float pitch,boolean camera){
        if(anchor!=null)return false;return super.teleportTo(level,x,y,z,flags,yaw,pitch,camera);
    }
    @Override public boolean isPushable(){return false;}
    @Override public void push(Entity e){}
    @Override public WisdomAttachments.Anchor[] anchors(){
        int seed=entityData.get(WORM_SEED);
        if(giantAnchors==null||seed!=cachedSeed){
            cachedSeed=seed;int count=WisdomAttachments.count(seed)*5;
            giantAnchors=new WisdomAttachments.Anchor[count];
            for(int i=0;i<count;i++){
                var group=WisdomAttachments.create(seed+(i/5)*7919);
                var a=group[i%group.length];
                giantAnchors[i]=new WisdomAttachments.Anchor(a.point().scale(3),a.normal(),a.phase()+i*.7,a.length());
            }
        }
        return giantAnchors;
    }
    @Override protected boolean tickVariant(ServerLevel level){
        if(anchor==null){
            // /summon and dispensers have no clicked face: use the nearest support below.
            BlockPos p=blockPosition();for(int n=0;n<12 && level.getBlockState(p).isAir();n++)p=p.below();
            tether(p,Vec3.atBottomCenterOf(p.above()));
        }
        setNoGravity(true);getNavigation().stop();setTarget(null);
        if(isNoAi()){updateRope();return true;}
        if(angryTicks>0)angryTicks--;
        if(quakeCooldown>0)quakeCooldown--;
        if(summonCooldown>0)summonCooldown--;
        if(--wander<=0||destination==null||destination.distanceToSqr(position())<.3){
            wander=80+random.nextInt(80);
            destination=anchor.add((random.nextDouble()-.5)*7,2+random.nextDouble()*2,(random.nextDouble()-.5)*7);
            Vec3 d=destination.subtract(anchor);if(d.length()>TETHER)destination=anchor.add(d.normalize().scale(TETHER));
        }
        Vec3 d=destination.subtract(position());
        Vec3 velocity=getDeltaMovement().scale(.85).add(d.normalize().scale(.007));
        Vec3 next=position().add(velocity), radial=next.add(0,.1875,0).subtract(anchor);
        if(radial.length()>TETHER)velocity=anchor.add(radial.normalize().scale(TETHER)).add(0,-.1875,0).subtract(position());
        // Reject portal-sized jumps instead of drawing a rope across distant dungeon rooms.
        if(velocity.length()>1){setPos(anchor.x,anchor.y+2,anchor.z);velocity=Vec3.ZERO;}
        setDeltaMovement(velocity);move(MoverType.SELF,velocity);
        if(horizontalCollision||verticalCollision){wander=0;setDeltaMovement(Vec3.ZERO);}
        if(velocity.horizontalDistanceSqr()>1e-6){setYRot(net.minecraft.util.Mth.rotLerp(.035F,getYRot(),(float)Math.toDegrees(Math.atan2(-velocity.x,velocity.z))));yBodyRot=getYRot();yHeadRot=getYRot();}
        updateRope();
        // Player movement packets need not leave a nonzero server velocity or xo delta.
        // Trigger by presence near the tether OR the moving body, including visual feedback in creative.
        var players=level.players().stream().filter(p->quakeAudience(p)&&
            (p.distanceToSqr(this)<=QUAKE_RADIUS*QUAKE_RADIUS||p.position().distanceToSqr(root())<=QUAKE_RADIUS*QUAKE_RADIUS)).toList();
        if(warning>0){if(--warning==0){quake(level,players,true);quakeCooldown=40;}}
        else if(quakeCooldown==0&&!players.isEmpty()){
            warning=10;angryTicks=100;
            for(var p:players)GiantWisdomNetwork.send(p,.25F,0,true);
        }
        if(angryTicks>0&&tickCount%60==0)playSound(SoundEvents.GHAST_WARN,1.6F,.7F);
        return true;
    }
    private void updateRope(){
        Vec3 start=position().add(0,.1875,0), end=root();
        double sag=Math.max(0,(TETHER-start.distanceTo(end))*.3);
        for(int i=0;i<StingerRope.NODES;i++){
            double t=i/(double)(StingerRope.NODES-1);
            Vec3 p=start.lerp(end,t).add(0,-Math.sin(Math.PI*t)*sag,0);
            setStingerPoint(i,p.subtract(position()));
        }
    }
    public static boolean eligible(ServerPlayer p){return p.isAlive()&&!p.isCreative()&&!p.isSpectator();}
    private static boolean quakeAudience(ServerPlayer p){return p.isAlive()&&!p.isSpectator();}
    private void quake(ServerLevel level,List<ServerPlayer> players,boolean damage){
        for(var p:players){
            boolean stagger=damage&&eligible(p);
            GiantWisdomNetwork.send(p,damage?.9F:1F,stagger?12:0,false);
            if(stagger){p.hurtServer(level,damageSources().mobAttack(this),2);GiantWisdomNetwork.crouch(p,12);}
        }
        // Sample actual exposed floors; random volume sampling mostly selected empty air.
        for(int i=0;i<24;i++){
            Vec3 center=players.isEmpty()?root():players.get(i%players.size()).position();
            BlockPos top=BlockPos.containing(center.x+random.nextInt(9)-4,center.y+1,center.z+random.nextInt(9)-4);
            for(int down=0;down<8;down++){
                BlockPos p=top.below(down);
                if(!level.hasChunkAt(p))break;
                var state=level.getBlockState(p);
                if(state.getCollisionShape(level,p).isEmpty()||!level.getBlockState(p.above()).isAir())continue;
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,state),p.getX()+.5,p.getY()+1.02,p.getZ()+.5,3,.3,.06,.3,.025);
                break;
            }
        }
    }
    @Override public boolean hurtServer(ServerLevel level,DamageSource source,float amount){
        boolean hurt=super.hurtServer(level,source,amount);
        if(hurt&&isAlive()){
            angryTicks=200;
            quake(level,level.players().stream().filter(p->quakeAudience(p)&&p.distanceToSqr(this)<256).toList(),false);
            if(summonCooldown==0){
                // Each later hit can summon a fresh wave once five seconds have elapsed.
                summon(level);summonCooldown=100;
            }
        }
        return hurt;
    }
    public void broodDied(UUID id){brood.remove(id);}
    private void summon(ServerLevel level){
        Vec3 p=root();level.sendParticles(ParticleTypes.LARGE_SMOKE,p.x,p.y+.3,p.z,55,.5,.6,.5,.025);
        for(int i=0;i<12;i++){
            EntityType<? extends Mob> type=i<2?WisdomRegistration.TYPE:i<5?ModEntities.ROACH.get():i<10?ModEntities.BABY_ROACH.get():ModEntities.GIANT_CENTIPEDE.get();
            Mob mob=type.create(level,EntitySpawnReason.MOB_SUMMONED);if(mob==null)continue;
            double angle=i*Math.PI*2/12;
            mob.snapTo(p.x+Math.cos(angle)*1.3,p.y+.1,p.z+Math.sin(angle)*1.3,random.nextFloat()*360,0);
            boolean clear=level.noCollision(mob);
            for(int trial=0;!clear&&trial<48;trial++){
                double a=angle+trial*2.39996,r=1.5+(trial/12)*.65;
                mob.setPos(p.x+Math.cos(a)*r,p.y+(trial%3)*.5,p.z+Math.sin(a)*r);
                clear=level.hasChunkAt(mob.blockPosition())&&level.noCollision(mob);
            }
            if(!clear){mob.discard();continue;}
            level.sendParticles(ParticleTypes.SMOKE,mob.getX(),mob.getY()+.3,mob.getZ(),8,.2,.3,.2,.02);
            if(mob instanceof net.beamex.shamaschizm.entity.custom.GiantCentipedeEntity)
                ((BroodCentipede)(Object)mob).shamaschizm$initializeHead();
            WisdomBrood.bind(mob,this);if(level.addFreshEntity(mob))brood.add(mob.getUUID());
        }
        playSound(SoundEvents.GHAST_SCREAM,1.8F,.65F);
    }
    @Override protected void dropCustomDeathLoot(ServerLevel level,DamageSource source,boolean playerKill){
        super.dropCustomDeathLoot(level,source,playerKill);
        spawnAtLocation(level,new ItemStack(Items.ROTTEN_FLESH,8+random.nextInt(9)));
    }
    @Override protected SoundEvent getHurtSound(DamageSource source){return SoundEvents.GHAST_HURT;}
    @Override protected SoundEvent getDeathSound(){return SoundEvents.GHAST_DEATH;}
    @Override protected void tickDeath(){
        // Keep the brain on screen for the electrical discharge instead of vanilla's one second.
        deathTime++;
        if(level() instanceof ServerLevel level){
            if(!lightningDone){
                lightningDone=true;
                for(var id:brood){var e=level.getEntity(id);if(e instanceof Mob m)WisdomBrood.release(m);}
                for(var player:level.players())if(eligible(player)&&player.distanceToSqr(this)<=DEATH_RADIUS*DEATH_RADIUS&&metalArmor(player)){
                    lightning(level,player.position());player.hurtServer(level,damageSources().lightningBolt(),5);
                }
            }
            if(deathTime%12==1&&deathTime<60)lightning(level,position().add((random.nextDouble()-.5)*14,0,(random.nextDouble()-.5)*14));
            if(deathTime>=60)remove(RemovalReason.KILLED);
        }
    }
    private void lightning(ServerLevel level,Vec3 p){
        var bolt=EntityTypes.LIGHTNING_BOLT.create(level,EntitySpawnReason.TRIGGERED);
        if(bolt!=null){bolt.snapTo(p.x,p.y,p.z,0,0);bolt.setVisualOnly(true);level.addFreshEntity(bolt);}
    }
    private static boolean metalArmor(ServerPlayer p){
        for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET}){
            var item=p.getItemBySlot(slot).getItem();
            if(item==Items.IRON_HELMET||item==Items.IRON_CHESTPLATE||item==Items.IRON_LEGGINGS||item==Items.IRON_BOOTS||item==Items.COPPER_HELMET||item==Items.COPPER_CHESTPLATE||item==Items.COPPER_LEGGINGS||item==Items.COPPER_BOOTS)return true;
        }return false;
    }
    @Override protected void addAdditionalSaveData(ValueOutput out){
        super.addAdditionalSaveData(out);out.putInt("GiantWormSeed",entityData.get(WORM_SEED));
        if(anchor!=null){out.store("GiantAnchor",Vec3.CODEC,anchor);out.store("GiantAnchorBlock",BlockPos.CODEC,anchorBlock);}
        out.store("GiantBrood",com.mojang.serialization.Codec.STRING.listOf(),brood.stream().map(UUID::toString).toList());
        out.putBoolean("GiantLightningDone",lightningDone);out.putInt("GiantDeathTime",deathTime);
        out.putInt("GiantSummonCooldown",summonCooldown);out.putInt("GiantQuakeCooldown",quakeCooldown);
    }
    @Override protected void readAdditionalSaveData(ValueInput in){
        super.readAdditionalSaveData(in);entityData.set(WORM_SEED,in.getIntOr("GiantWormSeed",0));
        anchor=in.read("GiantAnchor",Vec3.CODEC).orElse(null);anchorBlock=in.read("GiantAnchorBlock",BlockPos.CODEC).orElse(blockPosition());
        brood.clear();for(String id:in.read("GiantBrood",com.mojang.serialization.Codec.STRING.listOf()).orElse(List.of()))try{brood.add(UUID.fromString(id));}catch(IllegalArgumentException ignored){}
        lightningDone=in.getBooleanOr("GiantLightningDone",false);deathTime=in.getIntOr("GiantDeathTime",0);setPersistenceRequired();
        summonCooldown=Math.clamp(in.getIntOr("GiantSummonCooldown",0),0,100);
        quakeCooldown=Math.clamp(in.getIntOr("GiantQuakeCooldown",0),0,40);
    }
}
