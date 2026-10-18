package net.beamex.shamaschizm.elf;

import java.util.*;
import com.mojang.serialization.Codec;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.effect.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Saved doorway polylines, indexed by chunk; never requests terrain or elf navigation. */
@EventBusSubscriber(modid=Shamaschizm.MOD_ID)
public final class ElfRouteData extends SavedData {
    private static final Codec<ElfRouteData> CODEC=BlockPos.CODEC.listOf().listOf().xmap(ElfRouteData::new,d->d.routes);
    public static final SavedDataType<ElfRouteData> TYPE=new SavedDataType<>(Shamaschizm.id("elf_routes"),()->new ElfRouteData(List.of()),CODEC,null);
    private final List<List<BlockPos>> routes=new ArrayList<>();
    private final Map<Long,Set<BlockPos>> samples=new HashMap<>();
    private final Set<List<BlockPos>> known=new HashSet<>();
    public ElfRouteData(List<List<BlockPos>> saved){for(var route:saved)add(route);}
    public static ElfRouteData get(ServerLevel level){return level.getDataStorage().computeIfAbsent(TYPE);}
    public void add(List<BlockPos> points){
        if(points.size()<2)return;
        var route=points.stream().map(BlockPos::immutable).toList();
        if(!known.add(route))return;
        routes.add(route);setDirty();
        for(int i=1;i<route.size();i++){
            BlockPos a=route.get(i-1),b=route.get(i);
            double distance=Math.sqrt(a.distSqr(b));
            if(distance>512)continue; // Reject corrupt/unrelated saved waypoints.
            int steps=Math.max(1,(int)Math.ceil(distance/2));
            for(int n=0;n<=steps;n++){
                double t=(double)n/steps;
                BlockPos p=BlockPos.containing(a.getX()+(b.getX()-a.getX())*t,a.getY()+(b.getY()-a.getY())*t,a.getZ()+(b.getZ()-a.getZ())*t);
                samples.computeIfAbsent(ChunkPos.pack(p.getX()>>4,p.getZ()>>4),k->new HashSet<>()).add(p);
            }
        }
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event){
        if(!(event.getEntity() instanceof ServerPlayer player)||!player.isAlive()
                ||!player.hasEffect(ModEffects.TRIPPING)||player.tickCount%5!=0)return;
        ServerLevel level=player.level();var data=get(level);var origin=player.blockPosition();
        List<BlockPos> nearby=new ArrayList<>();int cx=origin.getX()>>4,cz=origin.getZ()>>4;
        for(int x=cx-2;x<=cx+2;x++)for(int z=cz-2;z<=cz+2;z++)
            for(BlockPos p:data.samples.getOrDefault(ChunkPos.pack(x,z),Set.of()))
                if(p.distSqr(origin)<=32*32)nearby.add(p);
        if(nearby.isEmpty())return;
        for(int i=0;i<6;i++){
            BlockPos at=nearby.get(level.getRandom().nextInt(nearby.size()));
            if(!level.hasChunkAt(at))continue;
            for(int n=0;n<=16;n++){
                int dy=n==0?0:((n+1)/2)*(n%2==1?-1:1);BlockPos p=at.offset(0,dy,0);
                if(p.getY()<=level.getMinY()||p.getY()>=level.getMaxY())continue;
                if(!level.getBlockState(p).getCollisionShape(level,p).isEmpty()||!level.getFluidState(p).isEmpty()
                        ||!level.getBlockState(p.below()).isFaceSturdy(level,p.below(),Direction.UP))continue;
                level.sendParticles(player,ElfTrailParticles.TRAIL,false,false,p.getX()+0.5,p.getY()+0.2,p.getZ()+0.5,1,0.12,0.06,0.12,0);break;
            }
        }
    }
}
