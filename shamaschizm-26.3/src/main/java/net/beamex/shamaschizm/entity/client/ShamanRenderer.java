package net.beamex.shamaschizm.entity.client;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.entity.custom.ShamanEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.AnimationState;


public final class ShamanRenderer extends MobRenderer<ShamanEntity, ShamanRenderer.ShamanRenderState, ShamanModel> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(Shamaschizm.MOD_ID, "textures/entity/shaman.png");

    public ShamanRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new ShamanModel(ctx.bakeLayer(ShamanModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public ShamanRenderState createRenderState() {
        return new ShamanRenderState();
    }

    @Override
    public void extractRenderState(ShamanEntity entity, ShamanRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);


        state.idle.copyFrom(entity.idleAnimationState);
    }

    @Override
    public Identifier getTextureLocation(ShamanRenderState state) {
        return TEXTURE;
    }
    public Identifier getTextureLocation(ShamanEntity entity) {
        return TEXTURE;
    }


    public static final class ShamanRenderState extends LivingEntityRenderState {
        public final AnimationState idle = new AnimationState();
    }
}
