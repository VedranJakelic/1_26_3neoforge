package net.beamex.shamaschizm.entity.client;

import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** The supplied 2 x 1 x 3 pixel Blockbench baby-roach model. */
public final class BabyRoachModel extends EntityModel<BabyRoachRenderer.BabyRoachRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(Shamaschizm.MOD_ID, "baby_roach"), "main");

    private final ModelPart body;

    public BabyRoachModel(ModelPart root) {
        super(root);
        this.body = root.getChild("bb_main");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("bb_main", CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-1.0F, -1.0F, -1.0F, 2.0F, 1.0F, 3.0F),
                PartPose.offset(0.0F, 24.0F, 0.0F));
        return LayerDefinition.create(mesh, 16, 16);
    }

    @Override
    public void setupAnim(BabyRoachRenderer.BabyRoachRenderState state) {
        super.setupAnim(state);
        float speed = Mth.clamp(state.walkAnimationSpeed * 3.0F, 0.0F, 1.0F);
        this.body.y += Mth.abs(Mth.sin(state.walkAnimationPos * 6.0F)) * 0.10F * speed;
        this.body.zRot += Mth.sin(state.walkAnimationPos * 3.0F) * 0.035F * speed;
    }
}
