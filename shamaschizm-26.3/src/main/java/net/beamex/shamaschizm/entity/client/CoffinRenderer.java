package net.beamex.shamaschizm.entity.client;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.entity.custom.CoffinEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.phys.AABB;

public final class CoffinRenderer extends MobRenderer<CoffinEntity, CoffinRenderer.RenderState, CoffinModel> {
    private static final Identifier TEXTURE = Shamaschizm.id("textures/entity/coffin1.png");

    public CoffinRenderer(EntityRendererProvider.Context context) {
        super(context, new CoffinModel(context.bakeLayer(CoffinModel.LAYER_LOCATION)), 0.0F);
    }

    @Override public RenderState createRenderState() { return new RenderState(); }
    @Override public Identifier getTextureLocation(RenderState state) { return TEXTURE; }

    @Override
    protected AABB getBoundingBoxForCulling(CoffinEntity entity, float partialTicks) {
        return new AABB(entity.getX() - 1.2D, entity.getY(), entity.getZ() - 1.2D,
                entity.getX() + 1.2D, entity.getY() + 1.5D, entity.getZ() + 1.2D);
    }

    @Override
    public void extractRenderState(CoffinEntity entity, RenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.open = entity.isOpen();
        state.dirtVisible = entity.isDirtVisible();
        state.openAnimation.copyFrom(entity.getClientOpenAnimation());
    }

    public static final class RenderState extends LivingEntityRenderState {
        public boolean open;
        public boolean dirtVisible;
        public final AnimationState openAnimation = new AnimationState();
    }
}
