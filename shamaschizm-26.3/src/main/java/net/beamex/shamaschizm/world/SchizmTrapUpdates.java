package net.beamex.shamaschizm.world;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.world.block.PoisonTrapBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TripWireBlock;
import net.minecraft.world.level.block.TripWireHookBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Deferred, bounded initialization for quiet structure placements and existing rooms. */
@EventBusSubscriber(modid=Shamaschizm.MOD_ID)
public final class SchizmTrapUpdates {
    private static final Map<ServerLevel,LinkedHashMap<ChunkPos,Long>> PENDING=new WeakHashMap<>();
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE
            | Block.UPDATE_SKIP_SHAPE_UPDATE_ON_WIRE | Block.UPDATE_SKIP_ON_PLACE;
    private SchizmTrapUpdates() {}
    private static void queue(ServerLevel level,int x,int z) {
        PENDING.computeIfAbsent(level,k->new LinkedHashMap<>())
                .putIfAbsent(new ChunkPos(x,z),level.getGameTime()+20);
    }
    public static void queueRoom(ServerLevel level,BoundingBox box) {
        for(int x=box.minX()>>4;x<=box.maxX()>>4;x++)
            for(int z=box.minZ()>>4;z<=box.maxZ()>>4;z++)queue(level,x,z);
    }
    @SubscribeEvent public static void loaded(ChunkEvent.Load event) {
        if(!(event.getLevel() instanceof ServerLevel level)||!level.dimension().equals(Schizm.KEY))return;
        ChunkPos pos=event.getChunk().getPos();
        level.getServer().execute(()->{
            // Hook lines may cross a chunk boundary. Revisit loaded neighbors;
            // never load a new chunk merely to refresh a trap.
            for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++)
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
        return state.getBlock() instanceof PoisonTrapBlock || state.is(Blocks.TRIPWIRE) || state.is(Blocks.TRIPWIRE_HOOK);
    }
    private static void repair(ServerLevel level,LevelChunk chunk) {
        var hooks=new ArrayList<BlockPos>();var sections=chunk.getSections();
        for(int i=0;i<sections.length;i++){
            var section=sections[i];if(section.hasOnlyAir()||!section.maybeHas(SchizmTrapUpdates::relevant))continue;
            int y0=chunk.getMinY()+i*16;
            for(int x=0;x<16;x++)for(int y=0;y<16;y++)for(int z=0;z<16;z++){
                BlockState state=section.getBlockState(x,y,z);if(!relevant(state))continue;
                BlockPos pos=new BlockPos(chunk.getPos().getMinBlockX()+x,y0+y,chunk.getPos().getMinBlockZ()+z);
                if(state.getBlock() instanceof PoisonTrapBlock trap){level.scheduleTick(pos,trap,1);continue;}
                if(state.is(Blocks.TRIPWIRE_HOOK)){hooks.add(pos);continue;}
                TripWireBlock wire=(TripWireBlock)state.getBlock();BlockState fixed=state;
                for(Direction dir:Direction.Plane.HORIZONTAL){
                    if(!level.hasChunkAt(pos.relative(dir)))continue;
                    var property=switch(dir){case NORTH->TripWireBlock.NORTH;case SOUTH->TripWireBlock.SOUTH;case EAST->TripWireBlock.EAST;default->TripWireBlock.WEST;};
                    fixed=fixed.setValue(property,wire.shouldConnectTo(level.getBlockState(pos.relative(dir)),dir));
                }
                if(fixed!=state)level.setBlock(pos,fixed,QUIET);
            }
        }
        for (BlockPos pos : hooks) initializeLine(level, pos);
    }

    private static void initializeLine(ServerLevel level, BlockPos pos) {
        BlockState hook = level.getBlockState(pos);
        if (!hook.is(Blocks.TRIPWIRE_HOOK)) return;
        Direction dir = hook.getValue(TripWireHookBlock.FACING);
        var wires = new ArrayList<BlockPos>();
        BlockPos other = null;
        boolean continuous = true;
        for (int n = 1; n < 42; n++) {
            BlockPos at = pos.relative(dir, n);
            if (!level.hasChunkAt(at)) return; // Revisited when the neighboring chunk loads.
            BlockState state = level.getBlockState(at);
            if (state.is(Blocks.TRIPWIRE_HOOK)) {
                if (state.getValue(TripWireHookBlock.FACING) == dir.getOpposite()) other = at;
                break;
            }
            if (state.is(Blocks.TRIPWIRE)) wires.add(at);
            else continuous = false;
        }
        boolean attached = other != null && continuous && !wires.isEmpty();
        boolean occupiedArmedWire = false;
        // Never feed template POWERED values into vanilla's hook recalculation:
        // it sends neighbor notifications even for an otherwise unchanged hook.
        for (BlockPos at : wires) {
            BlockState state = level.getBlockState(at).setValue(TripWireBlock.ATTACHED, attached);
            boolean occupied = !level.getEntitiesOfClass(Player.class,
                    state.getShape(level, at).bounds().move(at),
                    player -> player.isAlive() && !player.isSpectator() && !player.isIgnoringBlockTriggers()).isEmpty();
            level.setBlock(at, state.setValue(TripWireBlock.POWERED, occupied), QUIET);
            if (occupied) level.scheduleTick(at, state.getBlock(), 1);
            occupiedArmedWire |= occupied && !state.getValue(TripWireBlock.DISARMED);
        }
        level.setBlock(pos, hook.setValue(TripWireHookBlock.ATTACHED, attached)
                .setValue(TripWireHookBlock.POWERED, false), QUIET);
        if (other != null) {
            BlockState opposite = level.getBlockState(other);
            level.setBlock(other, opposite.setValue(TripWireHookBlock.ATTACHED, attached)
                    .setValue(TripWireHookBlock.POWERED, false), QUIET);
        }
        // Only real player contact may produce a redstone activation during repair.
        if (attached && occupiedArmedWire) {
            TripWireHookBlock.calculateState(level, pos, level.getBlockState(pos), false, true, -1, null);
        }
    }
}
