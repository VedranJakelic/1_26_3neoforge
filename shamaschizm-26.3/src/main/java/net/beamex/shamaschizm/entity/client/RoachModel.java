package net.beamex.shamaschizm.entity.client;

import net.beamex.shamaschizm.Shamaschizm;
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
import net.minecraft.util.Mth;

/** Blockbench roach model, cleaned for Minecraft 26.2's render-state API. */
public final class RoachModel extends EntityModel<RoachRenderer.RoachRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(Shamaschizm.MOD_ID, "roach"), "main");

    private final ModelPart body;
    private final ModelPart front1;
    private final ModelPart mid1;
    private final ModelPart hind1;
    private final ModelPart front2;
    private final ModelPart mid2;
    private final ModelPart hind2;

    public RoachModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        ModelPart body7 = this.body.getChild("body7");
        ModelPart legs1 = body7.getChild("legs1");
        ModelPart legs2 = body7.getChild("legs2");
        this.front1 = legs1.getChild("front1");
        this.mid1 = legs1.getChild("mid1");
        this.hind1 = legs1.getChild("hind1");
        this.front2 = legs2.getChild("front2");
        this.mid2 = legs2.getChild("mid2");
        this.hind2 = legs2.getChild("hind2");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

        body.addOrReplaceChild("body1", CubeListBuilder.create().texOffs(0, 23)
                .addBox(-1.5F, 0.0F, 0.0F, 3.0F, 2.0F, 1.0F), PartPose.offset(0.0F, -2.0F, -7.0F));
        body.addOrReplaceChild("body2", CubeListBuilder.create().texOffs(20, 15)
                .addBox(-2.0F, 1.0F, -1.0F, 4.0F, 2.0F, 1.0F), PartPose.offset(0.0F, -3.0F, -5.0F));
        body.addOrReplaceChild("body3", CubeListBuilder.create().texOffs(20, 7)
                .addBox(-3.0F, 2.0F, -2.5F, 6.0F, 2.0F, 2.0F), PartPose.offset(0.0F, -4.0F, -2.5F));
        body.addOrReplaceChild("body4", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-3.5F, 0.8F, -3.5F, 7.0F, 2.0F, 5.0F, new CubeDeformation(-0.02F)),
                PartPose.offset(0.0F, -3.0F, 0.5F));
        body.addOrReplaceChild("body5", CubeListBuilder.create().texOffs(18, 18)
                .addBox(-3.0F, 0.0F, -1.5F, 6.0F, 2.0F, 3.0F), PartPose.offset(0.0F, -2.0F, 3.5F));
        body.addOrReplaceChild("body6", CubeListBuilder.create().texOffs(22, 10)
                .addBox(-2.0F, -1.0F, -2.0F, 4.0F, 2.0F, 3.0F, new CubeDeformation(-0.2F)),
                PartPose.offset(0.0F, -1.0F, 6.0F));

        PartDefinition body7 = body.addOrReplaceChild("body7", CubeListBuilder.create().texOffs(37, 15)
                .addBox(-0.5F, 0.0F, -2.0F, 1.0F, 1.0F, 2.0F), PartPose.offset(0.0F, -1.0F, 8.0F));
        PartDefinition legs1 = body7.addOrReplaceChild("legs1", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1309F));
        legs1.addOrReplaceChild("front1", CubeListBuilder.create().texOffs(0, 18)
                .addBox(0.0F, 0.0F, -5.0F, 4.0F, 0.0F, 5.0F), PartPose.offset(3.0F, 0.0F, -11.0F));
        legs1.addOrReplaceChild("mid1", CubeListBuilder.create().texOffs(0, 14)
                .addBox(0.0F, 0.0F, -1.0F, 6.0F, 0.0F, 4.0F), PartPose.offset(3.0F, 0.0F, -10.0F));
        legs1.addOrReplaceChild("hind1", CubeListBuilder.create().texOffs(0, 7)
                .addBox(0.0F, 0.0F, -1.0F, 4.0F, 1.0F, 6.0F), PartPose.offset(3.0F, 0.0F, -6.0F));

        body7.addOrReplaceChild("fur", CubeListBuilder.create().texOffs(21, 2)
                .addBox(-3.0F, -1.4F, -11.0F, 6.0F, 2.0F, 0.0F)
                .texOffs(51, 1).addBox(-3.0F, -1.4F, -6.0F, 6.0F, 2.0F, 0.0F), PartPose.ZERO);

        PartDefinition legs2 = body7.addOrReplaceChild("legs2", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.1309F));
        legs2.addOrReplaceChild("front2", CubeListBuilder.create().texOffs(0, 18).mirror()
                .addBox(-4.0F, 0.0F, -5.0F, 4.0F, 0.0F, 5.0F).mirror(false),
                PartPose.offset(-3.0F, 0.0F, -11.0F));
        legs2.addOrReplaceChild("mid2", CubeListBuilder.create().texOffs(0, 14).mirror()
                .addBox(-6.0F, 0.0F, -1.0F, 6.0F, 0.0F, 4.0F).mirror(false),
                PartPose.offset(-3.0F, 0.0F, -10.0F));
        legs2.addOrReplaceChild("hind2", CubeListBuilder.create().texOffs(0, 7).mirror()
                .addBox(-4.0F, 0.0F, -1.0F, 4.0F, 1.0F, 6.0F).mirror(false),
                PartPose.offset(-3.0F, 0.0F, -6.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(RoachRenderer.RoachRenderState state) {
        super.setupAnim(state);
        float speed = Mth.clamp(state.walkAnimationSpeed * 2.5F, 0.0F, 1.0F);
        float pulse = (0.5F + 0.5F * Mth.sin(state.walkAnimationPos * 5.0F)) * speed;
        float radians = Mth.PI / 180.0F;

        // These signs and amplitudes preserve the six leg channels from the supplied animation.
        this.front1.yRot += 17.5F * radians * pulse;
        this.mid1.yRot -= 25.0F * radians * pulse;
        this.hind1.yRot += 22.5F * radians * pulse;
        this.front2.yRot -= 17.5F * radians * pulse;
        this.mid2.yRot += 25.0F * radians * pulse;
        this.hind2.yRot -= 22.5F * radians * pulse;
        this.body.y += Mth.abs(Mth.sin(state.walkAnimationPos * 5.0F)) * 0.15F * speed;
    }
}
