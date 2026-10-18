package net.beamex.shamaschizm.plantedsword.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.beamex.shamaschizm.plantedsword.PlantedSwordEntity;
import net.beamex.shamaschizm.plantedsword.PlantedSwords;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.LightLayer;

/** Uses the sword's real held-item model, including its components and enchantment glint. */
public final class PlantedSwordRenderer extends EntityRenderer<PlantedSwordEntity, PlantedSwordRenderer.State> {
    public PlantedSwordRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override public State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(PlantedSwordEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.spear = PlantedSwords.isSpear(entity.sword());
        state.yaw = entity.getYRot();
        state.leanX = entity.leanX();
        state.leanZ = entity.leanZ();
        Minecraft.getInstance().getItemModelResolver().updateForNonLiving(
                state.item, entity.sword(), ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, entity);
        // Sample above the surface, never inside the buried tip. The sword
        // still responds to actual daylight/torches instead of glowing fullbright.
        BlockPos lower = BlockPos.containing(entity.getX(), entity.getY() + 0.35D, entity.getZ());
        BlockPos upper = BlockPos.containing(entity.getX(), entity.getY() + 0.85D, entity.getZ());
        int block = Math.max(entity.level().getBrightness(LightLayer.BLOCK, lower),
                entity.level().getBrightness(LightLayer.BLOCK, upper));
        int sky = Math.max(entity.level().getBrightness(LightLayer.SKY, lower),
                entity.level().getBrightness(LightLayer.SKY, upper));
        // Packed light: block light in bits 4..7, sky light in bits 20..23.
        state.lightCoords = (block << 4) | (sky << 20);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state, pose, collector, camera);
        pose.pushPose();
        pose.rotate(Axis.YP.rotationDegrees(-state.yaw));
        // Tilt about the ground insertion point, not a moving/bobbing item origin.
        pose.rotate(Axis.XP.rotationDegrees(state.leanX));
        pose.rotate(Axis.ZP.rotationDegrees(state.leanZ));
        if (state.spear) {
            pose.translate(0.0F, 1.25F, 0.0F);
            pose.scale(1.30F, 1.30F, 1.30F);
            // Spear tip runs toward (-X,+Y) in the vanilla held texture.
            // Undo its XYZ(5,270,-40) display rotation in reverse order,
            // retaining the model's 1.7/1.7/0.85 held scale.
            pose.rotate(Axis.ZP.rotationDegrees(135.0F));
            pose.rotate(Axis.ZP.rotationDegrees(40.0F));
            pose.rotate(Axis.YP.rotationDegrees(-270.0F));
            pose.rotate(Axis.XP.rotationDegrees(-5.0F));
            pose.translate(0.0F, -2.0F / 16.0F, -2.0F / 16.0F);
        } else {
            pose.translate(0.0F, 0.52F, 0.0F);
            pose.scale(1.30F, 1.30F, 1.30F);
            // The standard handheld sword's blade runs along (+X,+Y).
            // Rotate that diagonal down into the ground; retain the held model's
            // 0.85 scale, but undo its display rotation and hand translation.
            pose.rotate(Axis.ZP.rotationDegrees(-135.0F));
            pose.rotate(Axis.ZP.rotationDegrees(-55.0F));
            pose.rotate(Axis.YP.rotationDegrees(90.0F));
            pose.translate(0.0F, -4.0F / 16.0F, -0.5F / 16.0F);
        }
        state.item.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        pose.popPose();
    }

    public static final class State extends EntityRenderState {
        public final ItemStackRenderState item = new ItemStackRenderState();
        public boolean spear;
        public float yaw;
        public float leanX;
        public float leanZ;
    }
}
