package net.beamex.shamaschizm.localportal.client;

import java.nio.*;
import java.util.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.textures.FilterMode;
import net.beamex.shamaschizm.localportal.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.*;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.*;
import net.minecraft.client.renderer.state.level.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Quaternionf;

/** Captures standard model/item/custom-quad submits into a portal-clipped geometry pass. */
final class PortalEntityMesh implements SubmitNodeCollector,AutoCloseable {
    private static final int MAX_VERTICES=300_000;
    private final Map<RenderType,Group> groups=new LinkedHashMap<>();
    private int vertices;
    private static final class Group implements AutoCloseable {
        final RenderType type;
        final PreparedRenderType.Texture texture;
        final ByteBufferBuilder memory;
        final BufferBuilder builder;
        final GpuBuffer material;
        GpuBuffer buffer;int count;
        Group(RenderType type) {
            this.type=type;
            texture=type.prepare().textures().stream().filter(t->t.name().equals("Sampler0")).findFirst().orElseThrow();
            var flags=type.pipeline().getShaderDefines();
            float cutoff=.1f;
            try{cutoff=Float.parseFloat(flags.values().getOrDefault("ALPHA_CUTOUT",type.hasBlending()?"0.003":"0.1"));}catch(NumberFormatException ignored){}
            var format=type.pipeline().getVertexFormatBinding(0);
            var bytes=ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder());
            bytes.putFloat(flags.flags().contains("NO_CARDINAL_LIGHTING")||!format.contains(DefaultVertexFormat.NORMAL_SEMANTIC_NAME)?0:1)
                .putFloat(flags.flags().contains("EMISSIVE")||!format.contains(DefaultVertexFormat.UV2_SEMANTIC_NAME)?1:0)
                .putFloat(flags.flags().contains("NO_OVERLAY")||!format.contains(DefaultVertexFormat.UV1_SEMANTIC_NAME)?0:1).putFloat(cutoff)
                .putFloat(flags.flags().contains("IS_GRAYSCALE")?1:0).putFloat(0).putFloat(0).putFloat(0).flip();
            material=RenderSystem.getDevice().createBuffer(()->"Portal entity material",GpuBuffer.USAGE_UNIFORM,bytes);
            memory=new ByteBufferBuilder(4096);
            builder=new BufferBuilder(memory,PrimitiveTopology.TRIANGLES,DefaultVertexFormat.ENTITY);
        }
        void upload() {
            try(var mesh=builder.build()) {if(mesh!=null){count=mesh.drawState().vertexCount();
                buffer=RenderSystem.getDevice().createBuffer(()->"Portal entity vertices",GpuBuffer.USAGE_VERTEX,mesh.vertexBuffer());}}
        }
        public void close(){if(buffer!=null)buffer.close();material.close();memory.close();}
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    static PortalEntityMesh build(PortalTerrain terrain,Vec3 camera,float partialTick,PortalBlockEntityScene blocks) {
        var out=new PortalEntityMesh();var mc=Minecraft.getInstance();var dispatcher=mc.getEntityRenderDispatcher();
        var virtualCamera=new CameraRenderState();virtualCamera.pos=terrain.source().transformPoint(terrain.target(),camera);
        virtualCamera.initialized=true;virtualCamera.isFirstPerson=true;
        // Billboards should face the observer mapped to the destination room.
        var real=mc.gameRenderer.gameRenderState().levelRenderState.cameraRenderState;
        double yaw=Math.toRadians(real.yRot);
        Vec3 look=terrain.source().transformVector(terrain.target(),new Vec3(-Math.sin(yaw),0,Math.cos(yaw)));
        virtualCamera.yRot=(float)Math.toDegrees(Math.atan2(-look.x,look.z));virtualCamera.xRot=real.xRot;
        virtualCamera.orientation=new Quaternionf().rotationYXZ((float)Math.toRadians(-virtualCamera.yRot),(float)Math.toRadians(virtualCamera.xRot),0);
        var visuals=new ArrayList<>(PortalEntityScene.visuals(terrain));
        visuals.sort(Comparator.comparingDouble((PortalEntityScene.Visual v)->v.entity().position().distanceToSqr(virtualCamera.pos)).reversed());
        for(var visual:visuals)try {
            var entity=visual.entity();
            if(entity.isInvisibleTo(mc.player))continue;
            var state=dispatcher.extractEntity(entity,visual.replica()?1:partialTick);
            state.lightCoords=visual.light();state.shadowPieces.clear();state.shadowRadius=0;state.nameTag=null;state.leashStates=null;
            if(visual.replica())state.ageInTicks=entity.tickCount;
            var renderer=dispatcher.getRenderer(state);
            Vec3 offset=renderer.getRenderOffset(state);
            var pose=new PoseStack();pose.translate(state.x-terrain.target().origin().x+offset.x,
                    state.y-terrain.target().origin().y+offset.y,state.z-terrain.target().origin().z+offset.z);
            renderer.submit(state,pose,out,virtualCamera);
            if(state.displayFireAnimation)out.submitFlame(pose,state,net.minecraft.util.Mth.rotationAroundAxis(
                    net.minecraft.util.Mth.Y_AXIS,virtualCamera.orientation,new Quaternionf()));
        }catch(RuntimeException failure){PortalEntityScene.warn(visual.entity().getType().toString(),failure);}
        blocks.submit(out,virtualCamera,partialTick);
        try {for(var g:out.groups.values())g.upload();return out;}
        catch(RuntimeException failure){out.close();throw failure;}
    }
    void draw(RenderPass pass,GpuBuffer sceneUniform) {
        var mc=Minecraft.getInstance();
        for(boolean translucent:new boolean[]{false,true})for(var group:groups.values()) {
            if(group.buffer==null||group.type.hasBlending()!=translucent)continue;
            pass.setPipeline(RenderSystem.getCompiledPipeline(translucent?PortalViewPipelines.ENTITY_TRANSLUCENT:PortalViewPipelines.ENTITY));
            pass.setUniform("Projection",RenderSystem.getProjectionMatrixBuffer());pass.setUniform("PortalView",sceneUniform);
            pass.setUniform("EntityMaterial",group.material);
            pass.setUniform("Sampler0",group.texture.textureView(),group.texture.sampler());
            pass.setUniform("Sampler1",mc.gameRenderer.overlayTexture().getTextureView(),RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
            pass.setUniform("Sampler2",mc.gameRenderer.levelLightmap(),RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            pass.setVertexBuffer(0,group.buffer.slice());pass.draw(group.count,1,0,0);
        }
    }
    private Capture capture(RenderType type,PrimitiveTopology topology) {
        return new Capture(groups.computeIfAbsent(type,Group::new),topology);
    }
    public OrderedSubmitNodeCollector order(int order){return this;}
    @Override public <S> void submitModel(Model<? super S> model,S state,PoseStack pose,RenderType type,int light,int overlay,int color,UvMapping uv,int outline) {
        model.setupAnim(state);
        try(var capture=capture(type,PrimitiveTopology.QUADS)) {
            model.renderToBuffer(pose,uv==null?capture:uv.wrap(capture),light,overlay,color);
        }
    }
    @Override public void submitCustomGeometry(PoseStack pose,RenderType type,CustomGeometryRenderer renderer) {
        try(var capture=capture(type,type.primitiveTopology())){renderer.render(pose.last(),capture);}
    }
    @Override public void submitItem(PoseStack pose,ItemDisplayContext context,int light,int overlay,int outline,int[] tints,ItemQuads quads,ItemStackRenderState.FoilType foil) {
        var instance=new QuadInstance();instance.setLightCoords(light);instance.setOverlayCoords(overlay);
        for(var q:quads.all()) {
            var material=q.materialInfo();int tint=material.tintIndex();
            instance.setColor(material.isTinted()&&tint>=0&&tint<tints.length?tints[tint]:-1);
            try(var capture=capture(material.itemRenderType(),PrimitiveTopology.QUADS)){capture.putBakedQuad(pose.last(),q,instance);}
        }
    }
    @Override public void submitBlockModel(PoseStack pose,RenderType type,List<BlockStateModelPart> parts,int[] tints,int light,int overlay,int outline) {
        var instance=new QuadInstance();instance.setLightCoords(light);instance.setOverlayCoords(overlay);
        for(var part:parts)for(var direction:new net.minecraft.core.Direction[]{null,net.minecraft.core.Direction.DOWN,net.minecraft.core.Direction.UP,net.minecraft.core.Direction.NORTH,net.minecraft.core.Direction.SOUTH,net.minecraft.core.Direction.WEST,net.minecraft.core.Direction.EAST})
            for(var q:part.getQuads(direction)) {
                int tint=q.materialInfo().tintIndex();instance.setColor(tint>=0&&tint<tints.length?tints[tint]:-1);
                try(var capture=capture(type,PrimitiveTopology.QUADS)){capture.putBakedQuad(pose.last(),q,instance);}
            }
    }
    @Override public <S> void submitCrumblingOverlay(Model<? super S> model,S state,PoseStack pose,RenderType type,int light,int overlay,int color,ModelFeatureRenderer.CrumblingOverlay crumble) {submitModel(model,state,pose,type,light,overlay,color,null,0);}
    // These world-space decorations are intentionally not part of a remote entity's body.
    public void submitShadow(PoseStack p,float r,List<EntityRenderState.ShadowPiece> s){}
    public void submitNameTag(PoseStack p,Vec3 v,int i,Component c,boolean b,int j,CameraRenderState s){}
    @Override public void submitText(PoseStack pose,float x,float y,FormattedCharSequence text,boolean shadow,
                                     Font.DisplayMode mode,int light,int color,int background,int outline) {
        var font=Minecraft.getInstance().font;
        if(outline==0)font.prepareText(text,x,y,color,shadow,false,background).visit(glyphs(pose,mode,light));
        else {
            font.prepare8xTextOutline(text,x,y,outline).visit(glyphs(pose,Font.DisplayMode.NORMAL,light));
            font.prepareText(text,x,y,color,false,false,0).visit(glyphs(pose,Font.DisplayMode.POLYGON_OFFSET,light));
        }
    }
    private Font.GlyphVisitor glyphs(PoseStack pose,Font.DisplayMode mode,int light) {
        return new Font.GlyphVisitor() {
            @Override public void acceptRenderable(net.minecraft.client.gui.font.TextRenderable glyph) {
                var type=glyph.renderType(mode);
                try(var vertices=capture(type,PrimitiveTopology.QUADS)){glyph.render(pose.last().pose(),vertices,light,false);}
            }
        };
    }
    @Override public void submitTextBackground(PoseStack pose,float x0,float y0,float x1,float y1,int color,Font.DisplayMode mode,int light) {
        glyphs(pose,mode,light).acceptRenderable(Minecraft.getInstance().font.prepareBackground(x0,y0,x1,y1,color));
    }
    @Override public void submitFlame(PoseStack pose,EntityRenderState state,Quaternionf rotation) {
        float scale=state.boundingBoxWidth*1.4f;if(scale<=0)return;
        var atlas=Minecraft.getInstance().getAtlasManager();
        var fire0=atlas.get(net.minecraft.client.resources.model.ModelBakery.FIRE_0);
        var fire1=atlas.get(net.minecraft.client.resources.model.ModelBakery.FIRE_1);
        var type=RenderTypes.entityCutoutCull(TextureAtlas.LOCATION_BLOCKS);
        int light=net.minecraft.util.LightCoordsUtil.withBlock(state.lightCoords,15);
        pose.pushPose();
        try(var vertices=capture(type,PrimitiveTopology.QUADS)) {
            pose.scale(scale,scale,scale);pose.last().rotate(rotation);
            float remaining=state.boundingBoxHeight/scale,halfWidth=.5f,base=0,z=0;
            pose.translate(0,0,.3f-(int)remaining*.02f);
            for(int layer=0;remaining>0&&layer<64;layer++) {
                var sprite=layer%2==0?fire0:fire1;float u0=sprite.getU0(),u1=sprite.getU1();
                if((layer/2)%2==0){float swap=u0;u0=u1;u1=swap;}
                fireVertex(vertices,pose,-halfWidth,-base,z,u1,sprite.getV1(),light);
                fireVertex(vertices,pose,halfWidth,-base,z,u0,sprite.getV1(),light);
                fireVertex(vertices,pose,halfWidth,1.4f-base,z,u0,sprite.getV0(),light);
                fireVertex(vertices,pose,-halfWidth,1.4f-base,z,u1,sprite.getV0(),light);
                remaining-=.45f;base-=.45f;halfWidth*=.9f;z-=.03f;
            }
        }finally{pose.popPose();}
    }
    private static void fireVertex(VertexConsumer vertices,PoseStack pose,float x,float y,float z,float u,float v,int light) {
        vertices.addVertex(pose.last(),x,y,z).setColor(-1).setUv(u,v).setUv1(0,10).setLight(light).setNormal(pose.last(),0,1,0);
    }
    public void submitLeash(PoseStack p,EntityRenderState.LeashState s){}
    public void submitMovingBlock(PoseStack p,MovingBlockRenderState s,int i){}
    public void submitBreakingBlockModel(PoseStack p,List<BlockStateModelPart> b,int i,boolean a){}
    public void submitShapeOutline(PoseStack p,VoxelShape s,RenderType t,int c,float f,boolean b){}
    public void submitQuadParticleGroup(QuadParticleRenderState s){}
    public void submitGizmoPrimitives(DrawableGizmoPrimitives.Group g,CameraRenderState s,boolean b){}
    public void close(){for(var g:groups.values())g.close();groups.clear();}
    private final class Capture implements VertexConsumer,AutoCloseable {
        final Group group;final int size;final Vertex[] primitive=new Vertex[4];int count;Vertex current;
        Capture(Group group,PrimitiveTopology topology){this.group=group;size=topology==PrimitiveTopology.QUADS?4:topology==PrimitiveTopology.TRIANGLES?3:0;}
        private void finish() {
            if(current==null)return;
            if(size>0){primitive[count++]=current;if(count==size){
                for(int index:size==4?new int[]{0,1,2,0,2,3}:new int[]{0,1,2}) {
                    if(++vertices>MAX_VERTICES)throw new IllegalStateException("Portal entity geometry budget exceeded");
                    var v=primitive[index];group.builder.addVertex(v.x,v.y,v.z).setColor(v.color).setUv(v.u,v.v)
                            .setUv1(v.ou,v.ov).setUv2(v.lu,v.lv).setNormal(v.nx,v.ny,v.nz);
                }count=0;
            }}current=null;
        }
        public VertexConsumer addVertex(float x,float y,float z){finish();current=new Vertex();current.x=x;current.y=y;current.z=z;return this;}
        public VertexConsumer setColor(int r,int g,int b,int a){return setColor((a<<24)|(r<<16)|(g<<8)|b);}
        public VertexConsumer setColor(int color){current.color=color;return this;}
        public VertexConsumer setUv(float u,float v){current.u=u;current.v=v;return this;}
        public VertexConsumer setUv1(int u,int v){current.ou=u;current.ov=v;return this;}
        public VertexConsumer setUv2(int u,int v){current.lu=u;current.lv=v;return this;}
        public VertexConsumer setUv3(float u,float v){return this;}
        public VertexConsumer setNormal(float x,float y,float z){current.nx=x;current.ny=y;current.nz=z;return this;}
        public VertexConsumer setLineWidth(float w){return this;}
        public void close(){finish();}
    }
    private static final class Vertex {
        float x,y,z,u,v,nx,ny=1,nz;int color=-1,ou=0,ov=10,lu=0,lv=0;
    }
}
