package net.beamex.shamaschizm.homunculus.client;

import java.util.*;
import java.util.function.Supplier;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.beamex.shamaschizm.homunculus.HomunculusEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.*;

final class HomunculusHumanoidRenderer extends MobRenderer<HomunculusEntity,AvatarRenderState,net.minecraft.client.model.player.PlayerModel> {
    // Only growth stages one and two are sent to this renderer.
    // The newborn uses a different renderer and a model without any player geometry.
    private final net.minecraft.client.model.player.PlayerModel normal,slim;
    // Bounded per-renderer cache: resolved profiles retain the feeder's texture properties across logout/reload.
    private final Map<GameProfile,Supplier<PlayerSkin>> skins=new LinkedHashMap<>(64,.75F,true){
        @Override protected boolean removeEldestEntry(Map.Entry<GameProfile,Supplier<PlayerSkin>> e){return size()>128;}
    };
    public HomunculusHumanoidRenderer(EntityRendererProvider.Context context){
        super(context,new GrownModel(context.bakeLayer(ModelLayers.PLAYER),false),.35F);
        addLayer(new net.minecraft.client.renderer.entity.layers.ItemInHandLayer<>(this));
        normal=model;slim=new GrownModel(context.bakeLayer(ModelLayers.PLAYER_SLIM),true);
        addLayer(new HumanoidArmorLayer<>(this,ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR,context.getModelSet(),
                part -> new HumanoidModel<AvatarRenderState>(part)),context.getEquipmentRenderer()));
    }
    @Override public AvatarRenderState createRenderState(){return new HomunculusRenderer.State();}
    @Override public Identifier getTextureLocation(AvatarRenderState state){
        return state.skin.body().texturePath();
    }
    @Override public void extractRenderState(HomunculusEntity mob,AvatarRenderState state,float partial){
        super.extractRenderState(mob,state,partial);
        HumanoidMobRenderer.extractHumanoidRenderState(mob,state,partial,itemModelResolver);((HomunculusRenderer.State)state).stage=mob.stage();
        GameProfile profile=mob.skinProfile().partialProfile();
        state.skin=mob.stage()==0?DefaultPlayerSkin.getDefaultSkin():skins.computeIfAbsent(profile,
                p->Minecraft.getInstance().getSkinManager().createLookup(p,false)).get();
        state.showHat=state.showJacket=state.showLeftSleeve=state.showRightSleeve=state.showLeftPants=state.showRightPants=true;
        state.isCrouching=mob.getPose()==Pose.CROUCHING;
        state.isBaby=false;state.ageScale=1;
    }
    @Override public void submit(AvatarRenderState state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){
        model=state.skin.model()==PlayerModelType.SLIM?slim:normal;
        super.submit(state,pose,collector,camera);
    }
    @Override protected boolean shouldRenderLayers(AvatarRenderState state){return ((HomunculusRenderer.State)state).stage>0;}
    @Override protected void scale(AvatarRenderState state,PoseStack pose){
        float scale=((HomunculusRenderer.State)state).stage==1?.5F:((HomunculusRenderer.State)state).stage==2?1.1F:1;
        pose.scale(scale,scale,scale);
    }
    @Override protected void setupRotations(AvatarRenderState state,PoseStack pose,float bodyRot,float scale){
        super.setupRotations(state,pose,bodyRot,scale);
        if(((HomunculusRenderer.State)state).stage==2&&state.pose==Pose.SWIMMING){pose.rotateDegrees(Axis.XP,-90);pose.translate(0,-1,.25);}
    }
    private static final class GrownModel extends net.minecraft.client.model.player.PlayerModel {
        GrownModel(net.minecraft.client.model.geom.ModelPart root,boolean slim){super(root,slim);}
        @Override public void setupAnim(AvatarRenderState state){
            super.setupAnim(state);
            if(((HomunculusRenderer.State)state).stage==1)head.xScale=head.yScale=head.zScale=1.25F;
        }
    }

}
