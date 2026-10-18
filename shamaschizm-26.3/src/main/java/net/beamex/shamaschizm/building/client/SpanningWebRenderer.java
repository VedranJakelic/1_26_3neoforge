package net.beamex.shamaschizm.building.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.beamex.shamaschizm.building.SpanningWebEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.AABB;

public final class SpanningWebRenderer extends EntityRenderer<SpanningWebEntity, SpanningWebRenderer.State> {
    public SpanningWebRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public State createRenderState() { return new State(); }
    @Override protected AABB getBoundingBoxForCulling(SpanningWebEntity web, float partialTicks) { return web.rectangle().bounds(); }
    @Override public void extractRenderState(SpanningWebEntity web, State state, float partial) {
        super.extractRenderState(web, state, partial);
        state.width = web.width(); state.height = web.height(); state.yaw = web.getYRot(); state.flipped = web.flipped();
    }
    @Override public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state, pose, collector, camera);
        float width = state.width, height = state.height; int light = state.lightCoords;
        float topV = state.flipped ? 1 : 0, bottomV = 1 - topV;
        pose.pushPose(); pose.rotate(Axis.YP.rotationDegrees(-state.yaw));
        collector.submitCustomGeometry(pose, RenderTypes.entityCutout(WebTextures.get(width, height)), (p, buffer) -> {
            vertex(p, buffer, -width / 2, 0, 0, bottomV, light);
            vertex(p, buffer, width / 2, 0, 1, bottomV, light);
            vertex(p, buffer, width / 2, height, 1, topV, light);
            vertex(p, buffer, -width / 2, height, 0, topV, light);
        });
        pose.popPose();
    }
    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float u, float v, int light) {
        buffer.addVertex(pose, x, y, 0).setColor(-1).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light).setNormal(pose, 0, 0, 1);
    }
    public static final class State extends EntityRenderState { float width, height, yaw; boolean flipped; }
}
