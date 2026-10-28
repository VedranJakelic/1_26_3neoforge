package net.beamex.shamaschizm.localportal;

import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.phys.Vec3;

/** A remembered entrance for an existing chase, not a portal graph/pathfinder. */
public final class DoorwayPursuit {
    private static final double RANGE=16;
    private static final int DURATION=200;
    private DoorwayPursuit() {}

    /** Must run before teleportation, while target validation still uses the old position. */
    static List<Mob> followers(ServerPlayer player,Doorway source) {
        if(player.isSpectator()||player.isCreative())return List.of();
        return player.level().getEntitiesOfClass(Mob.class,player.getBoundingBox().inflate(RANGE),mob ->
                mob.isAlive()&&!mob.isNoAi()&&!mob.isPassenger()&&!mob.isVehicle()
                &&mob.getNavigation() instanceof GroundPathNavigation&&mob.getTarget()==player
                &&mob.distanceToSqr(player)<=RANGE*RANGE&&source.distance(mob.position())>0
                &&source.origin().distanceToSqr(mob.position())<=RANGE*RANGE
                &&mob.getBbWidth()<source.width()&&mob.getBbHeight()<=source.height());
    }
    static void departed(List<Mob> followers,ServerPlayer player,Doorway source,Doorway target) {
        for(var mob:followers) {
            Chase goal=null;
            for(var wrapped:mob.goalSelector.getAvailableGoals())if(wrapped.getGoal() instanceof Chase chase){goal=chase;break;}
            if(goal==null){goal=new Chase(mob);mob.goalSelector.addGoal(0,goal);}
            goal.remember(player,source,target);
        }
    }
    static void crossed(Mob mob,Doorway source,Doorway target) {
        for(var wrapped:mob.goalSelector.getAvailableGoals())if(wrapped.getGoal() instanceof Chase chase)chase.arrived(source,target);
    }
    private static final class Chase extends Goal {
        private final Mob mob;
        private ServerPlayer player;
        private Doorway source,target;
        private long expires,nextPath,lastProgress;
        private Vec3 progressPosition;
        private boolean crossing;
        Chase(Mob mob){this.mob=mob;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
        void remember(ServerPlayer player,Doorway source,Doorway target) {
            this.player=player;this.source=source;this.target=target;
            long now=mob.level().getGameTime();expires=now+DURATION;nextPath=0;lastProgress=now;
            progressPosition=mob.position();crossing=false;
            mob.getNavigation().stop();
        }
        private boolean valid() {
            if(player==null)return false;
            if(!(mob.level() instanceof ServerLevel level)||!mob.isAlive()||mob.isNoAi()||mob.isPassenger()||mob.isVehicle()
                    ||!player.isAlive()||player.isSpectator()||player.isCreative()||player.level()!=level
                    ||level.getGameTime()>=expires||source.origin().distanceToSqr(mob.position())>24*24
                    ||mob.getTarget()!=null&&mob.getTarget()!=player) {player=null;return false;}
            var data=DoorwayData.get(level);
            if(!source.equals(data.get(source.name()))||!target.equals(data.get(target.name()))){player=null;return false;}
            return true;
        }
        @Override public boolean canUse(){return valid();}
        @Override public boolean canContinueToUse(){return valid();}
        @Override public boolean requiresUpdateEveryTick(){return true;}
        @Override public void start(){mob.getNavigation().stop();}
        @Override public void stop(){
            mob.getNavigation().stop();
            mob.getMoveControl().setWantedPosition(mob.getX(),mob.getY(),mob.getZ(),0);
            player=null;
        }
        @Override public void tick() {
            if(!valid())return;
            long now=mob.level().getGameTime();
            if(mob.position().distanceToSqr(progressPosition)>.25*.25){progressPosition=mob.position();lastProgress=now;}
            if(now-lastProgress>=60){player=null;mob.getNavigation().stop();return;}
            double approachDepth=mob.getBbWidth()/2+.65;
            Vec3 approach=source.world(0,0,approachDepth);
            Vec3 local=source.local(mob.position());
            double steeringRadius=Math.max(.75,mob.getBbWidth()+.25);
            if(!crossing&&mob.position().subtract(approach).horizontalDistanceSqr()<steeringRadius*steeringRadius
                    &&Math.abs(local.x)+mob.getBbWidth()/2<source.width()/2
                    &&Math.abs(local.y)<.6) {crossing=true;mob.getNavigation().stop();}
            if(crossing) {
                // Normal move control/collisions take the mob across the plane; traversal performs the teleport.
                Vec3 beyond=source.world(0,0,-mob.getBbWidth()/2-.35);
                mob.getLookControl().setLookAt(beyond.x,mob.getEyeY(),beyond.z);
                mob.getMoveControl().setWantedPosition(beyond.x,beyond.y,beyond.z,1.0);
            } else if(now>=nextPath) {
                nextPath=now+10;
                mob.getNavigation().moveTo(approach.x,approach.y,approach.z,1.0);
            }
        }
        void arrived(Doorway from,Doorway to) {
            if(player==null)return;
            ServerPlayer prey=player;boolean same=from.equals(source)&&to.equals(target);
            player=null;mob.getNavigation().stop();
            if(same&&prey.isAlive()&&!prey.isSpectator()&&!prey.isCreative()&&prey.level()==mob.level())mob.setTarget(prey);
        }
    }
}
