package net.beamex.shamaschizm.localportal.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import java.util.*;
import net.beamex.shamaschizm.localportal.PortalTerrain;
import net.beamex.shamaschizm.localportal.PortalLighting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.*;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;

/** Incrementally tessellates the snapshot with vanilla block models, culling, tint and lighting. */
final class PortalTerrainMesh implements AutoCloseable {
    record Batch(Identifier atlas,GpuBuffer vertices,int count) {}
    final PortalTerrain terrain;
    final PortalFluidMesh fluids;
    final PortalBlockEntityScene blockEntities;
    final List<Batch> batches=new ArrayList<>();
    private final Map<Identifier,Build> building=new LinkedHashMap<>();
    private final ModelBlockRenderer renderer;
    private final Snapshot world;
    private final Snapshot seamWorld;
    private final List<int[]> destinationColors = new ArrayList<>();
    private int seamQuad;
    private boolean blendingSeam;
    private int cursor,quads;
    boolean ready;
    private static final class Build implements AutoCloseable {
        final ByteBufferBuilder memory=new ByteBufferBuilder(65536);
        final BufferBuilder vertices=new BufferBuilder(memory,PrimitiveTopology.TRIANGLES,DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP);
        public void close(){memory.close();}
    }
    PortalTerrainMesh(PortalTerrain terrain){
        this.terrain=terrain;fluids=new PortalFluidMesh(terrain);blockEntities=new PortalBlockEntityScene(terrain);world=new Snapshot(terrain,PortalLighting.oriented(Minecraft.getInstance().level.cardinalLighting(),terrain.source(),terrain.target()));
        seamWorld=new Snapshot(terrain,PortalLighting.atAperture(world.cardinalLighting(),terrain.source(),terrain.target()));
        renderer=new ModelBlockRenderer(Minecraft.getInstance().options.ambientOcclusion().get(),true,Minecraft.getInstance().getBlockColors());
    }
    void advance(long deadline){
        var models=Minecraft.getInstance().getModelManager().getBlockStateModelSet();
        var pos=new BlockPos.MutableBlockPos();
        while(cursor<terrain.states().length){
            int i=cursor++;
            int x=i%terrain.sizeX(),z=(i/terrain.sizeX())%terrain.sizeZ(),y=i/(terrain.sizeX()*terrain.sizeZ());
            pos.set(terrain.min().getX()+x,terrain.min().getY()+y,terrain.min().getZ()+z);
            BlockState state=Block.stateById(terrain.states()[i]);
            if(!state.isAir()&&state.getRenderShape()==RenderShape.MODEL){
                var origin=terrain.target().origin();
                float bx=(float)(pos.getX()-origin.x), by=(float)(pos.getY()-origin.y), bz=(float)(pos.getZ()-origin.z);
                var local=terrain.target().local(net.minecraft.world.phys.Vec3.atCenterOf(pos));
                boolean nearSeam=local.z>=-1 && local.z<2.5
                        && Math.abs(local.x)<=terrain.target().width()/2+1
                        && local.y>=-1 && local.y<=terrain.target().height()+1;
                if(nearSeam && !world.cardinalLighting().equals(seamWorld.cardinalLighting())) {
                    destinationColors.clear();
                    // Both passes use identical geometry, state, seed and light samples.
                    // Let vanilla apply shade flags and AO; only interpolate its final colors.
                    renderer.tesselateBlock((qx,qy,qz,q,instance)-> {
                        destinationColors.add(new int[]{instance.getColor(0),instance.getColor(1),instance.getColor(2),instance.getColor(3)});
                        if(q.materialInfo().layer().translucent())fluids.translucentBlock(qx,qy,qz,q,instance);
                    },
                            bx,by,bz,world,pos,state,models.get(state),state.getSeed(pos));
                    seamQuad=0;blendingSeam=true;
                    try {
                        renderer.tesselateBlock(this::quad,bx,by,bz,seamWorld,pos,state,models.get(state),state.getSeed(pos));
                    } finally { blendingSeam=false;destinationColors.clear(); }
                } else {
                    renderer.tesselateBlock(this::quad,bx,by,bz,world,pos,state,models.get(state),state.getSeed(pos));
                }
            }
            fluids.block(world,pos,state);
            if((cursor&63)==0&&System.nanoTime()>=deadline)return;
        }
        for(var entry:building.entrySet()){
            try(var mesh=entry.getValue().vertices.build()){
                if(mesh!=null)batches.add(new Batch(entry.getKey(),RenderSystem.getDevice().createBuffer(
                        ()->"Schizm portal terrain",GpuBuffer.USAGE_VERTEX,mesh.vertexBuffer()),mesh.drawState().vertexCount()));
            }
            entry.getValue().close();
        }
        building.clear();ready=true;
    }
    private void quad(float x,float y,float z,BakedQuad q,QuadInstance instance){
        int[] nativeColors=blendingSeam?destinationColors.get(seamQuad++):null;
        if(++quads>100000)throw new IllegalStateException("Portal mesh exceeds 100,000 quads");
        if(q.materialInfo().layer().translucent()){
            if(nativeColors==null)fluids.translucentBlock(x,y,z,q,instance);
            return;
        }
        var builder=building.computeIfAbsent(q.materialInfo().sprite().atlasLocation(),k->new Build()).vertices;
        for(int v:new int[]{0,1,2,0,2,3}){
            var p=q.position(v);long uv=q.packedUV(v);
            int color=instance.getColor(v);
            if(nativeColors!=null) {
                var normal=terrain.target().facing().normal;
                double depth=(p.x()+x)*normal.x+(p.z()+z)*normal.z;
                color=PortalLighting.vertexColor(color,nativeColors[v],depth);
            }
            builder.addVertex(p.x()+x,p.y()+y,p.z()+z)
                    .setColor(ARGB.multiply(color,q.bakedColors().color(v)))
                    .setUv(UVPair.unpackU(uv),UVPair.unpackV(uv))
                    .setLight(instance.getLightCoordsWithEmission(v,q.materialInfo().lightEmission()));
        }
    }
    public void close(){
        fluids.close();
        for(var b:batches)b.vertices.close();batches.clear();
        for(var b:building.values())b.close();building.clear();
    }
    private record Snapshot(PortalTerrain data,CardinalLighting cardinalLighting) implements BlockAndTintGetter {
        public BlockState getBlockState(BlockPos p){int i=data.index(p);return i<0?Blocks.AIR.defaultBlockState():Block.stateById(data.states()[i]);}
        public FluidState getFluidState(BlockPos p){return getBlockState(p).getFluidState();}
        public BlockEntity getBlockEntity(BlockPos p){return null;}
        public int getHeight(){return data.sizeY();}
        public int getMinY(){return data.min().getY();}
        public int getBrightness(LightLayer layer,BlockPos p){
            int i=data.index(p);if(i<0)return 0;int light=data.lights()[i]&255;
            return layer==LightLayer.BLOCK?light&15:light>>4;
        }
        public int getRawBrightness(BlockPos p,int skyDarken){return Math.max(getBrightness(LightLayer.BLOCK,p),getBrightness(LightLayer.SKY,p)-skyDarken);}
        public LevelLightEngine getLightEngine(){return Minecraft.getInstance().level.getLightEngine();}
        public int getBlockTint(BlockPos p,ColorResolver resolver){
            var mc=Minecraft.getInstance();
            // Schizm uses one biome. Biome palettes for multi-biome portals are not needed here.
            return resolver.getColor(mc.level.getBiome(mc.player.blockPosition()).value(),p.getX(),p.getZ());
        }
    }
}
