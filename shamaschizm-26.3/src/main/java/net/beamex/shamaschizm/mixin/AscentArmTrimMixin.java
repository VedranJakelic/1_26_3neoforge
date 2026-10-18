package net.beamex.shamaschizm.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.beamex.shamaschizm.ascent.client.AscentTrimTextures;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SpriteCoordinateExpander;
import org.spongepowered.asm.mixin.Mixin;

/** Suppress the busy sleeve pattern only during the actual Ascent trim draw.
 * Doing this at draw time is important: model submissions are deferred in 26.3.
 */
@Mixin(Model.class)
public abstract class AscentArmTrimMixin {
    @WrapMethod(method = "renderToBuffer")
    private void shamaschizm$cleanAscentSleeves(PoseStack poses, VertexConsumer buffer,
            int light, int overlay, int color, Operation<Void> original) {
        ModelPart root = ((Model<?>)(Object)this).root();
        if (!(buffer instanceof SpriteCoordinateExpander expander)
                || !(expander.mapping() instanceof AscentTrimTextures.AscentHandle)
                || !root.hasChild("left_arm") || !root.hasChild("right_arm")) {
            original.call(poses, buffer, light, overlay, color);
            return;
        }
        ModelPart left = root.getChild("left_arm");
        ModelPart right = root.getChild("right_arm");
        boolean leftVisible = left.visible;
        boolean rightVisible = right.visible;
        try {
            left.visible = false;
            right.visible = false;
            original.call(poses, buffer, light, overlay, color);
        } finally {
            left.visible = leftVisible;
            right.visible = rightVisible;
        }
    }
}
