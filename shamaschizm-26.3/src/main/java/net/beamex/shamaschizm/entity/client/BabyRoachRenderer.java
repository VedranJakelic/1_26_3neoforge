package net.beamex.shamaschizm.entity.client;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.entity.custom.BabyRoachEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public final class BabyRoachRenderer extends MobRenderer<
        BabyRoachEntity, BabyRoachRenderer.BabyRoachRenderState, BabyRoachModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            Shamaschizm.MOD_ID, "textures/entity/baby_roach.png");

    public BabyRoachRenderer(EntityRendererProvider.Context context) {
        super(context,
                new BabyRoachModel(context.bakeLayer(BabyRoachModel.LAYER_LOCATION)),
                0.12F);
    }

    @Override
    public BabyRoachRenderState createRenderState() {
        return new BabyRoachRenderState();
    }

    @Override
    public Identifier getTextureLocation(BabyRoachRenderState state) {
        return TEXTURE;
    }

    public static final class BabyRoachRenderState extends LivingEntityRenderState {
    }
}
