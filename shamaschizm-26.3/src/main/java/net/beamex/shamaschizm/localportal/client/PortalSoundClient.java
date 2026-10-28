package net.beamex.shamaschizm.localportal.client;

import java.util.*;
import net.beamex.shamaschizm.localportal.*;
import net.beamex.shamaschizm.world.Schizm;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

@EventBusSubscriber(modid="shamaschizm",value=Dist.CLIENT)
public final class PortalSoundClient {
    private static final Set<ThroughPortal> ACTIVE=new LinkedHashSet<>();
    private static Object world;
    @SubscribeEvent public static void register(RegisterClientPayloadHandlersEvent event){
        event.register(PortalSound.TYPE,(packet,context)->context.enqueueWork(()->play(packet)));
    }
    private static void play(PortalSound packet){
        var mc=Minecraft.getInstance();
        if(mc.level==null||mc.player==null||!mc.level.dimension().equals(Schizm.KEY))return;
        if(world!=mc.level){clear();world=mc.level;}
        if(ACTIVE.size()>=64)return;
        var sound=new ThroughPortal(packet);
        if(sound.isStopped())return;
        ACTIVE.add(sound);mc.getSoundManager().play(sound);
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        var mc=Minecraft.getInstance();
        if(mc.level!=world){clear();world=mc.level;}
        ACTIVE.removeIf(sound->sound.isStopped()||(mc.player!=null&&mc.player.tickCount-sound.started>4&&!mc.getSoundManager().isActive(sound)));
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){clear();world=null;}
    private static void clear(){var manager=Minecraft.getInstance().getSoundManager();for(var sound:ACTIVE)manager.stop(sound);ACTIVE.clear();}
    private static final class ThroughPortal extends AbstractTickableSoundInstance {
        private final PortalSound packet;
        private final Object level;
        private Vec3 lastPosition;
        private final int started;
        ThroughPortal(PortalSound packet){
            super(SoundEvent.createVariableRangeEvent(packet.sound()),packet.category(),RandomSource.create(packet.seed()));
            this.started=Minecraft.getInstance().player.tickCount;
            this.packet=packet;this.level=Minecraft.getInstance().level;this.lastPosition=packet.position();
            pitch=packet.pitch();looping=false;relative=false;attenuation=Attenuation.NONE;
            update(true);
        }
        @Override public void tick(){update(false);}
        private void update(boolean initial){
            var mc=Minecraft.getInstance();
            if(mc.level!=level||mc.player==null||!mc.player.isAlive()||!mc.level.dimension().equals(Schizm.KEY)){stop();return;}
            Doorway source=null,target=null;
            for(var door:DoorwayDebugRenderer.doorways()){
                if(door.name().equals(packet.source()))source=door;
                if(door.name().equals(packet.target()))target=door;
            }
            if(packet.entityId()>=0){
                var moving=PortalEntityScene.soundPosition(packet.source(),packet.entityId(),packet.entityUuid());
                if(moving!=null)lastPosition=moving;
            }
            var route=source==null||target==null?null:PortalSoundPath.find(source,target,mc.player.getEyePosition(),lastPosition,packet.range());
            // Continue an already playing sound normally when the listener crosses to its room.
            double direct=mc.player.getEyePosition().distanceTo(lastPosition);
            if(!initial&&direct<packet.range()&&(route==null||direct<route.distance())){
                x=lastPosition.x;y=lastPosition.y;z=lastPosition.z;
                float desired=PortalSoundPath.gain(packet.volume(),direct,packet.range());
                volume+=(desired-volume)*.5f;return;
            }
            if(route==null){
                if(initial){stop();return;}
                volume*=.5f;if(volume<.001f)stop();return;
            }
            Vec3 point=route.aperture();x=point.x;y=point.y;z=point.z;
            float desired=PortalSoundPath.gain(packet.volume(),route.distance(),packet.range());
            volume=initial?desired:volume+(desired-volume)*.5f;
        }
    }
}
