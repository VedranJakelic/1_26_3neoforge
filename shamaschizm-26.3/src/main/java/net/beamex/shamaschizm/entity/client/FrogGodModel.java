package net.beamex.shamaschizm.entity.client;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.entity.custom.FrogGodEntity;
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

public final class FrogGodModel extends EntityModel<FrogGodRenderer.RenderState> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Shamaschizm.MOD_ID, "frog_god"), "main");
	private final ModelPart tongue;
	private final ModelPart body;
	private final ModelPart croaking_body;
	private final ModelPart left_arm;
	private final ModelPart right_arm;
	private final ModelPart left_leg;
	private final ModelPart right_leg;
	private final ModelPart head_with_eyes;
	private final ModelPart head;
	private final ModelPart eyes;
	private final ModelPart bone;
	private final ModelPart bone2;
	private final ModelPart bone3;
	private final ModelPart root;
	private final KeyframeAnimation wakeup;
	private final KeyframeAnimation croak;
	private final KeyframeAnimation walk;
	private final KeyframeAnimation jump;
	private final KeyframeAnimation openMouth;

	public FrogGodModel(ModelPart root) {
		super(root);
		this.root = root;
		this.tongue = root.getChild("tongue");
		this.body = root.getChild("body");
		this.croaking_body = root.getChild("croaking_body");
		this.left_arm = root.getChild("left_arm");
		this.right_arm = root.getChild("right_arm");
		this.left_leg = root.getChild("left_leg");
		this.right_leg = root.getChild("right_leg");
		this.head_with_eyes = root.getChild("head_with_eyes");
		this.head = this.head_with_eyes.getChild("head");
		this.eyes = this.head_with_eyes.getChild("eyes");
		this.bone = this.eyes.getChild("bone");
		this.bone2 = this.bone.getChild("bone2");
		this.bone3 = this.bone.getChild("bone3");
		this.wakeup = FrogGodAnimations.WAKEUP.bake(root);
		this.croak = FrogGodAnimations.CROAK.bake(root);
		this.walk = FrogGodAnimations.WALK.bake(root);
		this.jump = FrogGodAnimations.JUMP.bake(root);
		this.openMouth = FrogGodAnimations.OPEN_MOUTH.bake(root);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition tongue = partdefinition.addOrReplaceChild("tongue", CubeListBuilder.create().texOffs(66, 41).addBox(-4.0F, -1.0F, -7.0F, 8.0F, 2.0F, 11.0F, new CubeDeformation(0.0F))
		.texOffs(76, 73).addBox(-3.0F, -1.0F, -8.0F, 6.0F, 3.0F, 4.0F, new CubeDeformation(-0.2F)), PartPose.offset(0.0F, 13.0F, -1.0F));

		PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -8.0F, -10.0F, 16.0F, 8.0F, 21.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 22.0F, 0.0F));

		PartDefinition croaking_body = partdefinition.addOrReplaceChild("croaking_body", CubeListBuilder.create().texOffs(0, 55).addBox(-6.0F, -7.0F, -9.0F, 12.0F, 6.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition left_arm = partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(74, 0).addBox(0.0F, -6.0F, -3.0F, 5.0F, 6.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(74, 22).addBox(-2.0F, 0.0F, -4.0F, 6.0F, 0.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(7.0F, 24.0F, -5.0F));

		PartDefinition right_arm = partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(74, 11).addBox(-4.0F, -1.0F, -3.0F, 5.0F, 6.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(76, 67).addBox(-3.0F, 5.0F, -4.0F, 6.0F, 0.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(-8.0F, 19.0F, -5.0F));

		PartDefinition left_leg = partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(42, 67).addBox(-2.0F, -4.0F, -6.0F, 7.0F, 7.0F, 10.0F, new CubeDeformation(0.0F))
		.texOffs(42, 55).addBox(-1.0F, 3.0F, -8.0F, 9.0F, 0.0F, 12.0F, new CubeDeformation(0.0F)), PartPose.offset(9.0F, 21.0F, 8.0F));

		PartDefinition right_leg = partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 70).addBox(-5.0F, -3.0F, -5.0F, 7.0F, 7.0F, 10.0F, new CubeDeformation(0.0F))
		.texOffs(66, 29).addBox(-8.0F, 4.0F, -7.0F, 9.0F, 0.0F, 12.0F, new CubeDeformation(0.0F)), PartPose.offset(-8.0F, 20.0F, 7.0F));

		PartDefinition head_with_eyes = partdefinition.addOrReplaceChild("head_with_eyes", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 14.0F, 10.0F, 0.6109F, 0.0F, 0.0F));

		PartDefinition head = head_with_eyes.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 29).addBox(-7.0F, -5.0F, -18.0F, 14.0F, 7.0F, 19.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, -1.0F, -0.6109F, 0.0F, 0.0F));

		PartDefinition eyes = head_with_eyes.addOrReplaceChild("eyes", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 3.0F, -1.0F, -0.6109F, 0.0F, 0.0F));

		PartDefinition bone = eyes.addOrReplaceChild("bone", CubeListBuilder.create(), PartPose.offset(2.0F, -6.0F, -16.0F));

		PartDefinition bone2 = bone.addOrReplaceChild("bone2", CubeListBuilder.create().texOffs(76, 80).addBox(-1.0F, -3.0F, -1.0F, 4.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-6.0F, 0.0F, 0.0F));

		PartDefinition bone3 = bone.addOrReplaceChild("bone3", CubeListBuilder.create().texOffs(34, 84).addBox(-1.0F, -3.0F, -1.0F, 4.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	@Override
	public void setupAnim(FrogGodRenderer.RenderState state) {
		for (ModelPart part : this.root.getAllParts()) part.resetPose();
		int action = state.action;
		long actionMillis = (long)(state.actionTicks * 50.0F);

		if (action == FrogGodEntity.WAKING) {
			this.wakeup.apply(actionMillis, 1.0F);
		} else if (action == FrogGodEntity.PACIFYING) {
			long reversed = (long)(Math.max(0.0F, 100.0F - state.actionTicks) * 50.0F);
			this.wakeup.apply(reversed, 1.0F);
		} else if (action == FrogGodEntity.WAITING_FOR_TRIBUTE) {
			// WAKEUP is a hold animation; its final pose is the awake base pose.
			this.wakeup.apply(5_000L, 1.0F);
		}

		if (action == FrogGodEntity.CROAKING) {
			this.croak.apply(actionMillis % 1_500L, 1.0F);
		} else if (action == FrogGodEntity.FIRE_ATTACK) {
			this.openMouth.apply(actionMillis, 1.0F);
		} else if (action == FrogGodEntity.JUMPING) {
			this.jump.apply(actionMillis, 1.0F);
		} else if (action == FrogGodEntity.HOSTILE) {
			this.walk.applyWalk(state.walkAnimationPos, state.walkAnimationSpeed, 1.0F, 2.5F);
		}
	}
}
