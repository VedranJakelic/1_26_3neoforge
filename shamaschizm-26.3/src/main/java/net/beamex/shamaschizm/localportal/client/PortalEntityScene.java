package net.beamex.shamaschizm.localportal.client;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.beamex.shamaschizm.localportal.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.syncher.*;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

/** Render-only replicas. No world insertion, ticking, AI, collision, or client sound generation. */
@EventBusSubscriber(modid="shamaschizm",value=Dist.CLIENT)
public final class PortalEntityScene {
    private static final Map<String,Scene> SCENES=new HashMap<>();
    private static Object world;
    private static final Set<String> WARNED=new HashSet<>();
    private record Scene(Doorway source,Doorway target,long received,Map<UUID,Replica> entities) {}
    @SubscribeEvent public static void register(RegisterClientPayloadHandlersEvent event) {
        event.register(PortalEntities.TYPE,(packet,context)->accept(packet));
    }
    public static void clear(){SCENES.clear();WARNED.clear();world=null;}
    private static void accept(PortalEntities packet) {
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        if(world!=mc.level){clear();world=mc.level;}
        if(SCENES.size()>4)SCENES.clear();
        var old=SCENES.get(packet.source().name());
        var entries=new LinkedHashMap<UUID,Replica>();long now=System.nanoTime();
        for(var entry:packet.entries())try {
            var replica=old==null?null:old.entities.get(entry.uuid());
            if(replica==null||replica.entity.getType()!=entry.type())replica=new Replica(entry);
            if(replica.entity==null)continue;
            replica.update(entry,now);entries.put(entry.uuid(),replica);
        }catch(RuntimeException failure){warn(entry.type().toString(),failure);}
        // Rebuild only replica riding links; never attach a visual copy to a real client entity.
        if(old!=null)for(var replica:old.entities.values())replica.entity.stopRiding();
        var byId=new HashMap<Integer,Replica>();
        for(var replica:entries.values()){replica.entity.stopRiding();byId.put(replica.last.id(),replica);}
        for(var replica:entries.values()) {
            var vehicle=byId.get(replica.last.vehicleId());
            if(vehicle!=null&&vehicle!=replica)try{replica.entity.startRiding(vehicle.entity,true,false);}
            catch(RuntimeException failure){warn(replica.entity.getType().toString()+" riding pose",failure);}
        }
        SCENES.put(packet.source().name(),new Scene(packet.source(),packet.target(),now,entries));
    }
    static Vec3 soundPosition(String source,int id,UUID uuid){
        var mc=Minecraft.getInstance();if(mc.level==null)return null;
        var live=mc.level.getEntity(id);
        if(live!=null&&live.getUUID().equals(uuid)&&!live.isRemoved())return live.position();
        if(world!=mc.level)return null;
        var scene=SCENES.get(source);long now=System.nanoTime();
        if(scene==null||now-scene.received>2_000_000_000L)return null;
        var r=scene.entities.get(uuid);if(r==null||r.last.id()!=id)return null;
        return r.from.lerp(r.last.position(),Math.clamp((now-r.received)/200_000_000.0,0,1));
    }
    static void warn(String type,RuntimeException failure) {
        if(WARNED.add(type))org.slf4j.LoggerFactory.getLogger("shamaschizm.localportal").warn("Portal entity renderer skipped {}",type,failure);
    }
    static List<Visual> visuals(PortalTerrain terrain) {
        var mc=Minecraft.getInstance();if(world!=mc.level){clear();return List.of();}
        long now=System.nanoTime();SCENES.entrySet().removeIf(e->now-e.getValue().received>2_000_000_000L);
        var scene=SCENES.get(terrain.source().name());
        if(scene==null||!scene.source.equals(terrain.source())||!scene.target.equals(terrain.target()))return List.of();
        var result=new ArrayList<Visual>();
        for(var r:scene.entities.values()) {
            var live=mc.level.getEntity(r.last.id());
            if(live!=null&&live.getUUID().equals(r.last.uuid())&&!live.isRemoved())result.add(new Visual(live,r.last.light(),false));
            else {r.pose(now);result.add(new Visual(r.entity,r.last.light(),true));}
        }
        return result;
    }
    record Visual(Entity entity,int light,boolean replica) {}
    private static final class Replica {
        final Entity entity;
        final Map<Integer,SynchedEntityData.DataValue<?>> defaults=new HashMap<>();
        PortalEntities.Entry last;
        Vec3 from;
        float fromYaw,fromPitch,fromBody,fromHead;
        long received;
        int animationTick=-1;
        float walkSpeed;
        Replica(PortalEntities.Entry e) {
            var level=Minecraft.getInstance().level;
            entity=e.type()==EntityTypes.PLAYER?new RemotePlayer(level,new GameProfile(e.uuid(),e.playerName()))
                    :e.type().create(level,EntitySpawnReason.LOAD);
            if(entity!=null){entity.setId(e.id());entity.setUUID(e.uuid());entity.noPhysics=true;entity.setPos(e.position());}
        }
        void update(PortalEntities.Entry e,long now) {
            if(last!=null)pose(now);
            from=entity.position();fromYaw=last==null?e.yaw():entity.getYRot();fromPitch=last==null?e.pitch():entity.getXRot();
            fromBody=entity instanceof LivingEntity l&&last!=null?l.yBodyRot:e.bodyYaw();
            fromHead=entity instanceof LivingEntity l&&last!=null?l.yHeadRot:e.headYaw();
            if(last==null||from.distanceToSqr(e.position())>64)from=e.position();
            walkSpeed=Math.max(e.speed(),(float)Math.min(1,from.subtract(e.position()).horizontalDistance()));
            for(var value:e.data().packedItems())defaults.computeIfAbsent(value.id(),id->defaultValue(entity,value));
            entity.getEntityData().assignValues(new ArrayList<>(defaults.values()));
            entity.getEntityData().assignValues(e.data().packedItems());
            if(entity instanceof LivingEntity l) {
                int i=0;for(var slot:EquipmentSlot.values())l.setItemSlot(slot,e.equipment().get(i++).copy());
                l.hurtTime=e.hurt();l.deathTime=e.death();
            }
            last=e;received=now;
        }
        void pose(long now) {
            float t=Math.clamp((now-received)/200_000_000f,0,1);
            Vec3 pos=from.lerp(last.position(),t);entity.setPos(pos);
            entity.xo=entity.xOld=pos.x;entity.yo=entity.yOld=pos.y;entity.zo=entity.zOld=pos.z;
            entity.setYRot(Mth.rotLerp(t,fromYaw,last.yaw()));entity.yRotO=entity.getYRot();
            entity.setXRot(Mth.lerp(t,fromPitch,last.pitch()));entity.xRotO=entity.getXRot();
            entity.tickCount=last.age()+(int)Math.min(8,(now-received)/50_000_000L);
            if(entity instanceof LivingEntity l) {
                l.yBodyRot=l.yBodyRotO=Mth.rotLerp(t,fromBody,last.bodyYaw());
                l.yHeadRot=l.yHeadRotO=Mth.rotLerp(t,fromHead,last.headYaw());
                if(animationTick!=entity.tickCount){l.walkAnimation.update(walkSpeed,.4f,1);animationTick=entity.tickCount;}
            }
        }
    }
    private static <T> SynchedEntityData.DataValue<T> defaultValue(Entity entity,SynchedEntityData.DataValue<T> value) {
        var accessor=new EntityDataAccessor<T>(value.id(),value.serializer());
        return new SynchedEntityData.DataValue<>(value.id(),value.serializer(),value.serializer().copy(entity.getEntityData().get(accessor)));
    }
}
