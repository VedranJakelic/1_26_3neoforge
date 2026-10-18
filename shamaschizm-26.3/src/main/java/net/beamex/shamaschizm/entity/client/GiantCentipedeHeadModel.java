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

/** The supplied Blockbench head. Both end heads continuously use IDLE. */
public final class GiantCentipedeHeadModel extends EntityModel<GiantCentipedeRenderer.RenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Shamaschizm.id("giant_centipede_head"), "main");
    private final ModelPart root;
    private final KeyframeAnimation idle;

    public GiantCentipedeHeadModel(ModelPart root) {
        super(root);
        this.root = root;
        this.idle = GiantCentipedeAnimations.IDLE.bake(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition bone = root.addOrReplaceChild("bone", CubeListBuilder.create(),
                PartPose.offset(-2.0F, 21.0F, -1.0F));
        bone.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-9.0F, 0.0F, -8.0F, 17.0F, 0.0F, 9.0F),
                PartPose.offsetAndRotation(-6.0F, -1.0F, 1.0F, 0.0F, 0.0F, 0.1745F));
        PartDefinition bone2 = root.addOrReplaceChild("bone2", CubeListBuilder.create(),
                PartPose.offset(3.0F, 21.0F, -1.0F));
        bone2.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(0, 9)
                .addBox(-8.0F, 0.0F, -8.0F, 17.0F, 0.0F, 9.0F),
                PartPose.offsetAndRotation(5.0F, -1.0F, 1.0F, 0.0F, 0.0F, -0.1745F));
        root.addOrReplaceChild("bb_main", CubeListBuilder.create().texOffs(34, 18)
                .addBox(-5.0F, -3.0F, -1.0F, 10.0F, 2.0F, 5.0F, new CubeDeformation(0.3F))
                .texOffs(34, 25).addBox(-4.0F, -3.0F, -3.0F, 8.0F, 2.0F, 3.0F)
                .texOffs(0, 18).addBox(0.0F, -1.0F, -8.0F, 8.0F, 0.0F, 9.0F)
                .texOffs(0, 27).addBox(-8.0F, -1.0F, -8.0F, 8.0F, 0.0F, 9.0F),
                PartPose.offset(0.0F, 24.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(GiantCentipedeRenderer.RenderState state) {
        super.setupAnim(state);
        this.root.visible = state.head;
        if (state.head) this.idle.apply((long)(state.ageInTicks * 50.0F), 1.0F);
    }
}
