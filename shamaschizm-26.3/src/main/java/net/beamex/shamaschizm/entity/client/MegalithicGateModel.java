package net.beamex.shamaschizm.entity.client;

import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** The supplied solid 3x3x0.875-block megalithic gate model. */
public final class MegalithicGateModel extends EntityModel<MegalithicGateRenderer.RenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Shamaschizm.id("megalithic_gate"), "main");
    private final KeyframeAnimation open;

    public MegalithicGateModel(ModelPart root) {
        super(root);
        open = MegalithicGateAnimations.OPEN.bake(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("bone", CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-24.0F, -48.0F, -7.0F, 48.0F, 48.0F, 14.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 24.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(MegalithicGateRenderer.RenderState state) {
        super.setupAnim(state);
        if (state.open) open.apply(state.openAnimation, state.ageInTicks);
    }
}
