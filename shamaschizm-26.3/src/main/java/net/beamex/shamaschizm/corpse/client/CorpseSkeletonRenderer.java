package net.beamex.shamaschizm.corpse.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.beamex.shamaschizm.corpse.CorpseSkeletonEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.skeleton.SkeletonModel;
import net.minecraft.client.renderer.entity.AbstractSkeletonRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;
import net.minecraft.resources.Identifier;

/** Normal vanilla skeleton appearance; only the stored corpse is rendered lying down. */
public final class CorpseSkeletonRenderer extends AbstractSkeletonRenderer<CorpseSkeletonEntity,
        CorpseSkeletonRenderer.State> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            "minecraft", "textures/entity/skeleton/skeleton.png");

    public CorpseSkeletonRenderer(EntityRendererProvider.Context context) {
        super(context, ModelLayers.SKELETON_ARMOR,
                new SkeletonModel<>(context.bakeLayer(ModelLayers.SKELETON)));
    }

    @Override public Identifier getTextureLocation(State state) { return TEXTURE; }
    @Override public State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(CorpseSkeletonEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.lying = entity.isLying();
    }

    @Override
    protected void setupRotations(State state, PoseStack pose, float bodyRot, float entityScale) {
        if (!state.lying) {
            super.setupRotations(state, pose, bodyRot, entityScale);
            return;
        }
        pose.rotate(Axis.YP.rotationDegrees(bodyRot));
        pose.translate(0.0F, 0.1F, 0.0F);
        pose.rotate(Axis.ZP.rotationDegrees(90.0F));
        pose.rotate(Axis.YP.rotationDegrees(270.0F));
    }

    public static final class State extends SkeletonRenderState {
        public boolean lying;
    }
}
