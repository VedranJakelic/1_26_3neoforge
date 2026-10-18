package net.beamex.shamaschizm.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.entity.custom.SkeletonVariantEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AbstractSkeletonRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Pose;

public final class SkeletonVariantRenderer
        extends AbstractSkeletonRenderer<SkeletonVariantEntity, SkeletonVariantRenderState> {
    private static final Identifier TEXTURE = Shamaschizm.id("textures/entity/skeleton_variant1.png");

    public SkeletonVariantRenderer(EntityRendererProvider.Context context) {
        super(context, ModelLayers.SKELETON_ARMOR,
                new SkeletonVariantModel(context.bakeLayer(SkeletonVariantModel.LAYER_LOCATION)));
    }

    @Override public Identifier getTextureLocation(SkeletonVariantRenderState state) { return TEXTURE; }
    @Override public SkeletonVariantRenderState createRenderState() { return new SkeletonVariantRenderState(); }

    @Override
    public void extractRenderState(SkeletonVariantEntity entity, SkeletonVariantRenderState state,
                                   float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.coffinSummoning = entity.isCoffinSummoning();
        state.coffinResting = entity.getPose() == Pose.SLEEPING;
    }

    @Override
    protected void setupRotations(SkeletonVariantRenderState state, PoseStack poseStack,
                                  float bodyRot, float entityScale) {
        if (!state.coffinResting) {
            super.setupRotations(state, poseStack, bodyRot, entityScale);
            return;
        }

        // The skeleton model's longitudinal direction is opposite the coffin-facing
        // convention: add 180 degrees so its head points toward the head end.
        poseStack.rotate(Axis.YP.rotationDegrees(bodyRot + 180.0F));

        // Apply the lengthwise adjustment after yaw. It is now in coffin/model space,
        // so the offset rotates correctly for north, east, south, and west.
        poseStack.translate(1.8F, 0.0F, 0.0F);

        // Keep the normal face-up sleeping roll, but do not apply the bed-pillow translation.
        poseStack.rotate(Axis.ZP.rotationDegrees(this.getFlipDegrees()));
        poseStack.rotate(Axis.YP.rotationDegrees(270.0F));
    }
}
