package net.beamex.shamaschizm.entity.client;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.entity.custom.MegalithicGateEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;

public final class MegalithicGateRenderer extends MobRenderer<MegalithicGateEntity,
        MegalithicGateRenderer.RenderState, MegalithicGateModel> {
    private static final Identifier TEXTURE = Shamaschizm.id("textures/entity/megalithic_gate1.png");

    public MegalithicGateRenderer(EntityRendererProvider.Context context) {
        super(context, new MegalithicGateModel(context.bakeLayer(MegalithicGateModel.LAYER_LOCATION)), 0.0F);
    }

    @Override public RenderState createRenderState() { return new RenderState(); }
    @Override public Identifier getTextureLocation(RenderState state) { return TEXTURE; }

//    @Override
//    protected int getBlockLightLevel(MegalithicGateEntity entity, BlockPos pos) {
//        // Keep the engraved supplied texture legible in the naturally dark Schizm.
//        return Math.max(7, Math.max(super.getBlockLightLevel(entity, pos), Math.max(
//                entity.level().getBrightness(LightLayer.BLOCK, pos.relative(entity.getGateFacing())),
//                entity.level().getBrightness(LightLayer.BLOCK, pos.relative(entity.getGateFacing().getOpposite())))));
//    }

    @Override
    protected AABB getBoundingBoxForCulling(MegalithicGateEntity entity, float partialTicks) {
        // The rising animation reaches almost three blocks above the closed model.
        return new AABB(entity.getX() - 1.7D, entity.getY(), entity.getZ() - 1.7D,
                entity.getX() + 1.7D, entity.getY() + 6.1D, entity.getZ() + 1.7D);
    }

    @Override
    public void extractRenderState(MegalithicGateEntity entity, RenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.open = entity.isOpen();
        state.openAnimation.copyFrom(entity.getClientOpenAnimation());
    }

    public static final class RenderState extends LivingEntityRenderState {
        public boolean open;
        public final AnimationState openAnimation = new AnimationState();
    }
}
