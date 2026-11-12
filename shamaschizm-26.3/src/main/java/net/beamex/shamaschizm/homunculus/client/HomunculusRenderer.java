package net.beamex.shamaschizm.homunculus.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.homunculus.HomunculusEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;

/** Keep the public state on Minecraft's entity-type dispatch path for every growth stage. */
public final class HomunculusRenderer extends EntityRenderer<HomunculusEntity,HomunculusRenderer.State> {
    // The dispatcher routes AvatarRenderState (including subclasses) to the player renderer.
    // Never expose the internal avatar state as this entity's public render state.
    public static final class State extends LivingEntityRenderState {
        public int stage;
        final HomunculusHumanoidRenderer.State humanoid = new HomunculusHumanoidRenderer.State();
    }
    private final NewbornRenderer newborn;
    private final HomunculusHumanoidRenderer humanoid;
    public HomunculusRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius=.35F;
        newborn=new NewbornRenderer(context);
        humanoid=new HomunculusHumanoidRenderer(context);
    }
    @Override public State createRenderState() { return new State(); }
    @Override public void extractRenderState(HomunculusEntity mob,State state,float partial) {
        super.extractRenderState(mob,state,partial);
        state.stage=mob.stage();
        if(mob.stage()==0)newborn.extractRenderState(mob,state,partial);
        else humanoid.extractRenderState(mob,state.humanoid,partial);
    }
    @Override public void submit(State state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera) {
        if(state.stage==0)newborn.submit(state,pose,collector,camera);
        else humanoid.submit(state.humanoid,pose,collector,camera);
    }
    @Override protected AABB getBoundingBoxForCulling(HomunculusEntity mob,float partial) {
        return mob.getBoundingBox().inflate(.4);
    }
    private static final class NewbornRenderer extends MobRenderer<HomunculusEntity,State,HomunculusModel> {
        private static final Identifier TEXTURE=Shamaschizm.id("textures/entity/homunculus.png");
        NewbornRenderer(EntityRendererProvider.Context context) {
            super(context,new HomunculusModel(context.bakeLayer(HomunculusModel.NORMAL),false),.25F);
        }
        @Override public State createRenderState() { return new State(); }
        @Override public Identifier getTextureLocation(State state) { return TEXTURE; }
        @Override protected boolean shouldRenderLayers(State state) { return false; }
    }
}

