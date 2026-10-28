package net.beamex.shamaschizm.localportal;

import java.util.*;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.world.Schizm;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid="shamaschizm")
public final class PortalTerrainServer {
    // Loading-only, nonpersistent, expires two seconds after the last viewer leaves.
    private static final TicketType VIEW_TICKET=new TicketType(40,TicketType.FLAG_LOADING);
    private static final Map<ServerPlayer,Map<String,PortalTerrain>> SENT=new WeakHashMap<>();
    private static final Map<ServerLevel,Map<String,Cached>> CACHE=new WeakHashMap<>();
    private static final Set<ServerPlayer> DIRTY=Collections.newSetFromMap(new WeakHashMap<>());
    public static void refresh(ServerPlayer player){DIRTY.add(player);CACHE.remove(player.level());}
    private record Cached(long tick, PortalTerrain terrain) {}
    @SubscribeEvent public static void register(RegisterEvent event){
        event.register(Registries.TICKET_TYPE,Shamaschizm.id("local_portal_view"),()->VIEW_TICKET);
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event){
        if(!(event.getEntity() instanceof ServerPlayer p))return;
        if(!p.level().dimension().equals(Schizm.KEY)||!p.isAlive()){SENT.remove(p);DIRTY.remove(p);return;}
        if(p.tickCount%20!=0&&!(p.tickCount%4==0&&DIRTY.contains(p)))return;
        DIRTY.remove(p);
        var data=DoorwayData.get(p.level());
        var selected=data.all().stream().filter(d->d.enabled()&&d.distance(p.getEyePosition())>-.5
                &&d.origin().distanceToSqr(p.position())<24*24)
                .sorted(Comparator.comparingDouble(d->d.origin().distanceToSqr(p.position()))).limit(2).toList();
        var sent=SENT.computeIfAbsent(p,k->new HashMap<>());
        var names=new HashSet<String>();
        for(var source:selected){
            names.add(source.name());var target=data.get(source.target());
            if(target==null||!target.enabled()||!target.target().equals(source.name()))continue;
            PortalTerrain terrain=snapshot(p.level(),source,target);
            if(terrain==null)continue;
            PortalTerrain old=sent.get(source.name());
            if(old==null||p.tickCount%100==0||!old.source().equals(source)||!old.target().equals(target)
                    ||!old.blockEntities().equals(terrain.blockEntities())||!Arrays.equals(old.states(),terrain.states())||!Arrays.equals(old.lights(),terrain.lights())){
                PacketDistributor.sendToPlayer(p,terrain);sent.put(source.name(),terrain);
            }
        }
        sent.keySet().retainAll(names);
    }
    private static PortalTerrain snapshot(ServerLevel level,Doorway source,Doorway target){
        var cache=CACHE.computeIfAbsent(level,k->new HashMap<>());
        long tick=level.getGameTime();
        cache.entrySet().removeIf(e->tick-e.getValue().tick>40);
        Cached cached=cache.get(source.name());
        if(cached!=null&&tick-cached.tick<20&&cached.terrain.source().equals(source)&&cached.terrain.target().equals(target))return cached.terrain;
        // Neighbour padding is retained for face culling and ambient light sampling.
        double side=target.width()/2+16;
        var a=target.world(-side,-16,-1);var b=target.world(side,target.height()+16,16);
        BlockPos min=BlockPos.containing(Math.min(a.x,b.x),Math.min(a.y,b.y),Math.min(a.z,b.z));
        BlockPos max=BlockPos.containing(Math.max(a.x,b.x),Math.max(a.y,b.y),Math.max(a.z,b.z));
        boolean ready=true;
        for(int x=min.getX()>>4;x<=max.getX()>>4;x++)for(int z=min.getZ()>>4;z<=max.getZ()>>4;z++){
            level.getChunkSource().addTicketWithRadius(VIEW_TICKET,new ChunkPos(x,z),0);
            if(level.getChunkSource().getChunkNow(x,z)==null)ready=false;
        }
        if(!ready)return null; // Do not synchronously generate distant terrain in a player tick.
        int sx=max.getX()-min.getX()+1,sy=max.getY()-min.getY()+1,sz=max.getZ()-min.getZ()+1;
        int[] states=new int[sx*sy*sz];byte[] lights=new byte[states.length];
        var pos=new BlockPos.MutableBlockPos();
        for(int y=0;y<sy;y++)for(int z=0;z<sz;z++)for(int x=0;x<sx;x++){
            pos.set(min.getX()+x,min.getY()+y,min.getZ()+z);int i=(y*sz+z)*sx+x;
            states[i]=Block.getId(level.getBlockState(pos));
            int block=level.getBrightness(LightLayer.BLOCK,pos),sky=level.getBrightness(LightLayer.SKY,pos);
            // Supply the same lighting field on both sides of the rendered aperture. Include
            // one neighbour cell around its rim because vanilla smooth lighting samples neighbours.
            var local=target.local(net.minecraft.world.phys.Vec3.atCenterOf(pos));
            if(local.z<2 && Math.abs(local.x)<=target.width()/2+1 && local.y>=-1 && local.y<=target.height()+1) {
                var sourcePos=BlockPos.containing(source.world(-local.x,local.y,Math.max(0,-local.z)));
                if(level.hasChunkAt(sourcePos)) {
                    block=PortalLighting.boundary(level.getBrightness(LightLayer.BLOCK,sourcePos),block,local.z);
                    sky=PortalLighting.boundary(level.getBrightness(LightLayer.SKY,sourcePos),sky,local.z);
                }
            }
            lights[i]=(byte)(block|(sky<<4));
        }
        var appearances=new ArrayList<PortalBlockAppearance>();
        var candidates=new ArrayList<BlockPos>();
        for(int i=0;i<states.length;i++)if(Block.stateById(states[i]).hasBlockEntity()) {
            var at=min.offset(i%sx,i/(sx*sz),(i/sx)%sz);
            double depth=target.distance(net.minecraft.world.phys.Vec3.atCenterOf(at));
            if(depth>=-1&&depth<=Doorway.VIEW_DEPTH+1)candidates.add(at);
        }
        candidates.sort(Comparator.comparingDouble(at->at.distToCenterSqr(target.origin())));
        for(var at:candidates) {
            var entity=level.getBlockEntity(at);if(entity==null)continue;
            try {
                var tag=entity.getUpdateTag(level.registryAccess()).copy();
                if(entity instanceof net.minecraft.world.level.block.entity.BeaconBeamOwner beacon) {
                    var sections=beacon.getBeamSections();int count=Math.min(64,sections.size());
                    int[] beam=new int[count*2];
                    for(int j=0;j<count;j++){beam[j*2]=sections.get(j).getColor();beam[j*2+1]=Math.clamp(sections.get(j).getHeight(),1,4096);}
                    tag.putIntArray("shamaschizm:portal_beam_sections",beam);
                }
                if(PortalBlockAppearance.tagBytes(tag).length>PortalBlockAppearance.MAX_TAG_BYTES)continue;
                int i=((at.getY()-min.getY())*sz+at.getZ()-min.getZ())*sx+at.getX()-min.getX();
                float open=entity instanceof net.minecraft.world.level.block.entity.ChestBlockEntity
                    &&net.minecraft.world.level.block.entity.ChestBlockEntity.getOpenCount(level,at)>0?1:0;
                appearances.add(new PortalBlockAppearance(i,net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(entity.getType()),tag,open));
            }catch(RuntimeException ignored) { /* An unsupported update tag must not break terrain streaming. */ }
            if(appearances.size()==PortalBlockAppearance.LIMIT)break;
        }
        var result=new PortalTerrain(source,target,min,sx,sy,sz,states,lights,List.copyOf(appearances));
        cache.put(source.name(),new Cached(tick,result));return result;
    }
}
