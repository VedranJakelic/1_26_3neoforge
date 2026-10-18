package net.beamex.shamaschizm.entity.client;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.entity.custom.FrogGodEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public final class FrogGodRenderer
        extends MobRenderer<FrogGodEntity, FrogGodRenderer.RenderState, FrogGodModel> {
    private static final Identifier NORMAL = Shamaschizm.id("textures/entity/frog_god.png");
    private static final Identifier BLESSED = Shamaschizm.id("textures/entity/frog_god2.png");

    public FrogGodRenderer(EntityRendererProvider.Context context) {
        super(context, new FrogGodModel(context.bakeLayer(FrogGodModel.LAYER_LOCATION)), 0.7F);
    }

    @Override
    public RenderState createRenderState() {
        return new RenderState();
    }

    @Override
    public void extractRenderState(FrogGodEntity entity, RenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.action = entity.getAction();
        state.actionTicks = entity.getClientAnimationTicks(partialTick);
        state.blessed = entity.usesBlessedTexture();
    }

    @Override
    public Identifier getTextureLocation(RenderState state) {
        return state.blessed ? BLESSED : NORMAL;
    }

    public static final class RenderState extends LivingEntityRenderState {
        public int action;
        public float actionTicks;
        public boolean blessed;
    }
}
