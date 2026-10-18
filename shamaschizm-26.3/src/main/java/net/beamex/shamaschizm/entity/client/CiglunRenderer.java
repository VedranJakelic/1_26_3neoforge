package net.beamex.shamaschizm.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.entity.custom.CiglunEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class CiglunRenderer extends MobRenderer<CiglunEntity, CiglunRenderer.CiglunRenderState, CiglunModel> {
    private static final Identifier TEXTURE = Shamaschizm.id("textures/entity/ciglun.png");
    private static final Identifier BEAM_TEXTURE =
            Identifier.withDefaultNamespace("textures/entity/guardian/guardian_beam.png");
    private static final RenderType BEAM_RENDER_TYPE = RenderTypes.entityCutout(BEAM_TEXTURE);

    public CiglunRenderer(EntityRendererProvider.Context context) {
        super(context, new CiglunModel(), 0.5F);
    }

    @Override
    public CiglunRenderState createRenderState() {
        return new CiglunRenderState();
    }

    @Override
    public Identifier getTextureLocation(CiglunRenderState state) {
        return TEXTURE;
    }

    @Override
    public void extractRenderState(CiglunEntity entity, CiglunRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.action = entity.getAction();
        state.turningAnimation.copyFrom(entity.getClientTurningAnimation());
        state.openAnimation.copyFrom(entity.getClientOpenAnimation());
        state.corePosition = entity.getCorePosition(partialTick);
        LivingEntity target = entity.getLaserTarget();
        state.laserTargetPosition = target == null ? null : getPosition(target, target.getBbHeight() * 0.5D, partialTick);
    }

    @Override
    public boolean shouldRender(CiglunEntity entity, Frustum culler, double cameraX, double cameraY, double cameraZ, float partialTicks) {
        if (super.shouldRender(entity, culler, cameraX, cameraY, cameraZ, partialTicks)) return true;
        LivingEntity target = entity.getLaserTarget();
        return target != null && culler.isVisible(new AABB(
                entity.getCorePosition(1.0F), getPosition(target, target.getBbHeight() * 0.5D, 1.0F)));
    }

    @Override
    public void submit(CiglunRenderState state, PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state, poseStack, collector, camera);
        if (state.laserTargetPosition == null) return;

        Vec3 beam = state.laserTargetPosition.subtract(state.corePosition);
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.5F, 0.0F);
        renderBeam(poseStack, collector, beam, state.ageInTicks);
        poseStack.popPose();
    }

    private static Vec3 getPosition(LivingEntity entity, double yOffset, float partialTick) {
        return new Vec3(
                Mth.lerp(partialTick, entity.xOld, entity.getX()),
                Mth.lerp(partialTick, entity.yOld, entity.getY()) + yOffset,
                Mth.lerp(partialTick, entity.zOld, entity.getZ()));
    }

    private static void renderBeam(PoseStack stack, SubmitNodeCollector collector, Vec3 vector, float time) {
        float length = (float)vector.length();
        if (length < 0.01F) return;
        vector = vector.normalize();
        float xRotation = (float)Math.acos(vector.y);
        float yRotation = (float)(Math.PI / 2.0D) - (float)Math.atan2(vector.z, vector.x);
        stack.rotate(Axis.YP.rotation(yRotation));
        stack.rotate(Axis.XP.rotation(xRotation));

        float spin = -time * 0.18F;
        float inner = 0.095F;
        float outer = 0.14F;
        float minV = -(time * 0.03F % 1.0F);
        float maxV = minV + length * 2.2F;
        float top = length;
        int red = 170;
        int green = 255;
        int blue = 255;

        float wx = Mth.cos(spin + Mth.PI) * inner;
        float wz = Mth.sin(spin + Mth.PI) * inner;
        float ex = Mth.cos(spin) * inner;
        float ez = Mth.sin(spin) * inner;
        float nx = Mth.cos(spin + Mth.HALF_PI) * inner;
        float nz = Mth.sin(spin + Mth.HALF_PI) * inner;
        float sx = Mth.cos(spin + Mth.PI + Mth.HALF_PI) * inner;
        float sz = Mth.sin(spin + Mth.PI + Mth.HALF_PI) * inner;

        float wnx = Mth.cos(spin + Mth.PI * 0.75F) * outer;
        float wnz = Mth.sin(spin + Mth.PI * 0.75F) * outer;
        float enx = Mth.cos(spin + Mth.PI * 0.25F) * outer;
        float enz = Mth.sin(spin + Mth.PI * 0.25F) * outer;
        float esx = Mth.cos(spin + Mth.PI * 1.75F) * outer;
        float esz = Mth.sin(spin + Mth.PI * 1.75F) * outer;
        float wsx = Mth.cos(spin + Mth.PI * 1.25F) * outer;
        float wsz = Mth.sin(spin + Mth.PI * 1.25F) * outer;

        collector.submitCustomGeometry(stack, BEAM_RENDER_TYPE, (pose, buffer) -> {
            vertex(buffer, pose, wx, top, wz, red, green, blue, 0.5F, maxV);
            vertex(buffer, pose, wx, 0, wz, red, green, blue, 0.5F, minV);
            vertex(buffer, pose, ex, 0, ez, red, green, blue, 0, minV);
            vertex(buffer, pose, ex, top, ez, red, green, blue, 0, maxV);
            vertex(buffer, pose, nx, top, nz, red, green, blue, 0.5F, maxV);
            vertex(buffer, pose, nx, 0, nz, red, green, blue, 0.5F, minV);
            vertex(buffer, pose, sx, 0, sz, red, green, blue, 0, minV);
            vertex(buffer, pose, sx, top, sz, red, green, blue, 0, maxV);
            vertex(buffer, pose, wnx, top, wnz, 220, 255, 255, 0.5F, 1.0F);
            vertex(buffer, pose, enx, top, enz, 220, 255, 255, 1.0F, 1.0F);
            vertex(buffer, pose, esx, top, esz, 220, 255, 255, 1.0F, 0.5F);
            vertex(buffer, pose, wsx, top, wsz, 220, 255, 255, 0.5F, 0.5F);
        });
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose,
                               float x, float y, float z, int red, int green, int blue, float u, float v) {
        buffer.addVertex(pose, x, y, z)
                .setColor(red, green, blue, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(15728880)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    public static final class CiglunRenderState extends LivingEntityRenderState {
        public int action;
        public final AnimationState turningAnimation = new AnimationState();
        public final AnimationState openAnimation = new AnimationState();
        public Vec3 corePosition = Vec3.ZERO;
        public Vec3 laserTargetPosition;
    }
}
