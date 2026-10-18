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

/** Five to ten closely spaced copies of the supplied centipedebody cube. */
public final class RegularCentipedeModel extends EntityModel<RegularCentipedeRenderer.RenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(Shamaschizm.MOD_ID, "regular_centipede"), "main");
    private static final int MAX_SECTIONS = 10;
    private final ModelPart[] sections = new ModelPart[MAX_SECTIONS];

    public RegularCentipedeModel(ModelPart root) {
        super(root);
        for (int i = 0; i < MAX_SECTIONS; i++) {
            this.sections[i] = root.getChild("section_" + i);
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        for (int i = 0; i < MAX_SECTIONS; i++) {
            float z = -5.2F + i * 1.15F;
            root.addOrReplaceChild("section_" + i, CubeListBuilder.create().texOffs(0, 0)
                            .addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F,
                                    new CubeDeformation(-0.3F)),
                    PartPose.offset(0.0F, 24.0F, z));
        }
        return LayerDefinition.create(mesh, 16, 16);
    }

    @Override
    public void setupAnim(RegularCentipedeRenderer.RenderState state) {
        super.setupAnim(state);
        float movement = Mth.clamp(state.walkAnimationSpeed * 5.0F, 0.0F, 1.0F);
        for (int i = 0; i < MAX_SECTIONS; i++) {
            ModelPart section = this.sections[i];
            section.visible = i < state.sectionCount;
            if (!section.visible) continue;
            if (state.curled) {
                float angle = i * 0.92F;
                float radius = 0.25F + i * 0.48F;
                section.x = Mth.cos(angle) * radius;
                section.z = Mth.sin(angle) * radius;
                section.yRot = -angle + Mth.HALF_PI;
                continue;
            }
            float phase = state.walkAnimationPos * 4.0F - i * 0.62F;
            section.x += Mth.sin(phase) * 0.22F * movement;
            section.y += Mth.abs(Mth.cos(phase)) * 0.06F * movement;
            section.yRot += Mth.sin(phase) * 0.10F * movement;
        }
    }
}
