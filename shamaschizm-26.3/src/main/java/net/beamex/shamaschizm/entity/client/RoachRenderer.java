package net.beamex.shamaschizm.entity.client;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.entity.custom.RoachEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public final class RoachRenderer extends MobRenderer<RoachEntity, RoachRenderer.RoachRenderState, RoachModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            Shamaschizm.MOD_ID, "textures/entity/roach.png");

    public RoachRenderer(EntityRendererProvider.Context context) {
        super(context, new RoachModel(context.bakeLayer(RoachModel.LAYER_LOCATION)), 0.35F);
    }

    @Override
    public RoachRenderState createRenderState() {
        return new RoachRenderState();
    }

    @Override
    public Identifier getTextureLocation(RoachRenderState state) {
        return TEXTURE;
    }

    public static final class RoachRenderState extends LivingEntityRenderState {
    }
}
