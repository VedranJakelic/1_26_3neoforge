package net.beamex.shamaschizm.saints.client;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.entity.custom.CoffinEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.phys.AABB;
public final class Coffin2Renderer extends MobRenderer<CoffinEntity,Coffin2Renderer.State,Coffin2Model> {
    public Coffin2Renderer(EntityRendererProvider.Context context){super(context,new Coffin2Model(context.bakeLayer(Coffin2Model.LAYER)),0);}
    @Override public State createRenderState(){return new State();}
    @Override public Identifier getTextureLocation(State state){return Shamaschizm.id("textures/entity/coffin2.png");}
    @Override protected AABB getBoundingBoxForCulling(CoffinEntity entity, float partialTicks){return entity.getBoundingBox().inflate(3);}
    @Override public void extractRenderState(CoffinEntity entity,State state,float partialTick){
        super.extractRenderState(entity,state,partialTick);state.open=entity.isOpen();state.lidGone=entity.isSaintsLidRemoved();
        state.animation.copyFrom(entity.getClientOpenAnimation());
    }
    public static final class State extends LivingEntityRenderState {
        public boolean open,lidGone;public final AnimationState animation=new AnimationState();
    }
}
