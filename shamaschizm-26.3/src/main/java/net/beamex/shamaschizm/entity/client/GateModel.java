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
import net.minecraft.resources.Identifier;

/** The supplied two-panel, three-block-wide and three-block-tall gate model. */
public final class GateModel extends EntityModel<GateRenderer.GateRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(Shamaschizm.MOD_ID, "gate"), "main");

    private final KeyframeAnimation open;

    public GateModel(ModelPart root) {
        super(root);
        this.open = GateAnimations.OPEN.bake(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("bone", CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-24.0F, -48.0F, -1.0F, 24.0F, 48.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(24.0F, 24.0F, 0.0F));
        PartDefinition bone2 = root.addOrReplaceChild("bone2", CubeListBuilder.create(),
                PartPose.offset(-24.0F, 24.0F, 0.0F));
        bone2.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-24.0F, -48.0F, -1.0F, 24.0F, 48.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 3.1416F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(GateRenderer.GateRenderState state) {
        super.setupAnim(state);
        if (state.open) {
            this.open.apply(state.openAnimation, state.ageInTicks);
        }
    }
}
