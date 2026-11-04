package net.beamex.shamaschizm.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.beamex.shamaschizm.banner.TatteredBannerMarker;
import net.beamex.shamaschizm.banner.client.TatteredBannerGeometry;
import net.beamex.shamaschizm.banner.client.TatteredBannerState;
import net.minecraft.client.model.object.banner.BannerFlagModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.blockentity.state.BannerRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Change ONLY tattered banners' cloth geometry; vanilla textures/pattern layers remain. */
@Mixin(BannerRenderer.class)
public abstract class TatteredBannerRendererMixin {
    /** Grayscale pixels are multiplied by the vanilla DyeColor tint at render time. */
    @Unique private static final SpriteId SHAMASCHIZM_TATTERED_CLOTH =
            Sheets.BANNER_MAPPER.apply(Shamaschizm.id("tattered_base"));

    @Unique private BannerFlagModel shamaschizm$tatteredStanding;
    @Unique private BannerFlagModel shamaschizm$tatteredWall;

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void shamaschizm$rememberFlag(BannerBlockEntity entity, BannerRenderState state,
            float partialTick, Vec3 cameraPosition,
            ModelFeatureRenderer.CrumblingOverlay breakProgress, CallbackInfo ci) {
        ((TatteredBannerState) state).shamaschizm$setTattered(
                entity.getBlockState().getBlock() instanceof TatteredBannerMarker);
    }

    @ModifyArgs(method = "submit",
            at = @At(value = "INVOKE", target =
                    "Lnet/minecraft/client/renderer/blockentity/BannerRenderer;submitBanner(" +
                    "Lnet/minecraft/client/resources/model/sprite/SpriteGetter;" +
                    "Lcom/mojang/blaze3d/vertex/PoseStack;" +
                    "Lnet/minecraft/client/renderer/SubmitNodeCollector;II" +
                    "Lnet/minecraft/client/model/object/banner/BannerModel;" +
                    "Lnet/minecraft/client/model/object/banner/BannerFlagModel;" +
                    "FLnet/minecraft/world/item/DyeColor;" +
                    "Lnet/minecraft/world/level/block/entity/BannerPatternLayers;" +
                    "Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;I)V"))
    private void shamaschizm$replaceFlag(Args args,
            BannerRenderState state, PoseStack poseStack,
            SubmitNodeCollector nodes, CameraRenderState camera) {
        if (!((TatteredBannerState) state).shamaschizm$isTattered()) return;

        // submitBanner draws both the pole (BANNER_BASE) and the cloth
        // (BANNER_PATTERN_BASE). Change ONLY the cloth's base sprite.
        // DyeColor still tints it normally, so all source grayscale wear,
        // folds and dirt-darkened pixels remain visible after dyeing.
        // Loom pattern sprites are untouched, preserving their colors/data.
        final SpriteGetter originalSprites = args.get(0);
        final SpriteGetter tatteredSprites = sprite -> originalSprites.get(
                sprite.equals(Sheets.BANNER_PATTERN_BASE)
                        ? SHAMASCHIZM_TATTERED_CLOTH : sprite);
        args.set(0, tatteredSprites);

        if (state.attachmentType == BannerBlock.AttachmentType.GROUND) {
            if (shamaschizm$tatteredStanding == null) {
                shamaschizm$tatteredStanding = new BannerFlagModel(
                        TatteredBannerGeometry.createFlagLayer(true).bakeRoot());
            }
            args.set(6, shamaschizm$tatteredStanding);
            return;
        }
        if (shamaschizm$tatteredWall == null) {
            shamaschizm$tatteredWall = new BannerFlagModel(
                    TatteredBannerGeometry.createFlagLayer(false).bakeRoot());
        }
        args.set(6, shamaschizm$tatteredWall);
    }
}
