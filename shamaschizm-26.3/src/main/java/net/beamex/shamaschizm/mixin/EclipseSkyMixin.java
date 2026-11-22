package net.beamex.shamaschizm.mixin;
import net.beamex.shamaschizm.eclipse.*;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.MoonPhase;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(SkyRenderer.class)
public abstract class EclipseSkyMixin {
    @Shadow @Final private TextureAtlas celestialsAtlas;
    @Unique private EclipseSkyData.View shamaschizm$currentEclipse=EclipseSkyData.View.NONE;
    @Unique private EclipseMoonRenderer shamaschizm$moon;
    @Inject(method="extractRenderState",at=@At("RETURN"))
    private void eclipseExtract(ClientLevel level,float partial,Camera camera,SkyRenderState state,CallbackInfo ci){
        var data=(EclipseSkyData)(Object)state;data.shamaschizm$eclipse(EclipseSkyData.View.NONE);
        if(!level.dimension().equals(Level.OVERWORLD))return;
        double time=level.getOverworldClockTime()+partial;var p=camera.position();
        var sky=EclipseOrbit.sky(time,p.x,p.y,p.z);
        double dark=EclipseOrbit.darkness(time,p.x,p.y,p.z);
        state.sunAngle=(float)EclipseOrbit.sunAngle(time);
        state.skyColor=new Vector3f(state.skyColor).mul((float)(1-.94*dark));
        // Leave vanilla star visibility alone: an eclipse never forces daytime stars on.
        if(sky.sun().y()>0)state.starBrightness=0;
        if(state.sunriseAndSunsetColor!=null){var tint=new Vector4f(state.sunriseAndSunsetColor);
            tint.w*=(float)(1-dark);state.sunriseAndSunsetColor=tint;}
        data.shamaschizm$eclipse(new EclipseSkyData.View(true,sky,state.skyColor.x(),state.skyColor.y(),state.skyColor.z()));
    }
    @Inject(method="render",at=@At("HEAD"))
    private void eclipseRender(GpuBufferSlice fog,SkyRenderState state,CallbackInfo ci){
        shamaschizm$currentEclipse=((EclipseSkyData)(Object)state).shamaschizm$eclipse();
    }
    @ModifyArg(method="renderSun",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/SkyRenderer;applyCelestialBodyTransform(Lcom/mojang/blaze3d/vertex/PoseStack;FF)Lorg/joml/Matrix4f;"),index=2)
    private float eclipseSunSize(float original){
        return shamaschizm$currentEclipse.enabled()?(float)(100*Math.tan(EclipseOrbit.SUN_RADIUS)/.25):original;
    }
    @Inject(method="renderMoon",at=@At("HEAD"),cancellable=true)
    private void eclipseReplaceMoon(RenderPass pass,MoonPhase phase,float brightness,PoseStack pose,CallbackInfo ci){
        if(shamaschizm$currentEclipse.enabled())ci.cancel();
    }
    @Inject(method="renderSunMoonAndStars",at=@At("TAIL"))
    private void eclipseDrawMoon(RenderPass pass,PoseStack pose,float sun,float moon,float stars,
            MoonPhase phase,float weather,float starBrightness,CallbackInfo ci){
        if(!shamaschizm$currentEclipse.enabled())return;
        if(shamaschizm$moon==null)shamaschizm$moon=new EclipseMoonRenderer();
        shamaschizm$moon.render(pass,celestialsAtlas,shamaschizm$currentEclipse,weather,phase);
    }
    @Inject(method="close",at=@At("HEAD"))
    private void eclipseClose(CallbackInfo ci){if(shamaschizm$moon!=null)shamaschizm$moon.close();}
}
