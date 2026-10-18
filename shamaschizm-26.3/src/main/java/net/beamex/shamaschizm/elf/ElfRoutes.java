package net.beamex.shamaschizm.elf;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
public final class ElfRoutes {
    private ElfRoutes(){}
    public static void spawn(ServerLevel level,List<BlockPos> markers,List<BoundingBox> rooms) {
        ElfRouteData.get(level).add(markers); // Independent of elf pathfinding and spawn success.
        List<BlockPos> points=new ArrayList<>();
        for(BlockPos marker:markers){
            BlockPos floor=findFloor(level,marker,rooms);
            // Never silently bridge a missing walkable doorway.
            if(floor==null){net.beamex.shamaschizm.Shamaschizm.LOGGER.warn("Elf route has no safe floor near {}; skipping guide spawning",marker);return;}
            if(points.isEmpty()||!points.get(points.size()-1).equals(floor))points.add(floor);
        }
        if(points.size()<2)return;
        int count=Math.min(6,Math.max(2,rooms.size()/3));
        List<Integer> starts=new ArrayList<>();for(int i=0;i<points.size();i++)starts.add(i);
        Collections.shuffle(starts,new Random(level.getRandom().nextLong()));
        for(int i=0;i<Math.min(count,starts.size());i++){
            int start=starts.get(i);var pos=points.get(start);
            ElfEntity elf=ElfRegistration.ELF.create(level,EntitySpawnReason.STRUCTURE);
            if(elf==null)continue;
            elf.snapTo(pos.getX()+0.5,pos.getY(),pos.getZ()+0.5,level.getRandom().nextFloat()*360,0);
            elf.configure(points,rooms,start);level.addFreshEntity(elf);
        }
    }
    private static BlockPos findFloor(ServerLevel level,BlockPos marker,List<BoundingBox> rooms){
        for(int r=0;r<=3;r++)for(int dy=2;dy>=-8;dy--)for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++){
            if(Math.max(Math.abs(dx),Math.abs(dz))!=r)continue;
            var p=marker.offset(dx,dy,dz);
            if(rooms.stream().noneMatch(b->b.isInside(p))||!level.hasChunkAt(p))continue;
            if(level.getBlockState(p).isAir()&&level.getBlockState(p.above()).isAir()
                    &&level.getBlockState(p.below()).isFaceSturdy(level,p.below(),Direction.UP))return p.immutable();
        }
        return null;
    }
}
