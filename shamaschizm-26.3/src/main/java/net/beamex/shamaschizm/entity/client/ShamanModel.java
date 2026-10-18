package net.beamex.shamaschizm.entity.client;

import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.animation.AnimationDefinition;

import java.lang.reflect.Method;

public final class ShamanModel extends EntityModel<ShamanRenderer.ShamanRenderState> {

	public static final ModelLayerLocation LAYER_LOCATION =
			new ModelLayerLocation(Identifier.fromNamespaceAndPath(Shamaschizm.MOD_ID, "shaman"), "main");

	private final ModelPart root;
	private final ModelPart tijelo;
	private final ModelPart noge;
	private final ModelPart head;
	private final ModelPart ruke;

	private final KeyframeAnimation idle1Baked;

	public ShamanModel(ModelPart root) {
		super(root);
		this.root  = root;
		this.tijelo = root.getChild("tijelo");
		this.noge   = root.getChild("noge");
		this.head   = root.getChild("head");
		this.ruke   = root.getChild("ruke");

		this.idle1Baked = ShamanAnimations.idle1.bake(this.root);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition tijelo = partdefinition.addOrReplaceChild("tijelo", CubeListBuilder.create().texOffs(88, 0).addBox(-6.0F, -6.0F, -1.0F, 12.0F, 6.0F, 8.0F, new CubeDeformation(-0.01F))
				.texOffs(120, 14).addBox(-1.0F, -15.0F, 5.5F, 2.0F, 11.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(120, 27).mirror().addBox(-1.0F, -21.0F, 4.5F, 2.0F, 10.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false)
				.texOffs(92, 31).mirror().addBox(-5.0F, -22.0F, -2.0F, 10.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)).mirror(false)
				.texOffs(98, 47).addBox(-4.0F, -14.0F, 0.0F, 8.0F, 8.0F, 7.0F, new CubeDeformation(0.0F))
				.texOffs(120, 62).addBox(-1.0F, -23.0F, 3.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(113, 63).addBox(-1.0F, -24.0F, 1.0F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.2F))
				.texOffs(103, 62).addBox(-1.0F, -25.0F, 0.0F, 2.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
				.texOffs(101, 20).addBox(-2.0F, -25.5F, -4.0F, 4.0F, 5.5F, 5.5F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition noge = partdefinition.addOrReplaceChild("noge", CubeListBuilder.create().texOffs(60, 109).addBox(-10.0F, -5.0F, -10.0F, 20.0F, 5.0F, 14.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(98, 95).mirror().addBox(-4.0F, -5.0F, -5.0F, 8.0F, 7.0F, 7.0F, new CubeDeformation(0.0F)).mirror(false)
				.texOffs(82, 95).addBox(-8.0F, -14.0F, -3.0F, 8.0F, 14.0F, 0.0F, new CubeDeformation(0.0F))
				.texOffs(66, 95).addBox(0.0F, -14.0F, -3.0F, 8.0F, 14.0F, 0.0F, new CubeDeformation(0.0F))
				.texOffs(56, 109).addBox(0.0F, -12.0F, -1.0F, 9.0F, 14.0F, 0.0F, new CubeDeformation(0.0F))
				.texOffs(38, 114).addBox(-9.0F, -12.0F, -1.0F, 9.0F, 14.0F, 0.0F, new CubeDeformation(0.0F))
				.texOffs(120, 116).addBox(-1.0F, 2.0F, -4.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.5F)), PartPose.offset(0.0F, 1.0F, -3.0F));

		PartDefinition ruke = partdefinition.addOrReplaceChild("ruke", CubeListBuilder.create().texOffs(100, 89).addBox(-7.0F, -11.0F, -6.0F, 14.0F, 6.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 23.0F, -1.0F));

		PartDefinition cube_r1 = ruke.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(72, 87).addBox(-6.0F, -1.0F, -1.0F, 7.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(8.0F, -13.0F, -5.0F, 0.0F, 1.5708F, 0.0F));

		PartDefinition cube_r2 = ruke.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(86, 87).addBox(-6.0F, -1.0F, -1.0F, 7.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.0F, -13.0F, -5.0F, 0.0F, 1.5708F, 0.0F));

		PartDefinition cube_r3 = ruke.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(40, 101).addBox(-4.25F, -4.0F, -1.0F, 5.0F, 10.0F, 3.0F, new CubeDeformation(-0.11F)), PartPose.offsetAndRotation(1.0F, -11.0F, -5.0F, 0.0F, 0.7854F, 1.5708F));

		PartDefinition cube_r4 = ruke.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(12, 116).addBox(-3.0F, -4.0F, -1.0F, 3.0F, 9.0F, 3.0F, new CubeDeformation(-0.1F))
				.texOffs(0, 116).addBox(-14.0F, -4.0F, -1.0F, 3.0F, 9.0F, 3.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(7.0F, -14.0F, -2.0F, -0.7854F, 0.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	public static final class ShamanRenderState {
		public boolean idle = true;
	}

	@Override
	public void setupAnim(ShamanRenderer.ShamanRenderState state) {
		// Reset to baked pose every frame
		for (ModelPart p : this.root.getAllParts()) {
			p.resetPose();
		}

		// Apply blockbench idle1
		idle1Baked.apply(state.idle, state.ageInTicks);

		// Add head look on top so it stacks with idle1
		if (this.head != null) {
			float yaw   = Mth.clamp(state.yRot, -30.0F, 30.0F) * (Mth.PI / 180.0F);
			float pitch = Mth.clamp(state.xRot, -25.0F, 45.0F) * (Mth.PI / 180.0F);
			this.head.yRot += yaw;
			this.head.xRot += pitch;
		}
	}


}
