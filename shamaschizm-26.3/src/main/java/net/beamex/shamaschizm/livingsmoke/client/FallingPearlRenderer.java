package net.beamex.shamaschizm.livingsmoke.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.livingsmoke.FallingPearl;
import net.beamex.shamaschizm.livingsmoke.LivingSmokeRegistration;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** The same Blockbench pearl mesh, tumbling in free flight and shrinking to nothing. */
@EventBusSubscriber(modid = Shamaschizm.MOD_ID, value = Dist.CLIENT)
public final class FallingPearlRenderer extends EntityRenderer<FallingPearl, FallingPearlRenderer.State> {
    public static final class State extends EntityRenderState {
        float yaw, pitch, age;
    }
    public FallingPearlRenderer(EntityRendererProvider.Context ctx) { super(ctx); shadowRadius = 0; }
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(LivingSmokeRegistration.FALLING_PEARL, FallingPearlRenderer::new);
    }
    @Override public State createRenderState() { return new State(); }
    @Override protected AABB getBoundingBoxForCulling(FallingPearl entity, float partial) {
        return entity.getBoundingBox().inflate(.25);
    }
    @Override public void extractRenderState(FallingPearl pearl, State state, float partial) {
        super.extractRenderState(pearl, state, partial);
        state.yaw = Mth.rotLerp(partial, pearl.yRotO, pearl.getYRot());
        state.pitch = Mth.lerp(partial, pearl.xRotO, pearl.getXRot());
        state.age = pearl.tickCount + partial;
    }
    @Override public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state, pose, collector, camera);
        float remaining = FallingPearl.LIFETIME - state.age;
        if (remaining <= 0) return;
        float scale = Math.min(1F, remaining / 18F);
        Matrix4f transform = new Matrix4f()
                .rotateY((float)Math.toRadians(state.yaw))
                .rotateX((float)Math.toRadians(state.pitch))
                .rotateZ((float)Math.toRadians(state.yaw * .65F))
                .scale(scale);
        Matrix3f normal = transform.normal(new Matrix3f());
        collector.submitCustomGeometry(pose,
                RenderTypes.entityCutout(Shamaschizm.id("textures/entity/living_smoke.png")),
                (matrix, vertices) -> {
                    for (float[] v : LivingSmokeMesh.PEARL) {
                        Vector3f point = transform.transformPosition(new Vector3f(v[0], v[1], v[2]));
                        Vector3f n = normal.transform(new Vector3f(v[5], v[6], v[7])).normalize();
                        vertices.addVertex(matrix, point.x, point.y, point.z)
                                .setColor(-1).setUv(v[3], v[4])
                                .setOverlay(OverlayTexture.NO_OVERLAY)
                                .setLight(state.lightCoords).setNormal(matrix, n.x, n.y, n.z);
                    }
                });
    }
}
