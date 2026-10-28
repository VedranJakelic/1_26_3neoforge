package net.beamex.shamaschizm.localportal;

import java.util.*;
import net.beamex.shamaschizm.world.Schizm;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Real same-dimension crossings; separate from the render-only portal replicas. */
@EventBusSubscriber(modid="shamaschizm")
public final class DoorwayEntityTraversal {
    private static final Map<Entity, Track> TRACKS = new WeakHashMap<>();
    private static final class Track {
        Vec3 before;
        long blockedUntil;
        DoorwayProjectileCrossing.Crossing pending;
        Vec3 pendingEnd;
        boolean processedImpact;
    }
    private record Motion(Entity entity, Vec3 velocity, float yaw, float body, float head, double fall) {
        Motion(Entity e) {
            this(e,e.getDeltaMovement(),e.getYRot(),e instanceof LivingEntity l?l.yBodyRot:e.getYRot(),
                    e.getYHeadRot(),e.fallDistance);
        }
    }
    @SubscribeEvent public static void beforeTick(EntityTickEvent.Pre event) {
        var entity=event.getEntity();
        if(!(entity.level() instanceof ServerLevel level))return;
        if(entity instanceof Player||!level.dimension().equals(Schizm.KEY)||entity.isRemoved()||entity.isPassenger()) {
            TRACKS.remove(entity);return;
        }
        // Capture before this tick's movement, so newly spawned/thrown entities do not miss their first crossing.
        var track=TRACKS.computeIfAbsent(entity,e->new Track());
        track.before=entity.position();track.pending=null;track.pendingEnd=null;track.processedImpact=false;
    }
    @SubscribeEvent public static void projectileImpact(ProjectileImpactEvent event) {
        var projectile=event.getProjectile();
        if(!(projectile.level() instanceof ServerLevel level)||!level.dimension().equals(Schizm.KEY))return;
        var track=TRACKS.get(projectile);
        if(track==null||track.before==null||level.getGameTime()<track.blockedUntil)return;
        track.processedImpact=true;
        Vec3 end=event.getRayTraceResult().getLocation();
        var crossing=DoorwayProjectileCrossing.first(DoorwayData.get(level).all(),track.before,end,
                projectile.getBbWidth(),projectile.getBbHeight());
        // A surface exactly on the plane wins over the portal.
        if(crossing==null||crossing.fraction()>=1-1e-7)return;
        if(DoorwayTraversal.findProjectileExit(projectile,crossing.source(),crossing.target(),track.before,end)==null)return;
        // Keep the projectile alive until its tick ends; never teleport in the middle of vanilla's impact loop.
        event.setCanceled(true);track.processedImpact=false;track.pending=crossing;track.pendingEnd=end;
    }
    @SubscribeEvent public static void afterTick(EntityTickEvent.Post event) {
        var entity=event.getEntity();
        if(!(entity.level() instanceof ServerLevel level)||entity instanceof Player)return;
        var track=TRACKS.get(entity);
        if(track==null||track.before==null)return;
        Vec3 previous=track.before;track.before=null;
        if(!level.dimension().equals(Schizm.KEY)||entity.isRemoved()||!entity.isAlive()) {
            TRACKS.remove(entity);return;
        }
        if(entity.isPassenger()||level.getGameTime()<track.blockedUntil)return;
        Vec3 current=entity.position();
        if(entity instanceof Projectile projectile) {
            if(track.processedImpact)return;
            var data=DoorwayData.get(level);
            Vec3 end=track.pending==null?current:track.pendingEnd;
            var crossing=DoorwayProjectileCrossing.first(data.all(),previous,end,entity.getBbWidth(),entity.getBbHeight());
            track.pending=null;track.pendingEnd=null;
            if(crossing!=null) {
                var source=crossing.source();var target=crossing.target();
                Vec3 exit=DoorwayTraversal.findProjectileExit(projectile,source,target,previous,end);
                if(exit==null)moveGroup(entity,level,previous,entity.getYRot(),null,null);
                else moveGroup(entity,level,exit,transformedYaw(source,target,entity.getYRot()),source,target);
                track.blockedUntil=level.getGameTime()+3;
            }
            return;
        }
        double movement=previous.distanceToSqr(current);
        if(movement<1.0e-12||movement>16*16)return; // Ignore unrelated teleports.
        var bounds=DoorwayTraversal.traversalBounds(entity);
        double footOffset=bounds.minY-current.y;
        var data=DoorwayData.get(level);
        for(var source:data.all()) {
            if(!source.enabled()||!source.crossed(previous.add(0,footOffset,0),current.add(0,footOffset,0),
                    bounds.getXsize(),bounds.getYsize()))continue;
            var target=data.get(source.target());
            if(target==null||!target.enabled()||!target.target().equals(source.name()))continue;
            Vec3 destination=DoorwayTraversal.findExit(entity,source,target,previous,current);
            if(destination==null) {
                // Return to the known source-side position instead of putting the entity inside an exit wall.
                moveGroup(entity,level,previous,entity.getYRot(),null,null);
                track.blockedUntil=level.getGameTime()+3;
                return;
            }
            float yaw=transformedYaw(source,target,entity.getYRot());
            if(moveGroup(entity,level,destination,yaw,source,target))track.blockedUntil=level.getGameTime()+3;
            return;
        }
    }
    static float transformedYaw(Doorway source,Doorway target,float yaw) {
        double radians=Math.toRadians(yaw);
        Vec3 heading=source.transformVector(target,new Vec3(-Math.sin(radians),0,Math.cos(radians)));
        return (float)Math.toDegrees(Math.atan2(-heading.x,heading.z));
    }
    private static boolean moveGroup(Entity root,ServerLevel level,Vec3 destination,float yaw,Doorway source,Doorway target) {
        var motions=root.getSelfAndPassengers().map(Motion::new).toList();
        // Vanilla same-dimension teleport retains the entity instances, UUIDs, inventories and riding links,
        // and sends teleport packets to tracking clients, including player passengers.
        if(!root.teleportTo(level,destination.x,destination.y,destination.z,Set.of(),yaw,root.getXRot(),true))return false;
        for(var motion:motions) {
            var entity=motion.entity();
            if(source!=null) {
                entity.setDeltaMovement(source.transformVector(target,motion.velocity()));
                entity.setYRot(transformedYaw(source,target,motion.yaw()));
                entity.setYHeadRot(transformedYaw(source,target,motion.head()));
                entity.setYBodyRot(transformedYaw(source,target,motion.body()));
            } else entity.setDeltaMovement(Vec3.ZERO);
            entity.fallDistance=motion.fall();entity.syncPosition=true;entity.syncVelocity=true;
            entity.setOldPosAndRot();
            if(entity instanceof LivingEntity living){living.yBodyRotO=living.yBodyRot;living.yHeadRotO=living.yHeadRot;}
            if(entity instanceof Mob mob) {
                mob.getNavigation().stop();
                mob.getMoveControl().setWantedPosition(entity.getX(),entity.getY(),entity.getZ(),0);
            }
            var memberTrack=TRACKS.get(entity);
            if(memberTrack!=null){memberTrack.before=null;memberTrack.blockedUntil=level.getGameTime()+3;}
            if(entity instanceof ServerPlayer player) {
                if(source!=null)PacketDistributor.sendToPlayer(player,new DoorwayNetwork.Turn(entity.getYRot()-motion.yaw()));
                player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));
            }
        }
        return true;
    }
}
