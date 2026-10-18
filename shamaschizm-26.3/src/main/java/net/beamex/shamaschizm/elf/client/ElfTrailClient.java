package net.beamex.shamaschizm.elf.client;

import java.util.*;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.effect.ModEffects;
import net.beamex.shamaschizm.elf.ElfEntity;
import net.beamex.shamaschizm.elf.ElfTrailParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/** Private route motes plus a fallback trail from tracked elf movement. */
@EventBusSubscriber(modid=Shamaschizm.MOD_ID,value=Dist.CLIENT)
public final class ElfTrailClient {
    private record Crumb(Vec3 feet,long expires){}
    private static final LinkedHashMap<BlockPos,Crumb> TRAIL=new LinkedHashMap<>();
    private static final Map<UUID,Vec3> PREVIOUS=new HashMap<>();
    private static final List<Mote> ACTIVE=new ArrayList<>();
    private static ClientLevel lastLevel;
    private static long ticks;
    private static boolean visible(){
        var mc=Minecraft.getInstance();
        return mc.player!=null&&mc.player.isAlive()&&mc.player.hasEffect(ModEffects.TRIPPING);
    }
    @SubscribeEvent public static void providers(RegisterParticleProvidersEvent event){
        event.registerSpriteSet(ElfTrailParticles.TRAIL,sprites->(options,level,x,y,z,dx,dy,dz,random)->
                trackedMote(level,x,y,z,sprites));
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        var mc=Minecraft.getInstance();
        if(mc.level!=lastLevel){
            lastLevel=mc.level;TRAIL.clear();PREVIOUS.clear();ticks=0;
            ACTIVE.forEach(Mote::remove);ACTIVE.clear();
        }
        if(!visible()){ACTIVE.forEach(Mote::remove);ACTIVE.clear();}
        if(mc.level==null||mc.player==null||mc.isPaused())return;
        ticks++;
        if(ticks%5==0){
            Set<UUID> seen=new HashSet<>();
            for(ElfEntity elf:mc.level.getEntitiesOfClass(ElfEntity.class,mc.player.getBoundingBox().inflate(64))){
                seen.add(elf.getUUID());Vec3 feet=elf.position();Vec3 previous=PREVIOUS.put(elf.getUUID(),feet);
                // Never draw a line across teleports or stationary conversation positions.
                if(previous==null||feet.distanceToSqr(previous)<0.01||feet.distanceToSqr(previous)>4)continue;
                BlockPos key=BlockPos.containing(feet);
                TRAIL.remove(key);TRAIL.put(key,new Crumb(feet,ticks+7200));
            }
            PREVIOUS.keySet().retainAll(seen);
            TRAIL.values().removeIf(c->c.expires<=ticks);
            while(TRAIL.size()>1024)TRAIL.remove(TRAIL.keySet().iterator().next());
        }
        ACTIVE.removeIf(p->{if(ticks-p.born>p.getLifetime()){p.remove();return true;}return !p.isAlive();});
        if(!visible()||ticks%5!=0||ACTIVE.size()>=256)return;
        var random=mc.level.getRandom();
        List<Crumb> nearby=new ArrayList<>();
        for(Crumb c:TRAIL.values())if(c.feet.distanceToSqr(mc.player.position())<=24*24)nearby.add(c);
        if(nearby.isEmpty())return;
        // Bounded local fallback; saved-route motes do not depend on seeing an elf.
        for(int i=0;i<3&&ACTIVE.size()<256;i++){
            Crumb c=nearby.get(random.nextInt(nearby.size()));
            double x=c.feet.x+(random.nextDouble()-0.5)*0.15;
            double y=c.feet.y+0.15+random.nextDouble()*0.12;
            double z=c.feet.z+(random.nextDouble()-0.5)*0.15;
            BlockPos pos=BlockPos.containing(x,y,z);
            if(!mc.level.hasChunkAt(pos)||!mc.level.getBlockState(pos).getCollisionShape(mc.level,pos).isEmpty()
                    ||!mc.level.getFluidState(pos).isEmpty())continue;
            var p=mc.particleEngine.createParticle(ElfTrailParticles.TRAIL,x,y,z,0,0,0);
            // The provider tracks both local and server-sent particles.
        }
    }
    private static Mote trackedMote(ClientLevel level,double x,double y,double z,SpriteSet sprites) {
        if (!visible()) return null;
        ACTIVE.removeIf(p -> !p.isAlive());
        if (ACTIVE.size()>=256) return null;
        Mote mote=new Mote(level,x,y,z,sprites);ACTIVE.add(mote);return mote;
    }
    private static final class Mote extends SimpleAnimatedParticle {
        private final long born=ticks;
        Mote(ClientLevel level,double x,double y,double z,SpriteSet sprites){
            super(level,x,y,z,sprites,0);
            setColor(0xFFFFFF);quadSize=0.028F;lifetime=180+random.nextInt(81);
            yd=0.0006F;xd=0;zd=0;alpha=0;hasPhysics=true;setSpriteFromAge(sprites);
        }
        @Override public void tick(){
            if(!visible()||Minecraft.getInstance().level!=level){remove();return;}
            super.tick();
            float fadeIn=Math.min(1F,age/6F),fadeOut=Math.min(1F,(lifetime-age)/12F);
            alpha=0.22F*Math.max(0F,Math.min(fadeIn,fadeOut));
        }
    }
}
