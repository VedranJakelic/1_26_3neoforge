package net.beamex.shamaschizm.mixin;

import net.beamex.shamaschizm.eclipse.*;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.level.Level;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyRenderer.class)
public abstract class EclipseSkyMixin {
    @Shadow @Final private TextureAtlas celestialsAtlas;
    @Shadow protected abstract Matrix4f applyCelestialBodyTransform(PoseStack pose,float height,float scale);
    @Unique private EclipseSkyData.View shamaschizm$currentEclipse=EclipseSkyData.View.NONE;
    @Unique private EclipseSunMesh shamaschizm$eclipseMesh;
    @Inject(method="extractRenderState",at=@At("RETURN"))
    private void eclipseExtract(ClientLevel level,float partial,Camera camera,SkyRenderState state,CallbackInfo ci) {
        EclipseSkyData data=(EclipseSkyData)(Object)state;
        data.shamaschizm$eclipse(EclipseSkyData.View.NONE);
        if (!level.dimension().equals(Level.OVERWORLD)) return;
        long time=level.getOverworldClockTime(); var pos=camera.position();
        var a=EclipseOrbit.nearest(time);
        double strength=EclipseOrbit.darkness(time,pos.x,pos.z);
        if(strength<=0) return;
        state.skyColor=new Vector3f(state.skyColor).mul((float)(1-.94*strength));
        state.starBrightness=Math.max(state.starBrightness,(float)(.7*strength));
        if (state.sunriseAndSunsetColor!=null) {
            var tint=new Vector4f(state.sunriseAndSunsetColor); tint.w*=(float)(1-strength);
            state.sunriseAndSunsetColor=tint;
        }
        // Observer-dependent parallax, in the sun's local sky plane. The sun itself retains vanilla's path.
        double travel=(time-a.peak())/6000.0*1.35;
        data.shamaschizm$eclipse(new EclipseSkyData.View(true,(pos.z-a.z())/1000,
                -travel+(pos.x-a.x())/1000));
    }
    @Inject(method="render",at=@At("HEAD"))
    private void eclipseRender(GpuBufferSlice fog,SkyRenderState state,CallbackInfo ci) {
        shamaschizm$currentEclipse=((EclipseSkyData)(Object)state).shamaschizm$eclipse();
    }
    @Inject(method="renderSun",at=@At("HEAD"),cancellable=true)
    private void eclipseSun(RenderPass pass,float brightness,PoseStack pose,CallbackInfo ci) {
        var view=shamaschizm$currentEclipse;
        if(!view.visible()) return;
        if(shamaschizm$eclipseMesh==null) shamaschizm$eclipseMesh=new EclipseSunMesh();
        shamaschizm$eclipseMesh.render(pass,applyCelestialBodyTransform(pose,100,30),celestialsAtlas,
                brightness,view.offsetX(),view.offsetZ());
        ci.cancel();
    }
    @Inject(method="close",at=@At("HEAD"))
    private void eclipseClose(CallbackInfo ci) { if(shamaschizm$eclipseMesh!=null) shamaschizm$eclipseMesh.close(); }
}
