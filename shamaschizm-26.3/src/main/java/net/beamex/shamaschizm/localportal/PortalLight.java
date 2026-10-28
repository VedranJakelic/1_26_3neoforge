package net.beamex.shamaschizm.localportal;

import java.util.*;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.world.Schizm;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Bounded block-light relay. Only owns its own invisible blocks; never replaces terrain. */
@EventBusSubscriber(modid="shamaschizm")
public final class PortalLight {
    private static LightBlock light;
    private static final Map<ServerLevel,Map<BlockPos,Integer>> DESIRED=new WeakHashMap<>();
    private record Report(int incoming,int unloaded,int blocked,Map<BlockPos,Integer> cells) {}
    private static final Map<ServerLevel,Map<String,Report>> REPORTS=new WeakHashMap<>();
    public static String describe(ServerLevel level,String name) {
        Report report=REPORTS.getOrDefault(level,Map.of()).get(name);
        if(report==null)return "No active light relay for " + name + ". Enable its pair and wait half a second.";
        int placed=0,emission=0,received=0;
        for(var pos:report.cells.keySet())if(level.hasChunkAt(pos)) {
            var state=level.getBlockState(pos);
            if(state.is(light)){placed++;emission=Math.max(emission,state.getValue(LightBlock.LEVEL));}
            received=Math.max(received,level.getBrightness(LightLayer.BLOCK,pos));
        }
        return name+": source block light="+report.incoming+", requested cells="+report.cells.size()
                +", placed="+placed+", emission="+emission+", received light="+received
                +", unloaded samples="+report.unloaded+", blocked samples="+report.blocked;
    }
    private static boolean writable(ServerLevel level,BlockPos pos) {
        var state=level.getBlockState(pos);return state.isAir()||state.is(light);
    }
    private static int sample(ServerLevel level,BlockPos pos) {
        var state=level.getBlockState(pos);
        int emission=state.getLightEmission(level,pos);
        return state.getCollisionShape(level,pos).isEmpty()
                ? Math.max(emission,level.getBrightness(LightLayer.BLOCK,pos)) : emission;
    }
    @SubscribeEvent public static void register(RegisterEvent event) {
        event.register(Registries.BLOCK,helper->{
            var id=Shamaschizm.id("local_portal_light");
            light=new RelayBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK,id))
                    .replaceable().noCollision().noOcclusion().noLootTable().strength(0)
                    .lightLevel(s->s.getValue(LightBlock.LEVEL)).isValidSpawn((s,l,p,e)->false));
            helper.register(id,light);
        });
    }
    @SubscribeEvent public static void tick(LevelTickEvent.Post event) {
        if(!(event.getLevel() instanceof ServerLevel level) || !level.dimension().equals(Schizm.KEY)
                || level.getGameTime()%10!=0)return;
        var wanted=new HashMap<BlockPos,Integer>();
        var data=DoorwayData.get(level);
        var reports=new HashMap<String,Report>();
        // Read both directions before writing any relays. A two-level loss prevents light feedback
        // from sustaining itself after a real light source is removed.
        for(var source:data.all()) {
            var target=data.get(source.target());
            if(!source.enabled() || target==null || !target.enabled() || !target.target().equals(source.name()))continue;
            var cells=new HashMap<BlockPos,Integer>();int incoming=0,unloaded=0,blocked=0;
            for(double x=-source.width()/2+.5;x<source.width()/2;x+=1)
                for(double y=.5;y<source.height();y+=1) {
                    var plane=BlockPos.containing(source.world(x,y,0));
                    var front=BlockPos.containing(source.world(x,y,.51));
                    var to=BlockPos.containing(target.world(-x,y,0));
                    var out=BlockPos.containing(target.world(-x,y,.51));
                    if(!level.hasChunkAt(plane)||!level.hasChunkAt(front)||!level.hasChunkAt(to)||!level.hasChunkAt(out)) {unloaded++;continue;}
                    int value=Math.max(sample(level,plane),sample(level,front));
                    incoming=Math.max(incoming,value);
                    if(!writable(level,to))to=out;
                    if(!writable(level,to)){blocked++;continue;}
                    value=Math.max(0,value-2);
                    if(value>0){wanted.merge(to,value,Math::max);cells.merge(to,value,Math::max);}
                }
            reports.put(source.name(),new Report(incoming,unloaded,blocked,Map.copyOf(cells)));
        }
        REPORTS.put(level,reports);
        var old=DESIRED.put(level,wanted);
        if(old!=null)for(var pos:old.keySet())if(!wanted.containsKey(pos) && level.hasChunkAt(pos)
                && level.getBlockState(pos).is(light))level.setBlock(pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS);
        for(var entry:wanted.entrySet()) {
            var pos=entry.getKey();var state=level.getBlockState(pos);
            if(!state.isAir()&&!state.is(light))continue;
            var next=light.defaultBlockState().setValue(LightBlock.LEVEL,entry.getValue());
            if(!state.equals(next)) {
                level.setBlock(pos,next,Block.UPDATE_CLIENTS);
                level.getChunkSource().getLightEngine().checkBlock(pos);
            }
            level.scheduleTick(pos,light,20);
        }
    }
    private static final class RelayBlock extends LightBlock {
        RelayBlock(Properties p){super(p);}
        @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random) {
            int wanted=DESIRED.getOrDefault(level,Map.of()).getOrDefault(pos,0);
            if(wanted<=0)level.setBlock(pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS);
            else {if(state.getValue(LEVEL)!=wanted)level.setBlock(pos,state.setValue(LEVEL,wanted),Block.UPDATE_CLIENTS);
                level.scheduleTick(pos,this,20);}
        }
    }
}
