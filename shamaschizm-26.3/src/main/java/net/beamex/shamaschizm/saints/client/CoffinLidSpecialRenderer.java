package net.beamex.shamaschizm.saints.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import org.joml.Vector3fc;

/** The revised coffin lid, rendered in the same coordinate space as a vanilla shield. */
public final class CoffinLidSpecialRenderer implements NoDataSpecialModelRenderer {
    private static final Identifier TEXTURE=Shamaschizm.id("textures/entity/coffin2.png");
    private final Model<Unit> model;

    private CoffinLidSpecialRenderer(ModelPart lid) {
        // The entity root is 24 pixels above its model origin; a shield has no such offset.
        lid.setInitialPose(PartPose.ZERO);
        lid.loadPose(PartPose.ZERO);
        model=new Model<Unit>(lid,RenderTypes::entityCutout) {};
    }
    private static void align(PoseStack poses) {
        // Convert the horizontal 16x32 lid to an 11x22 vertical plate centered
        // at the vanilla shield grip. Coordinates here are blocks, not pixels.
        poses.translate(0,5.5D/16.0D,5.1875D/16.0D);
        poses.scale(.6875F,.6875F,.6875F);
        poses.rotate(Axis.XP.rotationDegrees(90));
    }
    @Override public void submit(PoseStack poses,SubmitNodeCollector collector,
                                 int light,int overlay,boolean foil,int outline) {
        poses.pushPose();align(poses);
        collector.submitModel(model,Unit.INSTANCE,poses,(foil ? RenderTypes.itemCutoutGlint(TEXTURE) : RenderTypes.itemCutout(TEXTURE)),
                light,overlay,-1,null,outline);
        poses.popPose();
    }
    @Override public void getExtents(Consumer<Vector3fc> output) {
        PoseStack poses=new PoseStack();align(poses);model.root().getExtentsForGui(poses,output);
    }
    public record Unbaked() implements SpecialModelRenderer.Unbaked<Void> {
        public static final MapCodec<Unbaked> CODEC=MapCodec.unit(new Unbaked());
        @Override public MapCodec<Unbaked> type(){return CODEC;}
        @Override public CoffinLidSpecialRenderer bake(SpecialModelRenderer.BakingContext context) {
            return new CoffinLidSpecialRenderer(context.entityModelSet().bakeLayer(Coffin2Model.LAYER).getChild("lid"));
        }
    }
}
