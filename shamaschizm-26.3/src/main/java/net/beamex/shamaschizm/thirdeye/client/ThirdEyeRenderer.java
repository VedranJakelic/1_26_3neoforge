package net.beamex.shamaschizm.thirdeye.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.beamex.shamaschizm.thirdeye.ThirdEyeBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.AbstractEndPortalRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.EndPortalRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/** Renders the model's inset 15.6-pixel cube with the vanilla End Portal effect. */
public final class ThirdEyeRenderer
        extends AbstractEndPortalRenderer<ThirdEyeBlockEntity, EndPortalRenderState> {
    private static final float INSET = 0.2F / 16.0F;
    private static final float SIZE = 15.6F / 16.0F;

    public ThirdEyeRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public EndPortalRenderState createRenderState() {
        return new EndPortalRenderState();
    }

    @Override
    public void submit(EndPortalRenderState state, PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(INSET, INSET, INSET);
        poseStack.scale(SIZE, SIZE, SIZE);
        submitCube(state.facesToShow, RenderTypes.endPortal(), poseStack, collector);
        poseStack.popPose();
    }
}
