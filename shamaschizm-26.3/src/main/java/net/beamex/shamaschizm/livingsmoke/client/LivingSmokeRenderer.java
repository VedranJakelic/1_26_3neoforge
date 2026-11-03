package net.beamex.shamaschizm.livingsmoke.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.livingsmoke.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.*;
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

/** Submit-node geometry also renders correctly in the mod's portal entity scene. */
@EventBusSubscriber(modid = Shamaschizm.MOD_ID, value = Dist.CLIENT)
public final class LivingSmokeRenderer extends EntityRenderer<LivingSmoke, LivingSmokeRenderer.State> {
    private static final float PI = (float)Math.PI;
    public static final class State extends EntityRenderState {
        float yaw, pitch, reaction;
        boolean dead;
    }
    public LivingSmokeRenderer(EntityRendererProvider.Context context) {
        super(context); shadowRadius = 0;
    }
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(LivingSmokeRegistration.TYPE, LivingSmokeRenderer::new);
    }
    @Override public State createRenderState() { return new State(); }
    @Override protected AABB getBoundingBoxForCulling(LivingSmoke entity, float partial) {
        return entity.getBoundingBox().inflate(2.5);
    }
    @Override public void extractRenderState(LivingSmoke entity, State state, float partial) {
        super.extractRenderState(entity, state, partial);
        state.yaw = Mth.rotLerp(partial, entity.yRotO, entity.getYRot());
        // Base-entity X rotation is not a reliable animation channel for the
        // custom model. Read the explicitly synchronized server-side pose.
        state.pitch = entity.visualPitch();
        state.reaction = entity.pearlReaction(partial);
        state.dead = !entity.isAlive();
    }
    @Override public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state, pose, collector, camera);
        // Source model points north; Minecraft yaw zero points south.
        Matrix4f root = new Matrix4f()
                .rotateY(PI - (float)Math.toRadians(state.yaw))
                .rotateX(-(float)Math.toRadians(state.pitch)); // +visualPitch tips the -Z mouth down
        // Idle from Blockbench, slowed 20%: 10 seconds -> 12 seconds.
        float phase = (state.ageInTicks / 240F * 10F) % 10F;
        float idle1 = sample(phase, new float[][]{{0,0},{2,180},{2.75F,180},{3.125F,182.5F},{5,0},{6.5F,0},{7.58333F,0},{9.91667F,360},{10,360}});
        float idle2 = sample(phase, new float[][]{{0,0},{.75F,0},{3,90},{4,90},{7,180},{9.75F,360},{10,360}});
        float idle3 = sample(phase, new float[][]{{0,0},{3.5F,-360},{4.5F,-360},{7.25F,-185},{9.25F,-360},{10,-360}});
        submitPart(state, pose, collector, root, LivingSmokeMesh.MOUTH1, new Vector3f(0,0,0), idle1);
        submitPart(state, pose, collector, root, LivingSmokeMesh.MOUTH2, new Vector3f(0,0,-.125F), 45 + idle2);
        submitPart(state, pose, collector, root, LivingSmokeMesh.MOUTH3, new Vector3f(0,0,.25F), 45 + idle3);
        if (state.dead) return; // Detached pearls now have their own physics entities.
        collector.submitCustomGeometry(pose, RenderTypes.entityCutout(Shamaschizm.id("textures/entity/living_smoke.png")), (matrix, vertices) -> {
            for (int i = 0; i < 16; i++) {
                float angle = LivingSmokePearls.angle(i, state.ageInTicks);
                var pearl = LivingSmokePearls.position(i, state.ageInTicks, state.reaction);
                float x = (float)pearl.x, y = (float)pearl.y, z = (float)pearl.z;
                Matrix4f model = new Matrix4f(root)
                        .translate(x, y, z)
                        .rotateX((float)Math.toRadians(2.5))
                        .rotateZ(angle);
                Matrix3f n = model.normal(new Matrix3f());
                writeMesh(LivingSmokeMesh.PEARL, model, n, matrix, vertices, state.lightCoords);
            }
        });
    }
    private static void submitPart(State s, PoseStack pose, SubmitNodeCollector collector,
                                   Matrix4f root, float[][] mesh, Vector3f pivot, float degrees) {
        Matrix4f transform = new Matrix4f(root)
                .translate(pivot)
                .rotateZ((float)Math.toRadians(degrees))
                .translate(-pivot.x, -pivot.y, -pivot.z);
        Matrix3f normals = transform.normal(new Matrix3f());
        collector.submitCustomGeometry(pose, RenderTypes.entityCutout(Shamaschizm.id("textures/entity/living_smoke.png")),
                (matrix, vertices) -> writeMesh(mesh, transform, normals, matrix, vertices, s.lightCoords));
    }
    private static void writeMesh(float[][] mesh, Matrix4f transform, Matrix3f normals,
                                  PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer vertices, int light) {
        for (float[] v : mesh) {
            Vector3f p = transform.transformPosition(new Vector3f(v[0],v[1],v[2]));
            Vector3f n = normals.transform(new Vector3f(v[5],v[6],v[7])).normalize();
            vertices.addVertex(pose, p.x, p.y, p.z)
                    .setColor(-1).setUv(v[3],v[4])
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light).setNormal(pose,n.x,n.y,n.z);
        }
    }
    private static float smooth(float v) { v = Mth.clamp(v,0,1); return v*v*(3-2*v); }
    private static float sample(float t, float[][] keys) {
        for (int i=0;i<keys.length-1;i++) {
            if (t<=keys[i+1][0]) {
                float p = (t-keys[i][0]) / Math.max(.001F, keys[i+1][0]-keys[i][0]);
                return Mth.lerp(smooth(p),keys[i][1],keys[i+1][1]);
            }
        }
        return keys[keys.length-1][1];
    }
}
