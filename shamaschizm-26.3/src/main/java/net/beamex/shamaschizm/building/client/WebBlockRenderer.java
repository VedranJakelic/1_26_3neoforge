package net.beamex.shamaschizm.building.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.beamex.shamaschizm.building.WebBlockEntity;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.*;

/** Only one cell renders the whole web. Other cells supply ordinary block contact and mining. */
public final class WebBlockRenderer implements BlockEntityRenderer<WebBlockEntity,WebBlockRenderer.State>,
        net.neoforged.neoforge.client.extensions.IBlockEntityRendererExtension<WebBlockEntity> {
    public WebBlockRenderer(BlockEntityRendererProvider.Context context){}
    @Override public State createRenderState(){return new State();}
    @Override public boolean shouldRender(WebBlockEntity be,Vec3 camera){return be.master() && BlockEntityRenderer.super.shouldRender(be,camera);}
    @Override public AABB getRenderBoundingBox(WebBlockEntity be){return be.rectangle().bounds();}
    @Override public void extractRenderState(WebBlockEntity be,State s,float partial,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay cracks){
        BlockEntityRenderer.super.extractRenderState(be,s,partial,camera,cracks);
        var r=be.rectangle();s.offset=r.bottom().subtract(Vec3.atLowerCornerOf(be.getBlockPos()));
        s.width=(float)r.width();s.height=(float)r.height();s.yaw=(float)r.yaw();s.flip=be.flipped();s.draw=be.master();
    }
    @Override public void submit(State s,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){
        if(!s.draw)return;
        float w=s.width,h=s.height,top=s.flip?1:0,bottom=1-top;int light=s.lightCoords;
        pose.pushPose();pose.translate(s.offset.x,s.offset.y,s.offset.z);pose.rotate(Axis.YP.rotationDegrees(-s.yaw));
        collector.submitCustomGeometry(pose,RenderTypes.entityCutout(WebTextures.get(w,h)),(p,b)->{
            vertex(p,b,-w/2,0,0,bottom,light);vertex(p,b,w/2,0,1,bottom,light);
            vertex(p,b,w/2,h,1,top,light);vertex(p,b,-w/2,h,0,top,light);
        });pose.popPose();
    }
    private static void vertex(PoseStack.Pose p,VertexConsumer b,float x,float y,float u,float v,int light){
        b.addVertex(p,x,y,0).setColor(-1).setUv(u,v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p,0,0,1);
    }
    public static final class State extends BlockEntityRenderState{Vec3 offset=Vec3.ZERO;float width,height,yaw;boolean flip,draw;}
}
