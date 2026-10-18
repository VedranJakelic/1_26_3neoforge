package net.beamex.shamaschizm.elf.client;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.effect.ModEffects;
import net.beamex.shamaschizm.elf.ElfEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
public final class ElfRenderer extends MobRenderer<ElfEntity,LivingEntityRenderState,ElfModel>{
    public ElfRenderer(EntityRendererProvider.Context c){super(c,new ElfModel(c.bakeLayer(ElfModel.LAYER)),0F);}
    @Override public LivingEntityRenderState createRenderState(){return new LivingEntityRenderState();}
    @Override public Identifier getTextureLocation(LivingEntityRenderState s){return Shamaschizm.id("textures/entity/elf1.png");}
    @Override public boolean shouldRender(ElfEntity e,Frustum f,double x,double y,double z,float partialTicks){
        var p=Minecraft.getInstance().player;
        return p!=null&&p.hasEffect(ModEffects.TRIPPING)&&super.shouldRender(e,f,x,y,z,partialTicks);
    }
}
