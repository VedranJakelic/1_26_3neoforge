package net.beamex.shamaschizm.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.entity.custom.GiantCentipedeEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public final class GiantCentipedeRenderer extends MobRenderer<GiantCentipedeEntity,
        GiantCentipedeRenderer.RenderState, GiantCentipedeHeadModel> {
    private static final Identifier HEAD_TEXTURE = Shamaschizm.id("textures/entity/giant_centipede_head.png");
    private static final Identifier BODY_TEXTURE = Shamaschizm.id("textures/entity/giant_centipede_body.png");
    private final GiantCentipedeBodyMesh bodyMesh;

    public GiantCentipedeRenderer(EntityRendererProvider.Context context) {
        super(context, new GiantCentipedeHeadModel(
                context.bakeLayer(GiantCentipedeHeadModel.LAYER_LOCATION)), 0.42F);
        this.bodyMesh = new GiantCentipedeBodyMesh(context.getResourceManager());
    }

    @Override
    public RenderState createRenderState() {
        return new RenderState();
    }

    @Override
    public void extractRenderState(GiantCentipedeEntity entity, RenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.head = entity.isHead();
        state.moving = entity.isChainMoving();
        state.sectionPitch = Mth.rotLerp(partialTick, entity.xRotO, entity.getXRot());
        state.sectionRoll = entity.getVisualRoll(partialTick);
    }

    @Override
    public Identifier getTextureLocation(RenderState state) {
        return HEAD_TEXTURE;
    }

    @Override
    protected void setupRotations(RenderState state, PoseStack poseStack, float bodyRot, float scale) {
        super.setupRotations(state, poseStack, bodyRot, scale);
        // Yaw and pitch establish the neighbor-derived forward direction first.
        // The model was authored with its front/back vertical angle reversed,
        // so the pitch must deliberately retain this negative sign.
        poseStack.rotate(Axis.XP.rotationDegrees(-state.sectionPitch));
        // Roll is continuous and comes from the same inverse-distance surface
        // field as yaw and pitch; it is not derived from SUPPORT. The exported
        // models face local -Z, so their roll axis has the opposite sign from
        // the world-forward axis used by the roll calculation.
        poseStack.rotate(Axis.ZP.rotationDegrees(-state.sectionRoll));
    }

    @Override
    public void submit(RenderState state, PoseStack poseStack, SubmitNodeCollector collector,
                       CameraRenderState camera) {
        super.submit(state, poseStack, collector, camera);
        if (state.head || state.isInvisible) return;

        poseStack.pushPose();
        this.setupRotations(state, poseStack, state.bodyRot, state.scale);
        // glTF is already Y-up. Only mirror X to match Minecraft's entity
        // handedness; applying the usual model Y inversion would turn it over.
        poseStack.scale(-1.0F, 1.0F, 1.0F);
        int overlay = LivingEntityRenderer.getOverlayCoords(state, 0.0F);
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(BODY_TEXTURE),
                (pose, consumer) -> this.bodyMesh.render(pose, consumer, state.lightCoords,
                        overlay, state.ageInTicks, state.moving));
        poseStack.popPose();
    }

    public static final class RenderState extends LivingEntityRenderState {
        public boolean head;
        public boolean moving;
        public float sectionPitch;
        public float sectionRoll;
    }
}
