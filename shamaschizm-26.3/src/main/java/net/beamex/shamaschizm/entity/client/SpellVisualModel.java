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

public final class SpellVisualModel extends EntityModel<SpellVisualRenderer.RenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Shamaschizm.id("coffin_spell"), "main");
    private final KeyframeAnimation rotate;

    public SpellVisualModel(ModelPart root) {
        super(root);
        this.rotate = SpellVisualAnimations.ROTATE.bake(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("bone", CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-24, 0, -24, 48, 0, 48, new CubeDeformation(0)),
                PartPose.offset(0, 24, 0));
        return LayerDefinition.create(mesh, 256, 256);
    }

    @Override
    public void setupAnim(SpellVisualRenderer.RenderState state) {
        super.setupAnim(state);
        this.rotate.apply(state.rotateAnimation, state.ageInTicks);
    }
}
