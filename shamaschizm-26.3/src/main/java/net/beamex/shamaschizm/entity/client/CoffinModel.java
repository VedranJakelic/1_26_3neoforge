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

public final class CoffinModel extends EntityModel<CoffinRenderer.RenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Shamaschizm.id("coffin"), "main");

    private final ModelPart dirt;
    private final KeyframeAnimation open;

    public CoffinModel(ModelPart root) {
        super(root);
        this.dirt = root.getChild("dirt");
        this.open = CoffinAnimations.OPEN.bake(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition bone = root.addOrReplaceChild("bone", CubeListBuilder.create()
                .texOffs(0, 0).addBox(7, -9, -24, 1, 9, 32, new CubeDeformation(0))
                .texOffs(0, 41).addBox(-8, -9, -24, 1, 9, 32, new CubeDeformation(0))
                .texOffs(64, 86).addBox(-7, -9, -24, 14, 9, 1, new CubeDeformation(0))
                .texOffs(94, 86).addBox(-7, -9, 7, 14, 9, 1, new CubeDeformation(0)),
                PartPose.offset(0, 24, 16));
        bone.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(66, 0)
                        .addBox(0, -14, -7, 1, 14, 30, new CubeDeformation(0)),
                PartPose.offsetAndRotation(-7, -1, -16, 0, 0, 1.5708F));

        PartDefinition lid = root.addOrReplaceChild("lid", CubeListBuilder.create()
                .texOffs(66, 68).addBox(-2, -3, -16.9887F, 16, 2, 16, new CubeDeformation(0))
                .texOffs(0, 82).addBox(-2, -3, -0.9887F, 16, 2, 16, new CubeDeformation(0))
                .texOffs(66, 44).addBox(0, -4, -9.9887F, 12, 1, 23, new CubeDeformation(0)),
                PartPose.offset(-6, 16, 9));
        lid.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(-16, 0)
                        .addBox(-8, -0.2F, -8, 16, 0, 16, new CubeDeformation(0)),
                PartPose.offsetAndRotation(6, -4, 4, 0, 0.7854F, 0));

        root.addOrReplaceChild("dirt", CubeListBuilder.create().texOffs(0, 100)
                        .addBox(-7, -7, -7, 14, 7, 30, new CubeDeformation(0)),
                PartPose.offset(0, 24, 0));
        return LayerDefinition.create(mesh, 256, 256);
    }

    @Override
    public void setupAnim(CoffinRenderer.RenderState state) {
        super.setupAnim(state);
        this.dirt.visible = state.dirtVisible;
        if (state.open) this.open.apply(state.openAnimation, state.ageInTicks);
    }
}
