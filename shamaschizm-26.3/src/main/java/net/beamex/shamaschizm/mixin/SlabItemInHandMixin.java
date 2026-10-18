package net.beamex.shamaschizm.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.slab.SlabRegistration;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps vanilla's map hands and replaces only the flat map sheet. */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class SlabItemInHandMixin {
    private static final Identifier SLAB_TEXTURE =
            Shamaschizm.id("textures/item/slab01.png");

    @Inject(method = "renderMap", at = @At("HEAD"), cancellable = true)
    private void shamaschizm$renderSlab(PoseStack poseStack,
                                        SubmitNodeCollector submitNodeCollector,
                                        int lightCoords, ItemStack stack,
                                        boolean mainHand, net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState renderState, CallbackInfo ci) {
        if (!stack.is(SlabRegistration.SLAB)) return;
        ci.cancel();

        poseStack.pushPose();
        poseStack.rotate(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
        poseStack.rotate(com.mojang.math.Axis.ZP.rotationDegrees(180.0F));
        poseStack.scale(0.38F, 0.38F, 0.38F);
        poseStack.translate(-0.5F, -0.5F, 0.0F);
        poseStack.scale(1.0F / 128.0F, 1.0F / 128.0F, 1.0F / 128.0F);

        RenderType renderType = RenderTypes.text(net.beamex.shamaschizm.slab.SlabAppearance.isSecond(stack)
                ? Shamaschizm.id("textures/item/slab02.png") : SLAB_TEXTURE);
        // Same vertex format and UV orientation as vanilla maps. A single plane
        // preserves PNG transparency and removes the mirrored rear face and sides.
        submitNodeCollector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            buffer.addVertex(pose, 0.0F, 128.0F, 0.0F).setColor(-1).setUv(0.0F, 1.0F).setLight(lightCoords);
            buffer.addVertex(pose, 128.0F, 128.0F, 0.0F).setColor(-1).setUv(1.0F, 1.0F).setLight(lightCoords);
            buffer.addVertex(pose, 128.0F, 0.0F, 0.0F).setColor(-1).setUv(1.0F, 0.0F).setLight(lightCoords);
            buffer.addVertex(pose, 0.0F, 0.0F, 0.0F).setColor(-1).setUv(0.0F, 0.0F).setLight(lightCoords);
        });
        poseStack.popPose();
    }
}
