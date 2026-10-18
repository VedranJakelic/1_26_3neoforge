package net.beamex.shamaschizm.world;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Repair quietly placed portal templates after chunk loading; never load surrounding terrain. */
@EventBusSubscriber(modid=Shamaschizm.MOD_ID)
public final class PortalOrientationUpdates {
    private static final Map<ServerLevel,LinkedHashMap<ChunkPos,Long>> PENDING=new WeakHashMap<>();
    private PortalOrientationUpdates() {}
    private static void queue(ServerLevel level,int x,int z) {
        PENDING.computeIfAbsent(level,k->new LinkedHashMap<>())
                .putIfAbsent(new ChunkPos(x,z),level.getGameTime()+20);
    }
    public static void queueRoom(ServerLevel level,BoundingBox box) {
        for(int x=box.minX()>>4;x<=box.maxX()>>4;x++)
            for(int z=box.minZ()>>4;z<=box.maxZ()>>4;z++)queue(level,x,z);
    }
    @SubscribeEvent public static void loaded(ChunkEvent.Load event) {
        if(!(event.getLevel() instanceof ServerLevel level))return;
        ChunkPos pos=event.getChunk().getPos();
        level.getServer().execute(()->{
            // Hook lines may cross a chunk boundary. Revisit loaded neighbors;
            // never load a new chunk merely to refresh a trap.
            for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)
                if((dx==0 && dz==0) || level.getChunkSource().getChunkNow((pos.getMinBlockX()>>4)+dx,(pos.getMinBlockZ()>>4)+dz)!=null)
                    queue(level,(pos.getMinBlockX()>>4)+dx,(pos.getMinBlockZ()>>4)+dz);
        });
    }
    @SubscribeEvent public static void tick(LevelTickEvent.Post event) {
        if(!(event.getLevel() instanceof ServerLevel level))return;
        var pending=PENDING.get(level);if(pending==null)return;
        // Remove jobs before updating blocks; block updates may queue more jobs.
        var ready=new ArrayList<ChunkPos>();
        var it=pending.entrySet().iterator();
        while(it.hasNext()&&ready.size()<2){var job=it.next();if(job.getValue()<=level.getGameTime()){ready.add(job.getKey());it.remove();}}
        for(ChunkPos pos:ready){LevelChunk chunk=level.getChunkSource().getChunkNow(pos.getMinBlockX()>>4,pos.getMinBlockZ()>>4);if(chunk!=null)repair(level,chunk);}
    }
    private static boolean relevant(BlockState state) {
        return state.is(net.beamex.shamaschizm.registry.ModBlocks.SCHIZM_PORTAL);
    }
    private static void repair(ServerLevel level,LevelChunk chunk) {
        var sections=chunk.getSections();
        for(int i=0;i<sections.length;i++){
            var section=sections[i];if(section.hasOnlyAir()||!section.maybeHas(PortalOrientationUpdates::relevant))continue;
            int y0=chunk.getMinY()+i*16;
            for(int x=0;x<16;x++)for(int y=0;y<16;y++)for(int z=0;z<16;z++){
                if(!relevant(section.getBlockState(x,y,z)))continue;
                BlockPos p=new BlockPos(chunk.getPos().getMinBlockX()+x,y0+y,chunk.getPos().getMinBlockZ()+z);
                level.scheduleTick(p,net.beamex.shamaschizm.registry.ModBlocks.SCHIZM_PORTAL,1);
            }
        }
    }
}
