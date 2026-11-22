package net.beamex.shamaschizm.eclipse;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.*;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.MoonPhase;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/** Renders vanilla Moon-phase artwork at the physical lunar direction. */
@EventBusSubscriber(modid="shamaschizm",value=Dist.CLIENT)
public final class EclipseMoonRenderer implements AutoCloseable {
    public static final RenderPipeline PIPELINE=pipeline(false);
    public static final RenderPipeline GLOW=pipeline(true);
    private static RenderPipeline pipeline(boolean glow){return RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("shamaschizm",glow?"pipeline/orbital_moon_glow":"pipeline/orbital_moon"))
            .withVertexShader(Identifier.fromNamespaceAndPath("shamaschizm","core/orbital_moon"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("shamaschizm","core/orbital_moon"))
            .withBindGroupLayout(BindGroupLayout.builder().withUniform("Projection",UniformType.UNIFORM_BUFFER)
                    .withUniform("DynamicTransforms",UniformType.UNIFORM_BUFFER)
                    .withUniform("Sampler0",UniformType.COMBINED_IMAGE_SAMPLER).build())
            .withVertexBinding(0,DefaultVertexFormat.POSITION_TEX_COLOR).withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withCull(false).withColorTargetState(new ColorTargetState(glow?BlendFunction.ADDITIVE:BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS,false)).build();}
    @SubscribeEvent public static void register(RegisterRenderPipelinesEvent e){e.registerPipeline(PIPELINE);e.registerPipeline(GLOW);}
    private GpuBuffer buffer;
    public void render(RenderPass pass,TextureAtlas atlas,EclipseSkyData.View view,float weather,MoonPhase phase){
        var sky=view.sky();var d=sky.moon();
        var right=new EclipseOrbit.V(d.y(),-d.x(),0).unit();
        var up=d.cross(right).unit();
        // Vanilla artwork occupies the central quarter of its texture. Preserve the orbital body size.
        double radius=400*Math.tan(sky.moonRadius());
        Matrix4f model=new Matrix4f().m00((float)(right.x()*radius)).m01((float)(right.y()*radius)).m02((float)(right.z()*radius))
                .m10((float)(up.x()*radius)).m11((float)(up.y()*radius)).m12((float)(up.z()*radius))
                .m20((float)-d.x()).m21((float)-d.y()).m22((float)-d.z())
                .m30((float)(100*d.x())).m31((float)(100*d.y())).m32((float)(100*d.z()));
        Matrix4f transform=new Matrix4f(RenderSystem.getModelViewStack()).mul(model);
        var sprite=atlas.getSprite(Identifier.withDefaultNamespace("moon/"+phase.getSerializedName()));
        close();
        try(ByteBufferBuilder bytes=ByteBufferBuilder.exactlySized(4*24)){
            BufferBuilder b=new BufferBuilder(bytes,PrimitiveTopology.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);
            for(int i=0;i<4;i++){
                float x=i==0||i==3?-1:1,y=i<2?-1:1;
                float u=.5F+x*.5F,v=.5F-y*.5F;
                b.addVertex(x,y,0).setUv(sprite.getU(u),sprite.getV(v)).setColor(view.r(),view.g(),view.b(),1F);
            }
            try(MeshData mesh=b.buildOrThrow()){
                buffer=RenderSystem.getDevice().createBuffer(()->"Vanilla orbital Moon",GpuBuffer.USAGE_VERTEX,mesh.vertexBuffer());
            }
        }
        var indices=RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
        for(int layer=0;layer<2;layer++){
            var uniform=RenderSystem.getDynamicUniforms().writeTransform(transform,new Vector4f(layer,0,0,weather));
            pass.setPipeline(RenderSystem.getCompiledPipeline(layer==0?PIPELINE:GLOW));
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms",uniform);
            pass.setUniform("Sampler0",atlas.getTextureView(),atlas.getSampler());
            pass.setVertexBuffer(0,buffer.slice());pass.setIndexBuffer(indices.getBuffer(6),indices.type());
            pass.drawIndexed(6,1,0,0,0);
        }
    }
    public void close(){if(buffer!=null){buffer.close();buffer=null;}}
}
