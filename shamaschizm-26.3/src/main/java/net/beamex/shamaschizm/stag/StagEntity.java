package net.beamex.shamaschizm.stag;
import java.util.*;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class StagEntity extends Cow {
    private static final EntityDataAccessor<Boolean> UNSEEN=SynchedEntityData.defineId(StagEntity.class,EntityDataSerializers.BOOLEAN);
    private record Observation(boolean seen,long tick){}
    private final Map<UUID,Observation> observers=new HashMap<>();
    private int unseenTicks;
    public StagEntity(EntityType<? extends Cow> type,Level level){super(type,level);setSilent(true);}
    // Enforce silence for existing saved stags too, regardless of their old Silent tag.
    @Override public boolean isSilent(){return true;}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(UNSEEN,false);}
    @Override protected void registerGoals(){
        super.registerGoals();
        goalSelector.addGoal(-1,new Goal(){
            {setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK,Flag.JUMP));}
            @Override public boolean canUse(){return unseenForm();}
            @Override public boolean canContinueToUse(){return unseenForm();}
            @Override public boolean requiresUpdateEveryTick(){return true;}
            @Override public void start(){getNavigation().stop();}
            @Override public void tick(){standAndStare();}
        });
        goalSelector.addGoal(1,new AvoidEntityGoal<Player>(this,Player.class,20F,1.7,2.1){
            @Override public boolean canUse(){
                toAvoid=null;double nearest=400;
                for(Player p:mob.level().players()){
                    double distance=mob.distanceToSqr(p);
                    if(p.isAlive()&&!p.isSpectator()&&distance<(p.isCrouching()?100:400)&&distance<nearest){
                        nearest=distance;toAvoid=p;
                    }
                }
                if(toAvoid==null)return false;
                var away=net.minecraft.world.entity.ai.util.DefaultRandomPos.getPosAway(mob,16,7,toAvoid.position());
                if(away==null||toAvoid.distanceToSqr(away)<nearest)return false;
                path=pathNav.createPath(away.x,away.y,away.z,0);return path!=null;
            }
        });
    }
    public void observe(Player player,boolean seen){observers.put(player.getUUID(),new Observation(seen,level().getGameTime()));}
    public boolean unseenForm(){return entityData.get(UNSEEN);}
    @Override public void tick(){
        if(!(level() instanceof ServerLevel level)){super.tick();return;}
        long now=level.getGameTime();boolean seen=false;
        observers.values().removeIf(o->now-o.tick>40);
        for(var player:level.players()){
            if(!player.isAlive()||distanceToSqr(player)>160*160)continue;
            Observation o=observers.get(player.getUUID());
            // Conservative during entity tracking startup or a delayed report.
            if(o==null||now-o.tick>10||o.seen){seen=true;break;}
        }
        if(seen){unseenTicks=0;entityData.set(UNSEEN,false);}
        else if(++unseenTicks>=2)entityData.set(UNSEEN,true);
        super.tick();
        if(unseenForm())standAndStare();
    }
    private void standAndStare(){
        getNavigation().stop();setSpeed(0);setJumping(false);
        setDeltaMovement(0,getDeltaMovement().y,0);
        Player nearest=null;double best=160*160;
        for(Player p:level().players()){
            double d=distanceToSqr(p);
            if(p.isAlive()&&d<best){nearest=p;best=d;}
        }
        if(nearest==null)return;
        double dx=nearest.getX()-getX(),dz=nearest.getZ()-getZ();
        // Stag2's eyes are higher than the normal cow-sized collision box.
        double dy=nearest.getEyeY()-(getY()+2.125*getScale());
        float yaw=(float)(Math.atan2(dz,dx)*180/Math.PI)-90F;
        float pitch=(float)(-Math.atan2(dy,Math.sqrt(dx*dx+dz*dz))*180/Math.PI);
        setYRot(yaw);setYHeadRot(yaw);setYBodyRot(yaw);setXRot(pitch);
    }
    @Override public void travel(net.minecraft.world.phys.Vec3 input){
        if(unseenForm()){
            setDeltaMovement(0,getDeltaMovement().y,0);
            super.travel(net.minecraft.world.phys.Vec3.ZERO);
            setDeltaMovement(0,getDeltaMovement().y,0);
        }else super.travel(input);
    }
    @Override public Cow getBreedOffspring(ServerLevel level,AgeableMob partner){return StagRegistration.STAG.create(level,EntitySpawnReason.BREEDING);}
}
