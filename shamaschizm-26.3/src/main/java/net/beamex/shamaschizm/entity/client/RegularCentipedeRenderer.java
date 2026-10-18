package net.beamex.shamaschizm.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.entity.custom.RegularCentipedeEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.core.Direction;

public final class RegularCentipedeRenderer extends MobRenderer<RegularCentipedeEntity,
        RegularCentipedeRenderer.RenderState, RegularCentipedeModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            Shamaschizm.MOD_ID, "textures/entity/regular_centipede.png");

    public RegularCentipedeRenderer(EntityRendererProvider.Context context) {
        super(context, new RegularCentipedeModel(
                context.bakeLayer(RegularCentipedeModel.LAYER_LOCATION)), 0.10F);
    }

    @Override
    public RenderState createRenderState() {
        return new RenderState();
    }

    @Override
    public void extractRenderState(RegularCentipedeEntity entity, RenderState state,
                                   float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.sectionCount = entity.getSectionCount();
        state.support = entity.getSupportDirection();
        state.curled = entity.isCurled();
    }

    @Override
    protected void setupRotations(RenderState state, PoseStack poseStack, float bodyRot, float scale) {
        switch (state.support) {
            case UP -> poseStack.rotate(Axis.ZP.rotationDegrees(180.0F));
            case NORTH -> poseStack.rotate(Axis.XP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.rotate(Axis.XP.rotationDegrees(-90.0F));
            case WEST -> poseStack.rotate(Axis.ZP.rotationDegrees(-90.0F));
            case EAST -> poseStack.rotate(Axis.ZP.rotationDegrees(90.0F));
            default -> { }
        }
        super.setupRotations(state, poseStack, bodyRot, scale);
    }

    @Override
    public Identifier getTextureLocation(RenderState state) {
        return TEXTURE;
    }

    public static final class RenderState extends LivingEntityRenderState {
        public int sectionCount = 5;
        public Direction support = Direction.DOWN;
        public boolean curled;
    }
}
