package net.beamex.shamaschizm.entity.client;

import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.monster.skeleton.SkeletonModel;
import net.minecraft.util.Mth;

/** Vanilla skeleton rig, supplied banner pieces, and the evoker-style coffin cast pose. */
public final class SkeletonVariantModel extends SkeletonModel<SkeletonVariantRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Shamaschizm.id("skeleton_variant1"), "main");

    public SkeletonVariantModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();
        createDefaultSkeletonMesh(root);

        root.getChild("head").addOrReplaceChild("hat",
                CubeListBuilder.create().texOffs(32, 0)
                        .addBox(-4, -8, -4, 8, 8, 8), PartPose.ZERO);
        root.getChild("body").addOrReplaceChild("banner", CubeListBuilder.create()
                        .texOffs(46, 0).addBox(-6, 0, -2.5F, 9, 18, 0)
                        .texOffs(46, 0).addBox(-6, 0, 2.5F, 9, 18, 0)
                        .texOffs(55, 21).addBox(-6, 0, -2.5F, 2, 0, 5),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(SkeletonVariantRenderState state) {
        super.setupAnim(state);
        if (!state.coffinSummoning) return;

        this.rightArm.xRot = Mth.cos(state.ageInTicks * 0.6662F) * 0.25F;
        this.leftArm.xRot = Mth.cos(state.ageInTicks * 0.6662F) * 0.25F;
        this.rightArm.yRot = 0.0F;
        this.leftArm.yRot = 0.0F;
        this.rightArm.zRot = 2.3561945F;
        this.leftArm.zRot = -2.3561945F;
    }
}
