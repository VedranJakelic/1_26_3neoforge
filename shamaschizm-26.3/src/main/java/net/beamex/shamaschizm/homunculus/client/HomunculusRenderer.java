package net.beamex.shamaschizm.homunculus.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.homunculus.HomunculusEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;

/** Distinct model trees prevent player geometry/UVs from leaking into the newborn form. */
public final class HomunculusRenderer extends EntityRenderer<HomunculusEntity,AvatarRenderState> {
    public static final class State extends AvatarRenderState { public int stage; }
    private final NewbornRenderer newborn;
    private final HomunculusHumanoidRenderer humanoid;
    public HomunculusRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius=.35F;
        newborn=new NewbornRenderer(context);
        humanoid=new HomunculusHumanoidRenderer(context);
    }
    @Override public AvatarRenderState createRenderState() { return new State(); }
    @Override public void extractRenderState(HomunculusEntity mob,AvatarRenderState state,float partial) {
        ((State)state).stage=mob.stage();
        if(mob.stage()==0)newborn.extractRenderState(mob,state,partial);
        else humanoid.extractRenderState(mob,state,partial);
    }
    @Override public void submit(AvatarRenderState state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera) {
        if(((State)state).stage==0)newborn.submit(state,pose,collector,camera);
        else humanoid.submit(state,pose,collector,camera);
    }
    @Override protected AABB getBoundingBoxForCulling(HomunculusEntity mob,float partial) {
        return mob.getBoundingBox().inflate(.4);
    }
    private static final class NewbornRenderer extends MobRenderer<HomunculusEntity,AvatarRenderState,HomunculusModel> {
        private static final Identifier TEXTURE=Shamaschizm.id("textures/entity/homunculus.png");
        NewbornRenderer(EntityRendererProvider.Context context) {
            super(context,new HomunculusModel(context.bakeLayer(HomunculusModel.NORMAL),false),.25F);
        }
        @Override public AvatarRenderState createRenderState() { return new State(); }
        @Override public Identifier getTextureLocation(AvatarRenderState state) { return TEXTURE; }
        @Override protected boolean shouldRenderLayers(AvatarRenderState state) { return false; }
    }
}
