package net.beamex.shamaschizm.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.localportal.client.DoorwayDebugRenderer;
import net.beamex.shamaschizm.mixin.TrippingPostChainAccessor;
import net.beamex.shamaschizm.mixin.TrippingPostPassAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Depth-projected eyes: applied before scene distortions and before the HUD. */
public final class SurfaceEyeEffects {
    private static final Matrix4f PROJECTION = new Matrix4f();
    private static boolean captured;
    private static final ByteBuffer DATA = ByteBuffer.allocateDirect(352).order(ByteOrder.nativeOrder());
    private SurfaceEyeEffects() {}
    public static net.minecraft.world.phys.Vec3 projectDirection(net.minecraft.world.phys.Vec3 direction) {
        var camera=Minecraft.getInstance().gameRenderer.gameRenderState().levelRenderState.cameraRenderState;
        var p=new org.joml.Vector3f((float)direction.x*64,(float)direction.y*64,(float)direction.z*64);
        new Matrix4f(PROJECTION).mul(camera.viewRotationMatrix).transformProject(p);
        return new net.minecraft.world.phys.Vec3(p.x*.5+.5,p.y*.5+.5,1);
    }
    public static void beginFrame() { captured = false; }
    public static void captureProjection(Matrix4f matrix) { PROJECTION.set(matrix); captured = true; }
    public static void render(RenderTarget target, GraphicsResourceAllocator allocator, float time, float intensity) {
        var mc = Minecraft.getInstance();
        if (!captured || mc.level == null || mc.player == null || intensity <= 0.001F) return;
        captured = false;
        var camera = mc.gameRenderer.gameRenderState().levelRenderState.cameraRenderState;
        var pos = camera.pos;
        // Portal composites carry doorway-plane depth, not remote geometry depth.
        // Exclude visible apertures instead of drawing false decals over windows.
        var doors = DoorwayDebugRenderer.doorways().stream().filter(d -> d.enabled()
                && d.origin().distanceToSqr(pos) < 40*40).toList();
        if (doors.size() > 8) return; // Conservative fallback rather than unmasked windows.
        var chain = mc.getShaderManager().getPostChain(Shamaschizm.id("surface_eyes"), LevelTargetBundle.MAIN_TARGETS);
        if (chain == null) return;
        Matrix4f inverse = new Matrix4f(PROJECTION).mul(camera.viewRotationMatrix).invert();
        float depth = Mth.clamp((160F-(float)pos.y)/100F,0F,1F);
        float gain = TripVisionClient.eyeGain();
        DATA.clear(); inverse.get(0,DATA); DATA.position(64);
        DATA.putFloat((float)pos.x).putFloat((float)pos.y).putFloat((float)pos.z).putFloat(TripVisionClient.density());
        DATA.putFloat(time).putFloat(intensity*gain).putFloat(doors.size()).putFloat(RenderSystem.getDevice().getDeviceInfo().isZZeroToOne() ? 1F : 0F);
        for (int i=0;i<8;i++) {
            if(i<doors.size()) {
                var d=doors.get(i); var o=d.origin().subtract(pos);
                DATA.putFloat((float)o.x).putFloat((float)o.y).putFloat((float)o.z).putFloat((float)d.height());
                DATA.putFloat((float)d.facing().right.x).putFloat(0).putFloat((float)d.facing().right.z).putFloat((float)d.width());
            } else for(int k=0;k<8;k++) DATA.putFloat(0);
        }
        DATA.flip();
        for(var pass:((TrippingPostChainAccessor)(Object)chain).shamaschizm$passes()) {
            var uniforms=((TrippingPostPassAccessor)(Object)pass).shamaschizm$uniforms();
            GpuBuffer buffer=uniforms.get("SurfaceConfig");
            if(buffer==null) continue;
            if((buffer.usage() & GpuBuffer.USAGE_COPY_DST)==0) {
                GpuBuffer dynamic=RenderSystem.getDevice().createBuffer(() -> "Schizm surface eyes",
                        GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,DATA.duplicate());
                uniforms.put("SurfaceConfig",dynamic);buffer.close();
            } else RenderSystem.getDevice().createCommandEncoder().writeToBuffer(buffer.slice(),DATA.duplicate());
        }
        chain.process(target,allocator);
    }
}
