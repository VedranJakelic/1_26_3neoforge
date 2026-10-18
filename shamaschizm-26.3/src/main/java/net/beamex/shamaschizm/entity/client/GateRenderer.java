package net.beamex.shamaschizm.entity.client;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.entity.custom.GateEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.phys.AABB;

public final class GateRenderer extends MobRenderer<GateEntity, GateRenderer.GateRenderState, GateModel> {
    private static final Identifier TEXTURE = Shamaschizm.id("textures/entity/gate1.png");

    public GateRenderer(EntityRendererProvider.Context context) {
        super(context, new GateModel(context.bakeLayer(GateModel.LAYER_LOCATION)), 0.0F);
    }

    @Override
    public GateRenderState createRenderState() {
        return new GateRenderState();
    }

    @Override
    public Identifier getTextureLocation(GateRenderState state) {
        return TEXTURE;
    }

    @Override
    protected int getBlockLightLevel(GateEntity entity, BlockPos pos) {
        // The supplied burgundy texture is intentionally very dark. A modest
        // render-only floor keeps its detail readable without emitting light.
        return Math.max(7, Math.max(super.getBlockLightLevel(entity, pos), Math.max(
                entity.level().getBrightness(LightLayer.BLOCK, pos.relative(entity.getGateFacing())),
                entity.level().getBrightness(LightLayer.BLOCK, pos.relative(entity.getGateFacing().getOpposite())))));
    }

    @Override
    protected int getSkyLightLevel(GateEntity entity, BlockPos pos) {
        return Math.max(super.getSkyLightLevel(entity, pos), Math.max(
                entity.level().getBrightness(LightLayer.SKY, pos.relative(entity.getGateFacing())),
                entity.level().getBrightness(LightLayer.SKY, pos.relative(entity.getGateFacing().getOpposite()))));
    }

    @Override
    protected AABB getBoundingBoxForCulling(GateEntity entity, float partialTicks) {
        // Rendering must continue to use the model's full 3x3 extent after the
        // physical entity hitbox is reduced on opening.
        return new AABB(entity.getX() - 1.6D, entity.getY(), entity.getZ() - 1.6D,
                entity.getX() + 1.6D, entity.getY() + 3.1D, entity.getZ() + 1.6D);
    }

    @Override
    public void extractRenderState(GateEntity entity, GateRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.open = entity.isOpen();
        state.openAnimation.copyFrom(entity.getClientOpenAnimation());
    }

    public static final class GateRenderState extends LivingEntityRenderState {
        public boolean open;
        public final AnimationState openAnimation = new AnimationState();
    }
}
