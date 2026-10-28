package net.beamex.shamaschizm.localportal.client;

import java.util.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.textures.FilterMode;
import net.beamex.shamaschizm.localportal.PortalTerrain;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.*;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Vanilla fluid geometry and sorted translucent block faces, in destination-relative coordinates. */
final class PortalFluidMesh implements AutoCloseable {
    private record Key(Identifier atlas,boolean translucent) {}
    private record Vertex(float x,float y,float z,int color,float u,float v,int light) {}
    private record Quad(Vertex[] vertices,Vec3 center) {}
    private static final class Group {
        final List<Quad> quads=new ArrayList<>();GpuBuffer buffer;int count;
    }
    private final Map<Key,Group> groups=new LinkedHashMap<>();
    private final PortalTerrain terrain;
    private final FluidRenderer renderer;
    private final FluidStateModelSet models;
    private Vec3 lastCamera;
    private int quadCount;
    PortalFluidMesh(PortalTerrain terrain) {
        this.terrain=terrain;models=Minecraft.getInstance().getModelManager().getFluidStateModelSet();renderer=new FluidRenderer(models);
    }
    void block(BlockAndTintGetter world,BlockPos pos,BlockState state) {
        var fluid=state.getFluidState();if(fluid.isEmpty())return;
        var model=models.get(fluid);var atlas=model.stillMaterial().sprite().atlasLocation();
        // FluidRenderer emits positions local to its 16x16x16 section, including at negative coordinates.
        var origin=terrain.target().origin();
        float dx=(float)((pos.getX()&~15)-origin.x),dy=(float)((pos.getY()&~15)-origin.y),dz=(float)((pos.getZ()&~15)-origin.z);
        var captures=new ArrayList<Capture>();
        try {renderer.tesselate(world,pos,layer->{var c=new Capture(new Key(atlas,layer.translucent()),dx,dy,dz);captures.add(c);return c;},state,fluid);}
        finally{for(var c:captures)c.close();}
    }
    void translucentBlock(float x,float y,float z,BakedQuad q,QuadInstance instance) {
        var vertices=new Vertex[4];
        for(int i=0;i<4;i++){var p=q.position(i);long uv=q.packedUV(i);
            vertices[i]=new Vertex(p.x()+x,p.y()+y,p.z()+z,ARGB.multiply(instance.getColor(i),q.bakedColors().color(i)),
                    UVPair.unpackU(uv),UVPair.unpackV(uv),instance.getLightCoordsWithEmission(i,q.materialInfo().lightEmission()));}
        add(new Key(q.materialInfo().sprite().atlasLocation(),true),vertices);
    }
    private void add(Key key,Vertex[] vertices) {
        if(++quadCount>100000)throw new IllegalStateException("Portal fluid/transparency budget exceeded");
        Vec3 center=Vec3.ZERO;for(var v:vertices)center=center.add(v.x,v.y,v.z);
        groups.computeIfAbsent(key,k->new Group()).quads.add(new Quad(vertices,center.scale(.25)));
    }
    void prepare(Vec3 virtualCamera) {
        Vec3 camera=virtualCamera.subtract(terrain.target().origin());
        boolean moved=lastCamera==null||lastCamera.distanceToSqr(camera)>.0625;
        for(var entry:groups.entrySet()) {
            var group=entry.getValue();if(group.buffer!=null&&(!entry.getKey().translucent||!moved))continue;
            if(entry.getKey().translucent)group.quads.sort(Comparator.comparingDouble((Quad q)->q.center.distanceToSqr(camera)).reversed());
            try(var memory=new ByteBufferBuilder(4096)) {
                var builder=new BufferBuilder(memory,PrimitiveTopology.TRIANGLES,DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP);
                for(var q:group.quads)for(int i:new int[]{0,1,2,0,2,3}){var v=q.vertices[i];
                    builder.addVertex(v.x,v.y,v.z).setColor(v.color).setUv(v.u,v.v).setLight(v.light);}
                try(var mesh=builder.build()) {
                    if(mesh!=null){var replacement=RenderSystem.getDevice().createBuffer(()->"Portal fluids",GpuBuffer.USAGE_VERTEX,mesh.vertexBuffer());
                        if(group.buffer!=null)group.buffer.close();group.buffer=replacement;group.count=mesh.drawState().vertexCount();}
                }
            }
        }
        if(moved)lastCamera=camera;
    }
    void draw(RenderPass pass,GpuBuffer uniform,boolean translucent) {
        var mc=Minecraft.getInstance();
        pass.setPipeline(RenderSystem.getCompiledPipeline(translucent?PortalViewPipelines.TRANSLUCENT_TERRAIN:PortalViewPipelines.TERRAIN));
        pass.setUniform("Projection",RenderSystem.getProjectionMatrixBuffer());pass.setUniform("PortalView",uniform);
        pass.setUniform("Sampler2",mc.gameRenderer.levelLightmap(),RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
        for(var entry:groups.entrySet())if(entry.getKey().translucent==translucent&&entry.getValue().buffer!=null) {
            var atlas=mc.getTextureManager().getTexture(entry.getKey().atlas);
            pass.setUniform("Sampler0",atlas.getTextureView(),RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
            var group=entry.getValue();pass.setVertexBuffer(0,group.buffer.slice());pass.draw(group.count,1,0,0);
        }
    }
    public void close(){for(var group:groups.values())if(group.buffer!=null)group.buffer.close();groups.clear();}
    private final class Capture implements VertexConsumer,AutoCloseable {
        final Key key;final float dx,dy,dz;Vertex[] vertices=new Vertex[4];int count;boolean pending;
        float x,y,z,u,v;int color,light;
        Capture(Key key,float dx,float dy,float dz){this.key=key;this.dx=dx;this.dy=dy;this.dz=dz;}
        private void finish(){if(!pending)return;vertices[count++]=new Vertex(x+dx,y+dy,z+dz,color,u,v,light);
            if(count==4){add(key,vertices);vertices=new Vertex[4];count=0;}pending=false;}
        public VertexConsumer addVertex(float x,float y,float z){finish();this.x=x;this.y=y;this.z=z;color=-1;light=0;u=v=0;pending=true;return this;}
        public VertexConsumer setColor(int r,int g,int b,int a){color=(a<<24)|(r<<16)|(g<<8)|b;return this;}
        public VertexConsumer setColor(int c){color=c;return this;}
        public VertexConsumer setUv(float u,float v){this.u=u;this.v=v;return this;}
        public VertexConsumer setUv1(int u,int v){return this;}
        public VertexConsumer setUv2(int u,int v){light=(u&65535)|((v&65535)<<16);return this;}
        public VertexConsumer setUv3(float u,float v){return this;}
        public VertexConsumer setNormal(float x,float y,float z){return this;}
        public VertexConsumer setLineWidth(float w){return this;}
        public void close(){finish();}
    }
}
