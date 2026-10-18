package net.beamex.shamaschizm.entity.client;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.entity.custom.SpellVisualEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.AnimationState;

public final class SpellVisualRenderer extends MobRenderer<SpellVisualEntity,
        SpellVisualRenderer.RenderState, SpellVisualModel> {
    private static final Identifier TEXTURE = Shamaschizm.id("textures/entity/spell1.png");

    public SpellVisualRenderer(EntityRendererProvider.Context context) {
        super(context, new SpellVisualModel(context.bakeLayer(SpellVisualModel.LAYER_LOCATION)), 0.0F);
    }

    @Override public RenderState createRenderState() { return new RenderState(); }
    @Override public Identifier getTextureLocation(RenderState state) { return TEXTURE; }

    @Override
    public void extractRenderState(SpellVisualEntity entity, RenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.rotateAnimation.copyFrom(entity.getRotateAnimation());
    }

    public static final class RenderState extends LivingEntityRenderState {
        public final AnimationState rotateAnimation = new AnimationState();
    }
}
