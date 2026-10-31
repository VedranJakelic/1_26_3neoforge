package net.beamex.shamaschizm.wisdom.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.earthworm.client.WormMesh;
import net.beamex.shamaschizm.wisdom.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.joml.Vector3f;

/** Standard custom-geometry submits also work in PortalEntityMesh's remote collector. */
@EventBusSubscriber(modid = Shamaschizm.MOD_ID, value = Dist.CLIENT)
public final class WisdomRenderer extends EntityRenderer<TaintedWisdom, WisdomRenderer.State> {
    private static final float[] BIND_Y = {.0625F, -.5625F, -1.0625F, -1.5625F, -2.0625F};
    public static final class State extends EntityRenderState {
        final Vector3f[] rope = new Vector3f[StingerRope.NODES];
        WisdomAttachments.Anchor[] anchors;
        float yaw;
        boolean released;
    }
    public WisdomRenderer(EntityRendererProvider.Context context) { super(context); shadowRadius = .3F; }
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(WisdomRegistration.TYPE, WisdomRenderer::new);
    }
    @Override public State createRenderState() { return new State(); }
    @Override protected AABB getBoundingBoxForCulling(TaintedWisdom entity, float partial) { return entity.getBoundingBox().inflate(3); }
    @Override public void extractRenderState(TaintedWisdom entity, State state, float partial) {
        super.extractRenderState(entity, state, partial);
        state.yaw = entity.getYRot(); state.anchors = entity.anchors(); state.released = entity.wormsReleased();
        for (int i = 0; i < StingerRope.NODES; i++) state.rope[i] = entity.stingerPoint(i);
    }
    @Override public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state, pose, collector, camera);
        collector.submitCustomGeometry(pose, RenderTypes.entityCutout(Shamaschizm.id("textures/entity/tainted_wisdom.png")), (matrix, vertices) -> {
            for (float[] v : WisdomMesh.VERTICES) {
                Vec3 point, normal;
                if (v[0] == 0) {
                    point = WisdomAttachments.rotate(new Vec3(v[1], v[2], v[3]), state.yaw);
                    normal = WisdomAttachments.rotate(new Vec3(v[6], v[7], v[8]), state.yaw);
                } else {
                    int segment = 0;
                    while (segment < BIND_Y.length - 2 && v[2] < BIND_Y[segment + 1]) segment++;
                    float t = Math.clamp((BIND_Y[segment] - v[2]) / (BIND_Y[segment] - BIND_Y[segment + 1]), 0, 1);
                    Vector3f a = state.rope[segment], b = state.rope[segment + 1];
                    Vec3 up = new Vec3(a.x - b.x, a.y - b.y, a.z - b.z).normalize();
                    if (up.lengthSqr() < 1e-8) up = new Vec3(0, 1, 0);
                    Vec3 right = up.cross(new Vec3(0, 0, 1));
                    if (right.lengthSqr() < 1e-8) right = up.cross(new Vec3(1, 0, 0));
                    right = right.normalize(); Vec3 back = right.cross(up).normalize();
                    point = new Vec3(a.x + (b.x-a.x)*t, a.y + (b.y-a.y)*t, a.z + (b.z-a.z)*t)
                            .add(right.scale(v[1])).add(back.scale(v[3]));
                    normal = right.scale(v[6]).add(up.scale(v[7])).add(back.scale(v[8]));
                }
                vertices.addVertex(matrix, (float)point.x, (float)point.y, (float)point.z).setColor(-1)
                        .setUv(v[4], v[5]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(state.lightCoords)
                        .setNormal(matrix, (float)normal.x, (float)normal.y, (float)normal.z);
            }
        });
        if (state.released) return;
        collector.submitCustomGeometry(pose, RenderTypes.entityCutout(Shamaschizm.id("textures/entity/earthworm.png")), (matrix, vertices) -> {
            for (var anchor : state.anchors) {
                Vec3 axis = anchor.normal();
                Vec3 side = axis.cross(new Vec3(0, 1, 0));
                if (side.lengthSqr() < 1e-8) side = new Vec3(1, 0, 0);
                side = side.normalize();
                Vec3[] centers = new Vec3[9];
                for (int i = 0; i < 9; i++) {
                    double t = i / 8.0;
                    double wiggle = Math.sin(t * Math.PI) * Math.sin(state.ageInTicks * .18 + anchor.phase() - t * 7) * .045;
                    centers[i] = anchor.point().add(axis.scale(t * anchor.length())).add(side.scale(wiggle)).add(0, -.08 * t * t, 0);
                }
                for (float[] v : WormMesh.VERTICES) {
                    int i = (int)v[0];
                    Vec3 back = centers[Math.min(8, i+1)].subtract(centers[Math.max(0, i-1)]).normalize();
                    Vec3 right = new Vec3(0, 1, 0).cross(back);
                    if (right.lengthSqr() < 1e-8) right = new Vec3(1, 0, 0);
                    right = right.normalize(); Vec3 up = back.cross(right).normalize();
                    Vec3 point = centers[i].add(right.scale(v[1])).add(up.scale(v[2] - .03125)).add(back.scale(v[3]));
                    Vec3 normal = right.scale(v[6]).add(up.scale(v[7])).add(back.scale(v[8]));
                    point = WisdomAttachments.rotate(point, state.yaw); normal = WisdomAttachments.rotate(normal, state.yaw);
                    vertices.addVertex(matrix, (float)point.x, (float)point.y, (float)point.z).setColor(-1)
                            .setUv(v[4], v[5]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(state.lightCoords)
                            .setNormal(matrix, (float)normal.x, (float)normal.y, (float)normal.z);
                }
            }
        });
    }
}
