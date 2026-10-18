package net.beamex.shamaschizm.building;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;

/** Linked, non-solid cells with one render anchor. No periodic mob searches or block-entity ticks. */
public final class WebBlocks {
    private static final ThreadLocal<Boolean> CHANGING=ThreadLocal.withInitial(()->false);
    public static boolean changing(){return CHANGING.get();}
    public static List<BlockPos> cells(WebRectangle r){return WebBlockGeometry.cells(r);}
    public static boolean install(ServerLevel level,WebRectangle rectangle,boolean flipped){
        List<BlockPos> targets=new ArrayList<>();Map<BlockPos,BlockState> old=new HashMap<>();
        for(BlockPos pos:cells(rectangle)){
            if(!level.hasChunkAt(pos)||!level.isInWorldBounds(pos))return false;
            BlockState state=level.getBlockState(pos);
            boolean legacy=state.is(BuildingRegistration.WEB)&&!state.getValue(SpanningWebBlock.LINKED);
            if((legacy||state.canBeReplaced())&&level.getFluidState(pos).isEmpty()) {targets.add(pos);old.put(pos,state);}
        }
        if(targets.isEmpty())return false;
        UUID id=UUID.randomUUID();BlockPos master=targets.get(0);List<BlockPos>written=new ArrayList<>();
        CHANGING.set(true);
        try{
            for(BlockPos pos:targets){
                boolean placed=level.setBlock(pos,BuildingRegistration.WEB.defaultBlockState().setValue(SpanningWebBlock.LINKED,true),2);
                if(placed)written.add(pos);
                if(!placed || !(level.getBlockEntity(pos) instanceof WebBlockEntity be)){
                    for(BlockPos undo:written){
                        level.setBlock(undo,old.get(undo),3);
                        if(level.getBlockEntity(undo) instanceof WebBlockEntity restored)restored.clearConfiguration();
                    }
                    return false;
                }
                be.configure(id,rectangle,flipped,master);
            }
        }finally{CHANGING.remove();}
        for(BlockPos pos:targets)level.updateNeighborsAt(pos,BuildingRegistration.WEB);
        return true;
    }
    public static void remove(ServerLevel level,WebBlockEntity removed){
        if(changing())return;
        CHANGING.set(true);
        try{
            BlockPos owner=removed.owner();
            for(BlockPos pos:cells(removed.rectangle())){
                if(pos.equals(removed.getBlockPos()))continue;
                if(level.getBlockEntity(pos) instanceof WebBlockEntity be && removed.group().equals(be.group()) && owner.equals(be.owner()))
                    level.removeBlock(pos,false); // Only the originally broken cell produces loot.
            }
        }finally{CHANGING.remove();}
    }
    /** Called only after neighbor changes. Fills a newly opened hole in the existing rectangle. */
    public static void repair(ServerLevel level,WebBlockEntity source){
        if(!source.master()||changing())return;
        WebRectangle r=source.rectangle();
        CHANGING.set(true);
        try{
            for(BlockPos pos:cells(r)){
                if(!level.hasChunkAt(pos))continue;
                if(!level.getBlockState(pos).isAir()||!level.getFluidState(pos).isEmpty())continue;
                if(level.setBlock(pos,BuildingRegistration.WEB.defaultBlockState().setValue(SpanningWebBlock.LINKED,true),2)
                        && level.getBlockEntity(pos) instanceof WebBlockEntity be)
                    be.configure(source.group(),r,source.flipped(),source.getBlockPos());
            }
        }finally{CHANGING.remove();}
    }
    public static VoxelShape shape(WebRectangle r,BlockPos pos){return WebBlockGeometry.shape(r,pos);}
    private WebBlocks(){}
}
