package net.beamex.shamaschizm.localportal;

import java.util.*;
import net.beamex.shamaschizm.world.Schizm;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.*;
import net.minecraft.server.players.PlayerList;
import net.minecraft.sounds.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/** Observes final vanilla broadcasts, after cancellable sound hooks have run. */
public final class PortalSounds {
    private record Budget(long tick,int count) {}
    private static final Map<ServerPlayer,Budget> BUDGETS=new WeakHashMap<>();
    public static void broadcast(PlayerList players,net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
                                 Vec3 broadcastPosition,double nativeRadius,Packet<?> packet){
        if(!dimension.equals(Schizm.KEY))return;
        ServerLevel level=players.getServer().getLevel(dimension);if(level==null)return;
        if(packet instanceof ClientboundSoundPacket p)
            relay(level,new Vec3(p.getX(),p.getY(),p.getZ()),broadcastPosition,nativeRadius,p.getSound().value(),p.getSource(),p.getVolume(),p.getPitch(),p.getSeed(),-1);
        else if(packet instanceof ClientboundSoundEntityPacket p)
            relay(level,broadcastPosition,broadcastPosition,nativeRadius,p.getSound().value(),p.getSource(),p.getVolume(),p.getPitch(),p.getSeed(),p.getId());
        else if(packet instanceof ClientboundLevelEventPacket p && p.getType()==2001){
            var state=Block.stateById(p.getData());if(state.isAir())return;
            var type=state.getSoundType();
            relay(level,Vec3.atCenterOf(p.getPos()),broadcastPosition,nativeRadius,type.getBreakSound(),SoundSource.BLOCKS,
                (type.getVolume()+1)/2,type.getPitch()*.8f,level.getGameTime()^p.getPos().asLong(),-1);
        }
    }
    public static void relay(ServerLevel level,Vec3 soundPos,Vec3 nativePosition,double nativeRadius,SoundEvent sound,SoundSource category,
                             float volume,float pitch,long seed,int entityId){
        if(!level.dimension().equals(Schizm.KEY)||category==SoundSource.MUSIC||category==SoundSource.RECORDS||category==SoundSource.MASTER
            ||!Float.isFinite(volume)||volume<=0||!Float.isFinite(pitch)||pitch<=0)return;
        float range=Math.min(64,sound.getRange(volume));if(!Float.isFinite(range)||range<=0)return;
        var doors=DoorwayData.get(level).all();if(doors.isEmpty())return;
        var byName=new HashMap<String,Doorway>();for(var d:doors)byName.put(d.name(),d);
        var entity=entityId>=0?level.getEntity(entityId):null;
        for(var player:level.players()){
            if(!player.isAlive())continue;
            // Vanilla already sends the original to everyone in this radius. Never add an echo.
            if(player.position().distanceToSqr(nativePosition)<nativeRadius*nativeRadius)continue;
            var budget=BUDGETS.get(player);long tick=level.getGameTime();
            if(budget!=null&&budget.tick==tick&&budget.count>=24)continue;
            Doorway selected=null,target=null;double shortest=Double.POSITIVE_INFINITY;
            for(var a:doors){
                if(!a.enabled())continue;var b=byName.get(a.target());if(b==null)continue;
                var route=PortalSoundPath.find(a,b,player.position().add(0,player.getEyeHeight(),0),soundPos,range);
                if(route!=null&&route.distance()<shortest){shortest=route.distance();selected=a;target=b;}
            }
            if(selected==null)continue;
            PacketDistributor.sendToPlayer(player,new PortalSound(selected.name(),target.name(),sound.location(),category,soundPos,
                Math.min(16,volume),Math.min(4,pitch),range,seed,entity==null?-1:entityId,entity==null?null:entity.getUUID()));
            BUDGETS.put(player,new Budget(tick,budget!=null&&budget.tick==tick?budget.count+1:1));
        }
    }
}
