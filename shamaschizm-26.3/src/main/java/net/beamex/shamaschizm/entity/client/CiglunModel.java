package net.beamex.shamaschizm.entity.client;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.Direction;
import org.joml.Vector3f;

/** Exact GLTF geometry converted from the supplied Blockbench model. */
public final class CiglunModel extends EntityModel<CiglunRenderer.CiglunRenderState> {
    private final KeyframeAnimation idle;
    private final KeyframeAnimation turnsAround;
    private final KeyframeAnimation open;

    public CiglunModel() {
        super(createRoot(), RenderTypes::entityCutout);
        this.idle = CiglunAnimations.IDLE.bake(this.root());
        this.turnsAround = CiglunAnimations.TURNSAROUND2.bake(this.root());
        this.open = CiglunAnimations.OPEN.bake(this.root());
    }

    @Override
    public void setupAnim(CiglunRenderer.CiglunRenderState state) {
        super.setupAnim(state);
        if (state.action == 1) this.turnsAround.apply(state.turningAnimation, state.ageInTicks);
        else if (state.action == 2) this.open.apply(state.openAnimation, state.ageInTicks);
        else this.idle.apply((long)(state.ageInTicks * 50.0F), 1.0F);
    }

    private static ModelPart createRoot() {
        ModelPart brick1 = part(List.of(
                cube(new float[]{8F,4F,-4F,8F,4F,-8F,8F,8F,-4F,8F,8F,-8F,-8F,4F,-8F,-8F,4F,-4F,-8F,8F,-8F,-8F,8F,-4F,-8F,4F,-8F,8F,4F,-8F,-8F,4F,-4F,8F,4F,-4F,-8F,8F,-4F,8F,8F,-4F,-8F,8F,-8F,8F,8F,-8F,-8F,4F,-4F,8F,4F,-4F,-8F,8F,-4F,8F,8F,-4F,8F,4F,-8F,-8F,4F,-8F,8F,8F,-8F,-8F,8F,-8F},
                        new float[]{0.4375F,0.34375F,0.46875F,0.34375F,0.4375F,0.375F,0.46875F,0.375F,0.4375F,0.375F,0.46875F,0.375F,0.4375F,0.40625F,0.46875F,0.40625F,0.125F,0.09375F,0F,0.09375F,0.125F,0.0625F,0F,0.0625F,0.125F,0.09375F,0F,0.09375F,0.125F,0.125F,0F,0.125F,0F,0.03125F,0.125F,0.03125F,0F,0.0625F,0.125F,0.0625F,0F,0F,0.125F,0F,0F,0.03125F,0.125F,0.03125F},
                        new float[]{1F,0F,0F,-1F,0F,0F,0F,-1F,0F,0F,1F,0F,0F,0F,1F,0F,0F,-1F}),
                cube(new float[]{0F,0F,-4F,0F,0F,-8F,0F,4F,-4F,0F,4F,-8F,-8F,0F,-8F,-8F,0F,-4F,-8F,4F,-8F,-8F,4F,-4F,-8F,0F,-8F,0F,0F,-8F,-8F,0F,-4F,0F,0F,-4F,-8F,4F,-4F,0F,4F,-4F,-8F,4F,-8F,0F,4F,-8F,-8F,0F,-4F,0F,0F,-4F,-8F,4F,-4F,0F,4F,-4F,0F,0F,-8F,-8F,0F,-8F,0F,4F,-8F,-8F,4F,-8F},
                        new float[]{0.4375F,0.40625F,0.46875F,0.40625F,0.4375F,0.4375F,0.46875F,0.4375F,0.4375F,0.4375F,0.46875F,0.4375F,0.4375F,0.46875F,0.46875F,0.46875F,0.109375F,0.40625F,0.046875F,0.40625F,0.109375F,0.375F,0.046875F,0.375F,0.4375F,0.09375F,0.375F,0.09375F,0.4375F,0.125F,0.375F,0.125F,0.375F,0.03125F,0.4375F,0.03125F,0.375F,0.0625F,0.4375F,0.0625F,0.375F,0F,0.4375F,0F,0.375F,0.03125F,0.4375F,0.03125F},
                        new float[]{1F,0F,0F,-1F,0F,0F,0F,-1F,0F,0F,1F,0F,0F,0F,1F,0F,0F,-1F}),
                cube(new float[]{0F,4F,4F,0F,4F,-4F,0F,8F,4F,0F,8F,-4F,-8F,4F,-4F,-8F,4F,4F,-8F,8F,-4F,-8F,8F,4F,-8F,4F,-4F,0F,4F,-4F,-8F,4F,4F,0F,4F,4F,-8F,8F,4F,0F,8F,4F,-8F,8F,-4F,0F,8F,-4F,-8F,4F,4F,0F,4F,4F,-8F,8F,4F,0F,8F,4F,0F,4F,-4F,-8F,4F,-4F,0F,8F,-4F,-8F,8F,-4F},
                        new float[]{0.375F,0.15625F,0.4375F,0.15625F,0.375F,0.1875F,0.4375F,0.1875F,0.390625F,0.21875F,0.453125F,0.21875F,0.390625F,0.25F,0.453125F,0.25F,0.0625F,0.1875F,0F,0.1875F,0.0625F,0.125F,0F,0.125F,0.1875F,0F,0.125F,0F,0.1875F,0.0625F,0.125F,0.0625F,0.21875F,0.390625F,0.28125F,0.390625F,0.21875F,0.421875F,0.28125F,0.421875F,0.375F,0.125F,0.4375F,0.125F,0.375F,0.15625F,0.4375F,0.15625F},
                        new float[]{1F,0F,0F,-1F,0F,0F,0F,-1F,0F,0F,1F,0F,0F,0F,1F,0F,0F,-1F})
        ), Map.of(), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        ModelPart brick3 = part(List.of(
                cube(new float[]{8F,4F,-4F,8F,4F,-8F,8F,8F,-4F,8F,8F,-8F,-8F,4F,-8F,-8F,4F,-4F,-8F,8F,-8F,-8F,8F,-4F,-8F,4F,-8F,8F,4F,-8F,-8F,4F,-4F,8F,4F,-4F,-8F,8F,-4F,8F,8F,-4F,-8F,8F,-8F,8F,8F,-8F,-8F,4F,-4F,8F,4F,-4F,-8F,8F,-4F,8F,8F,-4F,8F,4F,-8F,-8F,4F,-8F,8F,8F,-8F,-8F,8F,-8F},
                        new float[]{0.453125F,0.21875F,0.484375F,0.21875F,0.453125F,0.25F,0.484375F,0.25F,0.453125F,0.25F,0.484375F,0.25F,0.453125F,0.28125F,0.484375F,0.28125F,0.25F,0.125F,0.125F,0.125F,0.25F,0.09375F,0.125F,0.09375F,0.1875F,0.15625F,0.0625F,0.15625F,0.1875F,0.1875F,0.0625F,0.1875F,0.125F,0.0625F,0.25F,0.0625F,0.125F,0.09375F,0.25F,0.09375F,0.0625F,0.125F,0.1875F,0.125F,0.0625F,0.15625F,0.1875F,0.15625F},
                        new float[]{1F,0F,0F,-1F,0F,0F,0F,-1F,0F,0F,1F,0F,0F,0F,1F,0F,0F,-1F}),
                cube(new float[]{0F,0F,-4F,0F,0F,-8F,0F,4F,-4F,0F,4F,-8F,-8F,0F,-8F,-8F,0F,-4F,-8F,4F,-8F,-8F,4F,-4F,-8F,0F,-8F,0F,0F,-8F,-8F,0F,-4F,0F,0F,-4F,-8F,4F,-4F,0F,4F,-4F,-8F,4F,-8F,0F,4F,-8F,-8F,0F,-4F,0F,0F,-4F,-8F,4F,-4F,0F,4F,-4F,0F,0F,-8F,-8F,0F,-8F,0F,4F,-8F,-8F,4F,-8F},
                        new float[]{0.453125F,0.28125F,0.484375F,0.28125F,0.453125F,0.3125F,0.484375F,0.3125F,0.46875F,0F,0.5F,0F,0.46875F,0.03125F,0.5F,0.03125F,0.375F,0.421875F,0.3125F,0.421875F,0.375F,0.390625F,0.3125F,0.390625F,0.4375F,0.390625F,0.375F,0.390625F,0.4375F,0.421875F,0.375F,0.421875F,0.390625F,0.28125F,0.453125F,0.28125F,0.390625F,0.3125F,0.453125F,0.3125F,0.390625F,0.25F,0.453125F,0.25F,0.390625F,0.28125F,0.453125F,0.28125F},
                        new float[]{1F,0F,0F,-1F,0F,0F,0F,-1F,0F,0F,1F,0F,0F,0F,1F,0F,0F,-1F}),
                cube(new float[]{0F,4F,4F,0F,4F,-4F,0F,8F,4F,0F,8F,-4F,-8F,4F,-4F,-8F,4F,4F,-8F,8F,-4F,-8F,8F,4F,-8F,4F,-4F,0F,4F,-4F,-8F,4F,4F,0F,4F,4F,-8F,8F,4F,0F,8F,4F,-8F,8F,-4F,0F,8F,-4F,-8F,4F,4F,0F,4F,4F,-8F,8F,4F,0F,8F,4F,0F,4F,-4F,-8F,4F,-4F,0F,8F,-4F,-8F,8F,-4F},
                        new float[]{0.40625F,0.0625F,0.46875F,0.0625F,0.40625F,0.09375F,0.46875F,0.09375F,0.40625F,0.1875F,0.46875F,0.1875F,0.40625F,0.21875F,0.46875F,0.21875F,0.0625F,0.25F,0F,0.25F,0.0625F,0.1875F,0F,0.1875F,0.25F,0F,0.1875F,0F,0.25F,0.0625F,0.1875F,0.0625F,0.109375F,0.40625F,0.171875F,0.40625F,0.109375F,0.4375F,0.171875F,0.4375F,0.046875F,0.40625F,0.109375F,0.40625F,0.046875F,0.4375F,0.109375F,0.4375F},
                        new float[]{1F,0F,0F,-1F,0F,0F,0F,-1F,0F,0F,1F,0F,0F,0F,1F,0F,0F,-1F})
        ), Map.of(), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 3.141593F, 0F));
        ModelPart brick2 = part(List.of(
                cube(new float[]{-4F,-8F,-4F,-4F,-8F,-8F,-4F,8F,-4F,-4F,8F,-8F,-8F,-8F,-8F,-8F,-8F,-4F,-8F,8F,-8F,-8F,8F,-4F,-8F,-8F,-8F,-4F,-8F,-8F,-8F,-8F,-4F,-4F,-8F,-4F,-8F,8F,-4F,-4F,8F,-4F,-8F,8F,-8F,-4F,8F,-8F,-8F,-8F,-4F,-4F,-8F,-4F,-8F,8F,-4F,-4F,8F,-4F,-4F,-8F,-8F,-8F,-8F,-8F,-4F,8F,-8F,-8F,8F,-8F},
                        new float[]{0.09375F,0.1875F,0.125F,0.1875F,0.09375F,0.3125F,0.125F,0.3125F,0.1875F,0.125F,0.21875F,0.125F,0.1875F,0.25F,0.21875F,0.25F,0.5F,0.0625F,0.46875F,0.0625F,0.5F,0.03125F,0.46875F,0.03125F,0.5F,0.0625F,0.46875F,0.0625F,0.5F,0.09375F,0.46875F,0.09375F,0.125F,0.1875F,0.15625F,0.1875F,0.125F,0.3125F,0.15625F,0.3125F,0.0625F,0.1875F,0.09375F,0.1875F,0.0625F,0.3125F,0.09375F,0.3125F},
                        new float[]{1F,0F,0F,-1F,0F,0F,0F,-1F,0F,0F,1F,0F,0F,0F,1F,0F,0F,-1F}),
                cube(new float[]{0F,0F,-4F,0F,0F,-8F,0F,8F,-4F,0F,8F,-8F,-4F,0F,-8F,-4F,0F,-4F,-4F,8F,-8F,-4F,8F,-4F,-4F,0F,-8F,0F,0F,-8F,-4F,0F,-4F,0F,0F,-4F,-4F,8F,-4F,0F,8F,-4F,-4F,8F,-8F,0F,8F,-8F,-4F,0F,-4F,0F,0F,-4F,-4F,8F,-4F,0F,8F,-4F,0F,0F,-8F,-4F,0F,-8F,0F,8F,-8F,-4F,8F,-8F},
                        new float[]{0.40625F,0.3125F,0.4375F,0.3125F,0.40625F,0.375F,0.4375F,0.375F,0.21875F,0.421875F,0.25F,0.421875F,0.21875F,0.484375F,0.25F,0.484375F,0.5F,0.125F,0.46875F,0.125F,0.5F,0.09375F,0.46875F,0.09375F,0.5F,0.125F,0.46875F,0.125F,0.5F,0.15625F,0.46875F,0.15625F,0F,0.421875F,0.03125F,0.421875F,0F,0.484375F,0.03125F,0.484375F,0.28125F,0.40625F,0.3125F,0.40625F,0.28125F,0.46875F,0.3125F,0.46875F},
                        new float[]{1F,0F,0F,-1F,0F,0F,0F,-1F,0F,0F,1F,0F,0F,0F,1F,0F,0F,-1F}),
                cube(new float[]{-4F,0F,4F,-4F,0F,-4F,-4F,8F,4F,-4F,8F,-4F,-8F,0F,-4F,-8F,0F,4F,-8F,8F,-4F,-8F,8F,4F,-8F,0F,-4F,-4F,0F,-4F,-8F,0F,4F,-4F,0F,4F,-8F,8F,4F,-4F,8F,4F,-8F,8F,-4F,-4F,8F,-4F,-8F,0F,4F,-4F,0F,4F,-8F,8F,4F,-4F,8F,4F,-4F,0F,-4F,-8F,0F,-4F,-4F,8F,-4F,-8F,8F,-4F},
                        new float[]{0.21875F,0.125F,0.28125F,0.125F,0.21875F,0.1875F,0.28125F,0.1875F,0.21875F,0.1875F,0.28125F,0.1875F,0.21875F,0.25F,0.28125F,0.25F,0.375F,0.484375F,0.34375F,0.484375F,0.375F,0.421875F,0.34375F,0.421875F,0.40625F,0.421875F,0.375F,0.421875F,0.40625F,0.484375F,0.375F,0.484375F,0.3125F,0.421875F,0.34375F,0.421875F,0.3125F,0.484375F,0.34375F,0.484375F,0.25F,0.421875F,0.28125F,0.421875F,0.25F,0.484375F,0.28125F,0.484375F},
                        new float[]{1F,0F,0F,-1F,0F,0F,0F,-1F,0F,0F,1F,0F,0F,0F,1F,0F,0F,-1F})
        ), Map.of(), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 3.141593F, -1.570796F));
        ModelPart brick6 = part(List.of(
                cube(new float[]{-4F,-8F,-4F,-4F,-8F,-8F,-4F,8F,-4F,-4F,8F,-8F,-8F,-8F,-8F,-8F,-8F,-4F,-8F,8F,-8F,-8F,8F,-4F,-8F,-8F,-8F,-4F,-8F,-8F,-8F,-8F,-4F,-4F,-8F,-4F,-8F,8F,-4F,-4F,8F,-4F,-8F,8F,-8F,-4F,8F,-8F,-8F,-8F,-4F,-4F,-8F,-4F,-8F,8F,-4F,-4F,8F,-4F,-4F,-8F,-8F,-8F,-8F,-8F,-4F,8F,-8F,-8F,8F,-8F},
                        new float[]{0F,0.25F,0.03125F,0.25F,0F,0.375F,0.03125F,0.375F,0.03125F,0.25F,0.0625F,0.25F,0.03125F,0.375F,0.0625F,0.375F,0.5F,0.1875F,0.46875F,0.1875F,0.5F,0.15625F,0.46875F,0.15625F,0.21875F,0.46875F,0.1875F,0.46875F,0.21875F,0.5F,0.1875F,0.5F,0.25F,0F,0.28125F,0F,0.25F,0.125F,0.28125F,0.125F,0.15625F,0.1875F,0.1875F,0.1875F,0.15625F,0.3125F,0.1875F,0.3125F},
                        new float[]{1F,0F,0F,-1F,0F,0F,0F,-1F,0F,0F,1F,0F,0F,0F,1F,0F,0F,-1F}),
                cube(new float[]{0F,0F,-4F,0F,0F,-8F,0F,8F,-4F,0F,8F,-8F,-4F,0F,-8F,-4F,0F,-4F,-4F,8F,-8F,-4F,8F,-4F,-4F,0F,-8F,0F,0F,-8F,-4F,0F,-4F,0F,0F,-4F,-4F,8F,-4F,0F,8F,-4F,-4F,8F,-8F,0F,8F,-8F,-4F,0F,-4F,0F,0F,-4F,-4F,8F,-4F,0F,8F,-4F,0F,0F,-8F,-4F,0F,-8F,0F,8F,-8F,-4F,8F,-8F},
                        new float[]{0.4375F,0F,0.46875F,0F,0.4375F,0.0625F,0.46875F,0.0625F,0.0625F,0.4375F,0.09375F,0.4375F,0.0625F,0.5F,0.09375F,0.5F,0.5F,0.21875F,0.46875F,0.21875F,0.5F,0.1875F,0.46875F,0.1875F,0.3125F,0.46875F,0.28125F,0.46875F,0.3125F,0.5F,0.28125F,0.5F,0.03125F,0.4375F,0.0625F,0.4375F,0.03125F,0.5F,0.0625F,0.5F,0.40625F,0.421875F,0.4375F,0.421875F,0.40625F,0.484375F,0.4375F,0.484375F},
                        new float[]{1F,0F,0F,-1F,0F,0F,0F,-1F,0F,0F,1F,0F,0F,0F,1F,0F,0F,-1F}),
                cube(new float[]{-4F,0F,4F,-4F,0F,-4F,-4F,8F,4F,-4F,8F,-4F,-8F,0F,-4F,-8F,0F,4F,-8F,8F,-4F,-8F,8F,4F,-8F,0F,-4F,-4F,0F,-4F,-8F,0F,4F,-4F,0F,4F,-8F,8F,4F,-4F,8F,4F,-8F,8F,-4F,-4F,8F,-4F,-8F,0F,4F,-4F,0F,4F,-8F,8F,4F,-4F,8F,4F,-4F,0F,-4F,-8F,0F,-4F,-4F,8F,-4F,-8F,8F,-4F},
                        new float[]{0.1875F,0.25F,0.25F,0.25F,0.1875F,0.3125F,0.25F,0.3125F,0.25F,0.25F,0.3125F,0.25F,0.25F,0.3125F,0.3125F,0.3125F,0.15625F,0.5F,0.125F,0.5F,0.15625F,0.4375F,0.125F,0.4375F,0.1875F,0.4375F,0.15625F,0.4375F,0.1875F,0.5F,0.15625F,0.5F,0.4375F,0.09375F,0.46875F,0.09375F,0.4375F,0.15625F,0.46875F,0.15625F,0.09375F,0.4375F,0.125F,0.4375F,0.09375F,0.5F,0.125F,0.5F},
                        new float[]{1F,0F,0F,-1F,0F,0F,0F,-1F,0F,0F,1F,0F,0F,0F,1F,0F,0F,-1F})
        ), Map.of(), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 1.570796F));
        ModelPart brick4 = part(List.of(
                cube(new float[]{8F,0F,4F,8F,0F,-8F,8F,4F,4F,8F,4F,-8F,4F,0F,-8F,4F,0F,4F,4F,4F,-8F,4F,4F,4F,4F,0F,-8F,8F,0F,-8F,4F,0F,4F,8F,0F,4F,4F,4F,4F,8F,4F,4F,4F,4F,-8F,8F,4F,-8F,4F,0F,4F,8F,0F,4F,4F,4F,4F,8F,4F,4F,8F,0F,-8F,4F,0F,-8F,8F,4F,-8F,4F,4F,-8F},
                        new float[]{0.0625F,0.3125F,0.15625F,0.3125F,0.0625F,0.34375F,0.15625F,0.34375F,0.3125F,0.0625F,0.40625F,0.0625F,0.3125F,0.09375F,0.40625F,0.09375F,0.34375F,0.1875F,0.3125F,0.1875F,0.34375F,0.09375F,0.3125F,0.09375F,0.1875F,0.3125F,0.15625F,0.3125F,0.1875F,0.40625F,0.15625F,0.40625F,0.46875F,0.34375F,0.5F,0.34375F,0.46875F,0.375F,0.5F,0.375F,0.46875F,0.3125F,0.5F,0.3125F,0.46875F,0.34375F,0.5F,0.34375F},
                        new float[]{1F,0F,0F,-1F,0F,0F,0F,-1F,0F,0F,1F,0F,0F,0F,1F,0F,0F,-1F}),
                cube(new float[]{4F,0F,-4F,4F,0F,-8F,4F,4F,-4F,4F,4F,-8F,0F,0F,-8F,0F,0F,-4F,0F,4F,-8F,0F,4F,-4F,0F,0F,-8F,4F,0F,-8F,0F,0F,-4F,4F,0F,-4F,0F,4F,-4F,4F,4F,-4F,0F,4F,-8F,4F,4F,-8F,0F,0F,-4F,4F,0F,-4F,0F,4F,-4F,4F,4F,-4F,4F,0F,-8F,0F,0F,-8F,4F,4F,-8F,0F,4F,-8F},
                        new float[]{0.46875F,0.40625F,0.5F,0.40625F,0.46875F,0.4375F,0.5F,0.4375F,0.46875F,0.4375F,0.5F,0.4375F,0.46875F,0.46875F,0.5F,0.46875F,0.5F,0.5F,0.46875F,0.5F,0.5F,0.46875F,0.46875F,0.46875F,0.03125F,0.484375F,0F,0.484375F,0.03125F,0.515625F,0F,0.515625F,0.4375F,0.46875F,0.46875F,0.46875F,0.4375F,0.5F,0.46875F,0.5F,0.46875F,0.375F,0.5F,0.375F,0.46875F,0.40625F,0.5F,0.40625F},
                        new float[]{1F,0F,0F,-1F,0F,0F,0F,-1F,0F,0F,1F,0F,0F,0F,1F,0F,0F,-1F})
        ), Map.of(), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        ModelPart brick5 = part(List.of(
                cube(new float[]{8F,0F,4F,8F,0F,-8F,8F,4F,4F,8F,4F,-8F,4F,0F,-8F,4F,0F,4F,4F,4F,-8F,4F,4F,4F,4F,0F,-8F,8F,0F,-8F,4F,0F,4F,8F,0F,4F,4F,4F,4F,8F,4F,4F,4F,4F,-8F,8F,4F,-8F,4F,0F,4F,8F,0F,4F,4F,4F,4F,8F,4F,4F,8F,0F,-8F,4F,0F,-8F,8F,4F,-8F,4F,4F,-8F},
                        new float[]{0.1875F,0.3125F,0.28125F,0.3125F,0.1875F,0.34375F,0.28125F,0.34375F,0.3125F,0.1875F,0.40625F,0.1875F,0.3125F,0.21875F,0.40625F,0.21875F,0.34375F,0.3125F,0.3125F,0.3125F,0.34375F,0.21875F,0.3125F,0.21875F,0.3125F,0.3125F,0.28125F,0.3125F,0.3125F,0.40625F,0.28125F,0.40625F,0.484375F,0.21875F,0.515625F,0.21875F,0.484375F,0.25F,0.515625F,0.25F,0.21875F,0.484375F,0.25F,0.484375F,0.21875F,0.515625F,0.25F,0.515625F},
                        new float[]{1F,0F,0F,-1F,0F,0F,0F,-1F,0F,0F,1F,0F,0F,0F,1F,0F,0F,-1F}),
                cube(new float[]{4F,0F,-4F,4F,0F,-8F,4F,4F,-4F,4F,4F,-8F,0F,0F,-8F,0F,0F,-4F,0F,4F,-8F,0F,4F,-4F,0F,0F,-8F,4F,0F,-8F,0F,0F,-4F,4F,0F,-4F,0F,4F,-4F,4F,4F,-4F,0F,4F,-8F,4F,4F,-8F,0F,0F,-4F,4F,0F,-4F,0F,4F,-4F,4F,4F,-4F,4F,0F,-8F,0F,0F,-8F,4F,4F,-8F,0F,4F,-8F},
                        new float[]{0.484375F,0.25F,0.515625F,0.25F,0.484375F,0.28125F,0.515625F,0.28125F,0.3125F,0.484375F,0.34375F,0.484375F,0.3125F,0.515625F,0.34375F,0.515625F,0.375F,0.515625F,0.34375F,0.515625F,0.375F,0.484375F,0.34375F,0.484375F,0.40625F,0.484375F,0.375F,0.484375F,0.40625F,0.515625F,0.375F,0.515625F,0.484375F,0.28125F,0.515625F,0.28125F,0.484375F,0.3125F,0.515625F,0.3125F,0.25F,0.484375F,0.28125F,0.484375F,0.25F,0.515625F,0.28125F,0.515625F},
                        new float[]{1F,0F,0F,-1F,0F,0F,0F,-1F,0F,0F,1F,0F,0F,0F,1F,0F,0F,-1F})
        ), Map.of(), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 3.141593F, 0F));
        ModelPart hitboks = part(List.of(
        ), Map.of(), PartPose.offsetAndRotation(0F, 8F, 0F, 0F, 0F, 0F));
        ModelPart oko = part(List.of(
                cube(new float[]{3F,-3F,3F,3F,-3F,-3F,3F,3F,3F,3F,3F,-3F,-3F,-3F,-3F,-3F,-3F,3F,-3F,3F,-3F,-3F,3F,3F,-3F,-3F,-3F,3F,-3F,-3F,-3F,-3F,3F,3F,-3F,3F,-3F,3F,3F,3F,3F,3F,-3F,3F,-3F,3F,3F,-3F,-3F,-3F,3F,3F,-3F,3F,-3F,3F,3F,3F,3F,3F,3F,-3F,-3F,-3F,-3F,-3F,3F,3F,-3F,-3F,3F,-3F},
                        new float[]{0.34375F,0.21875F,0.390625F,0.21875F,0.34375F,0.265625F,0.390625F,0.265625F,0.3125F,0.34375F,0.359375F,0.34375F,0.3125F,0.390625F,0.359375F,0.390625F,0.40625F,0.390625F,0.359375F,0.390625F,0.40625F,0.34375F,0.359375F,0.34375F,0.046875F,0.375F,0F,0.375F,0.046875F,0.421875F,0F,0.421875F,0.34375F,0.265625F,0.390625F,0.265625F,0.34375F,0.3125F,0.390625F,0.3125F,0.21875F,0.34375F,0.265625F,0.34375F,0.21875F,0.390625F,0.265625F,0.390625F},
                        new float[]{1F,0F,0F,-1F,0F,0F,0F,-1F,0F,0F,1F,0F,0F,0F,1F,0F,0F,-1F})
        ), Map.of(), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        ModelPart brick7 = part(List.of(
                cube(new float[]{8F,-4F,4F,8F,-4F,-8F,8F,0F,4F,8F,0F,-8F,4F,-4F,-8F,4F,-4F,4F,4F,0F,-8F,4F,0F,4F,4F,-4F,-8F,8F,-4F,-8F,4F,-4F,4F,8F,-4F,4F,4F,0F,4F,8F,0F,4F,4F,0F,-8F,8F,0F,-8F,4F,-4F,4F,8F,-4F,4F,4F,0F,4F,8F,0F,4F,8F,-4F,-8F,4F,-4F,-8F,8F,0F,-8F,4F,0F,-8F},
                        new float[]{0.3125F,0.3125F,0.40625F,0.3125F,0.3125F,0.34375F,0.40625F,0.34375F,0.0625F,0.34375F,0.15625F,0.34375F,0.0625F,0.375F,0.15625F,0.375F,0.375F,0.1875F,0.34375F,0.1875F,0.375F,0.09375F,0.34375F,0.09375F,0.21875F,0.34375F,0.1875F,0.34375F,0.21875F,0.4375F,0.1875F,0.4375F,0.5F,0F,0.53125F,0F,0.5F,0.03125F,0.53125F,0.03125F,0.40625F,0.484375F,0.4375F,0.484375F,0.40625F,0.515625F,0.4375F,0.515625F},
                        new float[]{1F,0F,0F,-1F,0F,0F,0F,-1F,0F,0F,1F,0F,0F,0F,1F,0F,0F,-1F}),
                cube(new float[]{4F,-4F,-6F,4F,-4F,-8F,4F,0F,-6F,4F,0F,-8F,0F,-4F,-8F,0F,-4F,-6F,0F,0F,-8F,0F,0F,-6F,0F,-4F,-8F,4F,-4F,-8F,0F,-4F,-6F,4F,-4F,-6F,0F,0F,-6F,4F,0F,-6F,0F,0F,-8F,4F,0F,-8F,0F,-4F,-6F,4F,-4F,-6F,0F,0F,-6F,4F,0F,-6F,4F,-4F,-8F,0F,-4F,-8F,4F,0F,-8F,0F,0F,-8F},
                        new float[]{0.171875F,0.40625F,0.1875F,0.40625F,0.171875F,0.4375F,0.1875F,0.4375F,0.5F,0.0625F,0.515625F,0.0625F,0.5F,0.09375F,0.515625F,0.09375F,0.125F,0.515625F,0.09375F,0.515625F,0.125F,0.5F,0.09375F,0.5F,0.53125F,0.09375F,0.5F,0.09375F,0.53125F,0.109375F,0.5F,0.109375F,0.5F,0.03125F,0.53125F,0.03125F,0.5F,0.0625F,0.53125F,0.0625F,0.03125F,0.5F,0.0625F,0.5F,0.03125F,0.53125F,0.0625F,0.53125F},
                        new float[]{1F,0F,0F,-1F,0F,0F,0F,-1F,0F,0F,1F,0F,0F,0F,1F,0F,0F,-1F})
        ), Map.of(), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        ModelPart brick8 = part(List.of(
                cube(new float[]{8F,-4F,4F,8F,-4F,-8F,8F,0F,4F,8F,0F,-8F,4F,-4F,-8F,4F,-4F,4F,4F,0F,-8F,4F,0F,4F,4F,-4F,-8F,8F,-4F,-8F,4F,-4F,4F,8F,-4F,4F,4F,0F,4F,8F,0F,4F,4F,0F,-8F,8F,0F,-8F,4F,-4F,4F,8F,-4F,4F,4F,0F,4F,8F,0F,4F,8F,-4F,-8F,4F,-4F,-8F,8F,0F,-8F,4F,0F,-8F},
                        new float[]{0.28125F,0F,0.375F,0F,0.28125F,0.03125F,0.375F,0.03125F,0.28125F,0.03125F,0.375F,0.03125F,0.28125F,0.0625F,0.375F,0.0625F,0.3125F,0.15625F,0.28125F,0.15625F,0.3125F,0.0625F,0.28125F,0.0625F,0.3125F,0.15625F,0.28125F,0.15625F,0.3125F,0.25F,0.28125F,0.25F,0.4375F,0.15625F,0.46875F,0.15625F,0.4375F,0.1875F,0.46875F,0.1875F,0.109375F,0.375F,0.140625F,0.375F,0.109375F,0.40625F,0.140625F,0.40625F},
                        new float[]{1F,0F,0F,-1F,0F,0F,0F,-1F,0F,0F,1F,0F,0F,0F,1F,0F,0F,-1F}),
                cube(new float[]{4F,-4F,-6F,4F,-4F,-8F,4F,0F,-6F,4F,0F,-8F,0F,-4F,-8F,0F,-4F,-6F,0F,0F,-8F,0F,0F,-6F,0F,-4F,-8F,4F,-4F,-8F,0F,-4F,-6F,4F,-4F,-6F,0F,0F,-6F,4F,0F,-6F,0F,0F,-8F,4F,0F,-8F,0F,-4F,-6F,4F,-4F,-6F,0F,0F,-6F,4F,0F,-6F,4F,-4F,-8F,0F,-4F,-8F,4F,0F,-8F,0F,0F,-8F},
                        new float[]{0.265625F,0.34375F,0.28125F,0.34375F,0.265625F,0.375F,0.28125F,0.375F,0.140625F,0.375F,0.15625F,0.375F,0.140625F,0.40625F,0.15625F,0.40625F,0.4375F,0.390625F,0.40625F,0.390625F,0.4375F,0.375F,0.40625F,0.375F,0.09375F,0.5F,0.0625F,0.5F,0.09375F,0.515625F,0.0625F,0.515625F,0.4375F,0.3125F,0.46875F,0.3125F,0.4375F,0.34375F,0.46875F,0.34375F,0.1875F,0.4375F,0.21875F,0.4375F,0.1875F,0.46875F,0.21875F,0.46875F},
                        new float[]{1F,0F,0F,-1F,0F,0F,0F,-1F,0F,0F,1F,0F,0F,0F,1F,0F,0F,-1F})
        ), Map.of(), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 3.141593F, 0F));
        Map<String, ModelPart> glavaChildren = new LinkedHashMap<>();
        glavaChildren.put("brick1", brick1);
        glavaChildren.put("brick3", brick3);
        glavaChildren.put("brick2", brick2);
        glavaChildren.put("brick6", brick6);
        glavaChildren.put("brick4", brick4);
        glavaChildren.put("brick5", brick5);
        glavaChildren.put("hitboks", hitboks);
        glavaChildren.put("oko", oko);
        glavaChildren.put("brick7", brick7);
        glavaChildren.put("brick8", brick8);
        ModelPart glava = part(List.of(), glavaChildren, PartPose.offsetAndRotation(0F, -24F, 0F, 0F, 0F, 0F));
        ModelPart root = part(List.of(), Map.of("glava", glava), PartPose.offset(0.0F, 24.0F, 0.0F));
        return root;
    }

    private static ModelPart part(List<ModelPart.Cube> cubes, Map<String, ModelPart> children, PartPose pose) {
        ModelPart part = new ModelPart(new ArrayList<>(cubes), new LinkedHashMap<>(children));
        part.setInitialPose(pose);
        part.loadPose(pose);
        return part;
    }

    private static ModelPart.Cube cube(float[] positions, float[] uvs, float[] normals) {
        return new ExactCube(positions, uvs, normals);
    }

    private static final class ExactCube extends ModelPart.Cube {
        ExactCube(float[] positions, float[] uvs, float[] normals) {
            super(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false,
                    128, 128, EnumSet.allOf(Direction.class));
            int[] order = {0, 2, 3, 1};
            for (int face = 0; face < 6; face++) {
                ModelPart.Vertex[] vertices = new ModelPart.Vertex[4];
                for (int corner = 0; corner < 4; corner++) {
                    int source = face * 4 + order[corner];
                    vertices[corner] = new ModelPart.Vertex(
                            positions[source * 3], positions[source * 3 + 1], positions[source * 3 + 2],
                            uvs[source * 2], uvs[source * 2 + 1]);
                }
                this.polygons[face] = new ModelPart.Polygon(vertices,
                        new Vector3f(normals[face * 3], normals[face * 3 + 1], normals[face * 3 + 2]));
            }
        }
    }
}
