package net.beamex.shamaschizm.orrery.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.orrery.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
@EventBusSubscriber(modid=Shamaschizm.MOD_ID,value=Dist.CLIENT)
public final class OrreryRenderer implements BlockEntityRenderer<OrreryBlockEntity,OrreryRenderer.State>,net.neoforged.neoforge.client.extensions.IBlockEntityRendererExtension<OrreryBlockEntity> {
    public OrreryRenderer(BlockEntityRendererProvider.Context context){}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(OrreryRegistration.TYPE,OrreryRenderer::new);}
    public static final class State extends BlockEntityRenderState{double phase;float facing;boolean hanging;}
    @Override public State createRenderState(){return new State();}
    @Override public AABB getRenderBoundingBox(OrreryBlockEntity be){return new AABB(be.getBlockPos()).inflate(4);}
    @Override public void extractRenderState(OrreryBlockEntity be,State s,float partial,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay cracks){
        BlockEntityRenderer.super.extractRenderState(be,s,partial,camera,cracks);
        s.hanging=be.getBlockState().getValue(OrreryBlock.HANGING);s.phase=be.visualPhase(partial);s.facing=switch(be.getBlockState().getValue(OrreryBlock.FACING)){case EAST->-90;case SOUTH->180;case WEST->90;default->0;};
    }
    @Override public void submit(State s,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){
        double earth=OrreryMotion.planetAngle(s.phase,2),moon=OrreryMotion.moonAngle(s.phase);double[] nibiru=OrreryMotion.nibiru(s.phase);
        pose.pushPose();pose.translate(.5,0,.5);pose.rotate(Axis.YP.rotationDegrees(s.facing));
        if(s.hanging){pose.translate(0,1,0);pose.rotate(Axis.XP.rotationDegrees(180));}
        collector.submitCustomGeometry(pose,RenderTypes.entityCutout(Shamaschizm.id("textures/block/orrery.png")),(matrix,vertices)->{
            for(float[] v:OrreryMesh.VERTICES){
                int part=(int)v[0];
                if(part==3 || (part==5 && (Math.abs(v[1])>.4 || Math.abs(v[3])>.4)))continue;
                Vec3 p=new Vec3(v[1],v[2],v[3]),n=new Vec3(v[6],v[7],v[8]);
                if(part==4){p=p.xRot((float)Math.toRadians(22.5)).add(nibiru[0]/16,nibiru[1]/16,nibiru[2]/16);n=n.xRot((float)Math.toRadians(22.5));}
                if(part==2){p=p.subtract(0,9.0/16,-1).scale(.55).yRot((float)(moon-earth)).add(0,9.0/16,-1);n=n.yRot((float)(moon-earth));}
                if(part==1||part==2){p=p.yRot((float)earth);n=n.yRot((float)earth);}
                vertices.addVertex(matrix,(float)p.x,(float)p.y,(float)p.z).setColor(-1).setUv(v[4],v[5]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(s.lightCoords).setNormal(matrix,(float)n.x,(float)n.y,(float)n.z);
            }
        });
        collector.submitCustomGeometry(pose,RenderTypes.entityCutout(net.minecraft.resources.Identifier.withDefaultNamespace("textures/block/white_concrete.png")),(matrix,vertices)->{
            for(float[] v:OrreryPlanets.TRACKS)vertices.addVertex(matrix,v[0],v[1],v[2]).setColor(0xFF000000|(int)v[8]).setUv(v[3],v[4]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(s.lightCoords).setNormal(matrix,v[5],v[6],v[7]);
            for(int i=0;i<9;i++){
                if(i==2)continue; // Keep the supplied Earth artwork.
                double a=OrreryMotion.planetAngle(s.phase,i),r=OrreryMotion.RADII[i];
                for(float[] v:OrreryPlanets.BODIES.get(i)){
                    Vec3 p=new Vec3(v[0],v[1],v[2]).yRot((float)a).add(-Math.sin(a)*r,9.0/16,-Math.cos(a)*r);
                    Vec3 n=new Vec3(v[5],v[6],v[7]).yRot((float)a);
                    vertices.addVertex(matrix,(float)p.x,(float)p.y,(float)p.z).setColor(0xFF000000|(int)v[8]).setUv(v[3],v[4]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(s.lightCoords).setNormal(matrix,(float)n.x,(float)n.y,(float)n.z);
                }
            }
        });pose.popPose();
    }
}
