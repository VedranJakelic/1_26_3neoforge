package net.beamex.shamaschizm.localportal.client;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.textures.FilterMode;
import java.nio.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.beamex.shamaschizm.localportal.*;
import net.beamex.shamaschizm.world.Schizm;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/** Single-layer terrain windows. Never changes Minecraft.level, the camera, or vanilla section lists. */
@EventBusSubscriber(modid="shamaschizm",value=Dist.CLIENT)
public final class PortalViewRenderer {
    private static final Map<String,PortalTerrain> INBOX=new ConcurrentHashMap<>();
    private static final Map<String,View> VIEWS=new LinkedHashMap<>();
    private static Object world,models;
    static boolean readyForInteraction(Doorway source){
        var view=VIEWS.get(source.name());
        return view!=null&&!view.failed&&view.rendered&&view.data.source().equals(source);
    }
    @SubscribeEvent public static void network(RegisterClientPayloadHandlersEvent event){
        event.register(PortalTerrain.TYPE,(packet,context)->{
            if(INBOX.size()>=4&&!INBOX.containsKey(packet.source().name()))INBOX.clear();
            INBOX.put(packet.source().name(),packet);
        });
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){
        INBOX.clear();closeAll();PortalEntityScene.clear();world=null;models=null;
    }
    private static void closeAll(){for(var v:VIEWS.values())v.close();VIEWS.clear();}
    @SubscribeEvent public static void prepare(PrepareRenderBuffersEvent event){
        var mc=Minecraft.getInstance();
        if(mc.level==null||mc.player==null||!mc.level.dimension().equals(Schizm.KEY)){closeAll();INBOX.clear();world=null;return;}
        if(world!=mc.level){closeAll();world=mc.level;models=mc.getModelManager().getBlockStateModelSet();}
        if(models!=mc.getModelManager().getBlockStateModelSet()){
            for(var v:VIEWS.values())INBOX.put(v.data.source().name(),v.data);
            closeAll();models=mc.getModelManager().getBlockStateModelSet();
        }
        var camera=event.getLevelRenderState().cameraRenderState;
        var active=new HashMap<String,Doorway>();
        for(var d:DoorwayDebugRenderer.doorways())if(d.enabled()&&d.origin().distanceToSqr(camera.pos)<26*26)active.put(d.name(),d);
        var iterator=VIEWS.entrySet().iterator();
        while(iterator.hasNext()){
            var entry=iterator.next();
            if(!entry.getValue().data.source().equals(active.get(entry.getKey()))){entry.getValue().close();iterator.remove();}
        }
        for(var entry:List.copyOf(INBOX.entrySet())){
            PortalTerrain packet=entry.getValue();
            if(!packet.source().equals(active.get(entry.getKey()))) {
                if(packet.source().origin().distanceToSqr(camera.pos)>26*26)INBOX.remove(entry.getKey(),packet);
                continue; // Metadata can arrive in the same frame but be applied on the next client tick.
            }
            INBOX.remove(entry.getKey(),packet);
            View view=VIEWS.get(entry.getKey());
            if(view==null){
                if(VIEWS.size()>=2){var first=VIEWS.keySet().iterator().next();VIEWS.remove(first).close();}
                VIEWS.put(entry.getKey(),new View(packet));
            }else view.update(packet);
        }
        // Use the explicit world-camera rotation. Projection (including optional bob/hurt effects)
        // is shared with vanilla on the GPU, rather than reconstructed from the raw camera projection.
        Matrix4f projectionView=new Matrix4f(camera.viewRotationMatrix);
        long budget=System.nanoTime()+4_000_000L;
        for(var view:VIEWS.values()){
            view.rendered=false;
            if(view.failed)continue;
            try{
                view.build(budget);
                if(view.mesh==null)continue;
                view.crossing(camera.pos,camera.isFirstPerson && !mc.player.isSpectator() && !mc.player.isPassenger(),
                        mc.player.getBbWidth(),mc.player.getBbHeight(),mc.player.getEyeHeight());
                if(view.data.source().distance(camera.pos)<=0 && !view.preview)continue;
                // At most two nearby windows: let GPU clipping handle screen visibility.
                // A separate CPU frustum can disagree with the final, effect-adjusted projection.
                view.render(projectionView,camera.pos,mc.gameRenderer.mainRenderTarget().width,mc.gameRenderer.mainRenderTarget().height,event.getLevelRenderState().worldPartialTicks);
            }catch(RuntimeException failure){
                view.failed=true;
                org.slf4j.LoggerFactory.getLogger("shamaschizm.localportal").error("Portal view failed for "+view.data.source().name(),failure);
            }
        }
    }
    @SubscribeEvent public static void composite(RenderLevelStageEvent.AfterOpaqueBlocks event){
        var pass=event.getRenderPass();
        for(var view:VIEWS.values())if(view.rendered){
            pass.setPipeline(RenderSystem.getCompiledPipeline(PortalViewPipelines.SURFACE));
            pass.setUniform("Projection",RenderSystem.getProjectionMatrixBuffer());
            pass.setUniform("PortalView",view.surfaceUniform);
            pass.setUniform("Sampler0",view.target.getColorTextureView(),RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            pass.setVertexBuffer(0,view.surface.slice());pass.draw(6,1,0,0);
        }
    }
    private static final class View implements AutoCloseable {
        PortalTerrain data,queued;
        PortalTerrainMesh mesh,pending;
        PortalEntityMesh entities;
        TextureTarget target;
        GpuBuffer surface,sceneUniform,surfaceUniform;
        boolean rendered,failed,preview;
        Vec3 previousCamera;
        long previewUntil;
        void crossing(Vec3 camera,boolean allowed,float bodyWidth,float bodyHeight,float eyeHeight) {
            var d=data.source();var local=d.local(camera);
            long now=System.nanoTime();
            boolean inside=Math.abs(local.x)<d.width()/2 && local.y>0 && local.y<d.height();
            if(allowed && inside && previousCamera!=null
                    && d.crossed(previousCamera.add(0,-eyeHeight,0),camera.add(0,-eyeHeight,0),bodyWidth,bodyHeight)
                    && previousCamera.distanceToSqr(camera)<4)previewUntil=now+1_000_000_000L;
            if(!allowed || !inside || local.z>0 || local.z< -2)previewUntil=0;
            preview=now<previewUntil;
            previousCamera=camera;
        }
        View(PortalTerrain p){data=p;pending=new PortalTerrainMesh(p);}
        void update(PortalTerrain p){
            if(data.source().equals(p.source())&&data.target().equals(p.target())
                    &&data.blockEntities().equals(p.blockEntities())&&Arrays.equals(data.states(),p.states())&&Arrays.equals(data.lights(),p.lights()))return;
            data=p;
            if(pending!=null)queued=p;
            else pending=new PortalTerrainMesh(p);
            failed=false;
        }
        void build(long deadline){
            if(pending==null)return;
            pending.advance(deadline);
            if(pending.ready){
                if(mesh!=null)mesh.close();mesh=pending;pending=null;
                if(queued!=null){pending=new PortalTerrainMesh(queued);queued=null;}
            }
        }
        void render(Matrix4f projectionView,Vec3 camera,int windowWidth,int windowHeight,float partialTick){
            // Match the main target exactly so aperture fragments sample the same pixel centers.
            // Downscaling and filtering can shift apparent block edges at the seam.
            int width=Math.max(1,windowWidth),height=Math.max(1,windowHeight);
            if(target==null)target=new TextureTarget("Schizm doorway view",width,height,GpuFormat.RGBA8_UNORM,GpuFormat.D32_FLOAT);
            else if(target.width!=width||target.height!=height)target.resize(width,height);
            var source=data.source();var destination=data.target();
            Matrix4f surfaceMatrix=PortalViewTransform.surface(projectionView,camera,source);
            Matrix4f sceneMatrix=PortalViewTransform.scene(surfaceMatrix,source,destination);
            if(surface==null)surface=surface(source);
            sceneUniform=uniform(sceneUniform,sceneMatrix,destination.facing().normal,Doorway.VIEW_DEPTH);
            surfaceUniform=uniform(surfaceUniform,surfaceMatrix,source.facing().normal,preview ? 1 : 0,(float)source.width(),(float)source.height());
            if(entities!=null){entities.close();entities=null;}
            try {entities=PortalEntityMesh.build(data,camera,partialTick,mesh.blockEntities);}
            catch(RuntimeException failure){PortalEntityScene.warn("geometry pass",failure);}
            mesh.fluids.prepare(source.transformPoint(destination,camera));
            var encoder=RenderSystem.getDevice().createCommandEncoder();
            try(var pass=encoder.createRenderPass(()->"Schizm portal terrain",target.getColorTextureView(),
                    Optional.of(new Vector4f(0,0,0,1)),target.getDepthTextureView(),OptionalDouble.of(RenderSystem.DEFAULT_DEPTH_CLEAR_VALUE))){
                pass.setPipeline(RenderSystem.getCompiledPipeline(PortalViewPipelines.TERRAIN));
                pass.setUniform("Projection",RenderSystem.getProjectionMatrixBuffer());
                pass.setUniform("PortalView",sceneUniform);
                pass.setUniform("Sampler2",Minecraft.getInstance().gameRenderer.levelLightmap(),RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
                for(var batch:mesh.batches){
                    // Sprite atlasLocation is a texture path, not an AtlasManager logical atlas ID.
                    var atlas=Minecraft.getInstance().getTextureManager().getTexture(batch.atlas());
                    pass.setUniform("Sampler0",atlas.getTextureView(),RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
                    pass.setVertexBuffer(0,batch.vertices().slice());pass.draw(batch.count(),1,0,0);
                }
                mesh.fluids.draw(pass,sceneUniform,false);
                if(entities!=null)entities.draw(pass,sceneUniform);
                mesh.fluids.draw(pass,sceneUniform,true);
            }
            rendered=true;
        }
        private static GpuBuffer uniform(GpuBuffer existing,Matrix4f matrix,Vec3 plane,float depth){
            return uniform(existing,matrix,plane,depth,0,0);
        }
        private static GpuBuffer uniform(GpuBuffer existing,Matrix4f matrix,Vec3 plane,float depth,float width,float height){
            ByteBuffer bytes=ByteBuffer.allocateDirect(96).order(ByteOrder.nativeOrder());
            matrix.get(0,bytes);bytes.position(64);bytes.putFloat((float)plane.x).putFloat((float)plane.y).putFloat((float)plane.z).putFloat(depth);bytes.putFloat(width).putFloat(height).putFloat(0).putFloat(0);bytes.flip();
            if(existing==null)return RenderSystem.getDevice().createBuffer(()->"Schizm portal view matrix",GpuBuffer.USAGE_UNIFORM|GpuBuffer.USAGE_COPY_DST,bytes);
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(existing.slice(),bytes);return existing;
        }
        private static GpuBuffer surface(Doorway d){
            try(var memory=new ByteBufferBuilder(256)){
                var b=new BufferBuilder(memory,PrimitiveTopology.TRIANGLES,DefaultVertexFormat.POSITION);
                // Screen-space coverage avoids losing the aperture to near-plane clipping.
                for(float[] p:new float[][]{{-1,-1},{1,-1},{1,1},{-1,-1},{1,1},{-1,1}})
                    b.addVertex(p[0],p[1],0);
                try(var mesh=b.buildOrThrow()){
                    return RenderSystem.getDevice().createBuffer(()->"Schizm portal aperture",GpuBuffer.USAGE_VERTEX,mesh.vertexBuffer());
                }
            }
        }
        public void close(){
            if(entities!=null)entities.close();
            if(mesh!=null)mesh.close();if(pending!=null)pending.close();if(target!=null)target.destroyBuffers();
            if(surface!=null)surface.close();if(sceneUniform!=null)sceneUniform.close();if(surfaceUniform!=null)surfaceUniform.close();
        }
    }
}
