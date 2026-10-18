package net.beamex.shamaschizm.stag.client;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.stag.StagEntity;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
public final class StagRenderer extends MobRenderer<StagEntity,StagRenderer.State,StagModel>{
 public StagRenderer(EntityRendererProvider.Context c){super(c,new StagModel(c.bakeLayer(StagModel.LAYER)),0.5F);}
 public static final class State extends LivingEntityRenderState{public boolean changed;}
 @Override public State createRenderState(){return new State();}
 @Override public Identifier getTextureLocation(State s){return Shamaschizm.id(s.changed?"textures/entity/stag2.png":"textures/entity/stag1.png");}
 @Override public void extractRenderState(StagEntity e,State s,float partial){super.extractRenderState(e,s,partial);s.changed=e.unseenForm();}
 @Override protected AABB getBoundingBoxForCulling(StagEntity e, float partialTicks){return e.getBoundingBox().expandTowards(0,1.5,0).inflate(0.4);}
}
