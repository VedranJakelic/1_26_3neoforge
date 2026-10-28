package net.beamex.shamaschizm.localportal.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.pipeline.*;
import net.beamex.shamaschizm.Shamaschizm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

@EventBusSubscriber(modid="shamaschizm",value=Dist.CLIENT)
public final class PortalViewPipelines {
    public static final RenderPipeline TERRAIN=RenderPipeline.builder()
            .withLocation(Shamaschizm.id("pipeline/portal_terrain"))
            .withVertexShader(Shamaschizm.id("core/portal_terrain"))
            .withFragmentShader(Shamaschizm.id("core/portal_terrain"))
            .withBindGroupLayout(BindGroupLayout.builder().withUniform("Projection",UniformType.UNIFORM_BUFFER)
                    .withUniform("PortalView",UniformType.UNIFORM_BUFFER)
                    .withUniform("Sampler0",UniformType.COMBINED_IMAGE_SAMPLER)
                    .withUniform("Sampler2",UniformType.COMBINED_IMAGE_SAMPLER).build())
            .withVertexBinding(0,DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES).withCull(false)
            .withColorTargetState(ColorTargetState.DEFAULT).withDepthStencilState(DepthStencilState.DEFAULT).build();
    public static final RenderPipeline TRANSLUCENT_TERRAIN=RenderPipeline.builder()
            .withLocation(Shamaschizm.id("pipeline/portal_translucent_terrain"))
            .withVertexShader(Shamaschizm.id("core/portal_terrain"))
            .withFragmentShader(Shamaschizm.id("core/portal_translucent_terrain"))
            .withBindGroupLayout(BindGroupLayout.builder().withUniform("Projection",UniformType.UNIFORM_BUFFER)
                    .withUniform("PortalView",UniformType.UNIFORM_BUFFER)
                    .withUniform("Sampler0",UniformType.COMBINED_IMAGE_SAMPLER)
                    .withUniform("Sampler2",UniformType.COMBINED_IMAGE_SAMPLER).build())
            .withVertexBinding(0,DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES).withCull(false)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(DepthStencilState.DEFAULT.depthTest(),false)).build();
    public static final RenderPipeline SURFACE=RenderPipeline.builder()
            .withLocation(Shamaschizm.id("pipeline/portal_surface"))
            .withVertexShader(Shamaschizm.id("core/portal_surface"))
            .withFragmentShader(Shamaschizm.id("core/portal_surface"))
            .withBindGroupLayout(BindGroupLayout.builder().withUniform("Projection",UniformType.UNIFORM_BUFFER)
                    .withUniform("PortalView",UniformType.UNIFORM_BUFFER)
                    .withUniform("Sampler0",UniformType.COMBINED_IMAGE_SAMPLER).build())
            .withVertexBinding(0,DefaultVertexFormat.POSITION).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withCull(false).withColorTargetState(ColorTargetState.DEFAULT)
            .withDepthStencilState(DepthStencilState.DEFAULT).build();
    public static final RenderPipeline ENTITY=entity(false), ENTITY_TRANSLUCENT=entity(true);
    private static RenderPipeline entity(boolean translucent) {
        return RenderPipeline.builder().withLocation(Shamaschizm.id(translucent?"pipeline/portal_entity_translucent":"pipeline/portal_entity"))
                .withVertexShader(Shamaschizm.id("core/portal_entity")).withFragmentShader(Shamaschizm.id("core/portal_entity"))
                .withBindGroupLayout(BindGroupLayout.builder().withUniform("Projection",UniformType.UNIFORM_BUFFER)
                        .withUniform("PortalView",UniformType.UNIFORM_BUFFER).withUniform("EntityMaterial",UniformType.UNIFORM_BUFFER)
                        .withUniform("Sampler0",UniformType.COMBINED_IMAGE_SAMPLER)
                        .withUniform("Sampler1",UniformType.COMBINED_IMAGE_SAMPLER)
                        .withUniform("Sampler2",UniformType.COMBINED_IMAGE_SAMPLER).build())
                .withVertexBinding(0,DefaultVertexFormat.ENTITY).withPrimitiveTopology(PrimitiveTopology.TRIANGLES).withCull(false)
                .withColorTargetState(translucent?new ColorTargetState(BlendFunction.TRANSLUCENT):ColorTargetState.DEFAULT)
                .withDepthStencilState(new DepthStencilState(DepthStencilState.DEFAULT.depthTest(),!translucent)).build();
    }
    @SubscribeEvent public static void register(RegisterRenderPipelinesEvent event){
        event.registerPipeline(TERRAIN);event.registerPipeline(TRANSLUCENT_TERRAIN);event.registerPipeline(SURFACE);
        event.registerPipeline(ENTITY);event.registerPipeline(ENTITY_TRANSLUCENT);
    }
}
