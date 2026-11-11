package net.beamex.shamaschizm.client;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.systems.RenderSystem;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.effect.ModEffects;
import net.beamex.shamaschizm.mixin.TrippingPostChainAccessor;
import net.beamex.shamaschizm.mixin.TrippingPostPassAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Local world distortion plus a post-HUD fractal overlay; no movement or aim changes. */
@EventBusSubscriber(modid = Shamaschizm.MOD_ID, value = Dist.CLIENT)
public final class ClientTrippyEffects {
    private static final Identifier OVERLAY = Shamaschizm.id("tripping_fractals");
    private static float severity, soundGain = 1F;
    private static long overlayFrame;
    public static float soundGain() { return soundGain; }
    private static final Identifier EYES = Shamaschizm.id("pareidolia");
    private static PostChain previousEyeChain;
    private static float eyeDepth;
    private static final Identifier EFFECT = Shamaschizm.id("tripping");
    private static Object lastLevel, lastPlayer;
    private static Vec3 previousPosition;
    private static float previousYaw, previousPitch, intensity, motion;
    private static double time, fractalRotation;
    private static float headTurn;
    private static long lastFrame;
    private static PostChain previousChain;
    private static int width, height;
    private static boolean historyValid, eyeHistoryValid;
    private static final ByteBuffer UNIFORMS = ByteBuffer.allocateDirect(16).order(ByteOrder.nativeOrder());
    private ClientTrippyEffects() {}

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.level != lastLevel || mc.player != lastPlayer) {
            TripVisionClient.reset();
            lastLevel = mc.level; lastPlayer = mc.player;
            previousPosition = null; motion = 0; headTurn = 0;
            if (mc.level == null || mc.player == null) { eyeDepth = 0; previousEyeChain = null; intensity = 0; severity = 0; soundGain = 1; time = 0; fractalRotation = 0; overlayFrame = 0; }
            historyValid = eyeHistoryValid = false; lastFrame = 0;
        }
        if (mc.player == null || mc.level == null || mc.isPaused()) return;
        TripVisionClient.tick();
        var player = mc.player;
        Vec3 position = player.position();
        float targetMotion = 0;
        float targetTurn = 0;
        if (previousPosition != null) {
            double travel = position.distanceTo(previousPosition);
            float turn = Math.abs(Mth.wrapDegrees(player.getYRot() - previousYaw))
                    + Math.abs(player.getXRot() - previousPitch);
            if (travel > 4) historyValid = eyeHistoryValid = false; // Teleports must not drag the previous scene along.
            else {
                targetMotion = Mth.clamp((float)(travel / 0.30) + turn / 18F, 0F, 1F);
                targetTurn = Mth.clamp(turn / 12F, 0F, 1F);
            }
        }
        motion += (targetMotion - motion) * 0.25F;
        headTurn += (targetTurn - headTurn) * 0.35F;
        previousPosition = position; previousYaw = player.getYRot(); previousPitch = player.getXRot();
        if (intensity <= 0) historyValid = eyeHistoryValid = false;
    }

    public static void renderSurfaces(RenderTarget target, GraphicsResourceAllocator allocator) {
        var mc = Minecraft.getInstance();
        if (mc.player != null && mc.getCameraEntity() == mc.player)
            SurfaceEyeEffects.render(target, allocator, (float)time, intensity);
    }

    public static void render(RenderTarget target, GraphicsResourceAllocator allocator) {
        var mc = Minecraft.getInstance();
        long now = System.nanoTime();
        float dt = lastFrame == 0 ? 1F / 60F : (float)Math.min(0.1, (now - lastFrame) / 1e9);
        lastFrame = now;
        if (mc.player == null || mc.level != lastLevel || intensity <= 0.001F
                || mc.getCameraEntity() != mc.player) { historyValid = eyeHistoryValid = false; return; }

        PostChain chain = mc.getShaderManager().getPostChain(EFFECT, LevelTargetBundle.MAIN_TARGETS);
        if (chain == null) { historyValid = eyeHistoryValid = false; return; }
        if (chain != previousChain || width != target.width || height != target.height) historyValid = eyeHistoryValid = false;
        previousChain = chain; width = target.width; height = target.height;
        // Short, frame-rate-adjusted persistence, strongest during movement.
        float trail = historyValid && !mc.isPaused()
                ? (float)Math.exp(-dt / 0.018) * motion * intensity : 0F;
        uniforms(chain, "TrippingConfig", (float)time, intensity, motion, trail);
        chain.process(target, allocator);
        historyValid = true;
    }

    /** Runs after GUI rendering, including the death screen. Fades use wall time so
     * a paused death/menu screen cannot leave the overlay or audio stuck forever. */
    public static void renderOverlay(RenderTarget target, GraphicsResourceAllocator allocator) {
        var mc = Minecraft.getInstance();
        long now = System.nanoTime();
        float dt = overlayFrame == 0 ? 1F/60F : (float)Math.min(0.1, (now-overlayFrame)/1e9);
        overlayFrame = now;
        if (mc.level == null || mc.player == null) {
            intensity = 0; severity = 0; soundGain = 1; historyValid = eyeHistoryValid = false; return;
        }
        boolean alive = mc.player.isAlive();
        boolean active = alive && mc.player.hasEffect(ModEffects.TRIPPING);
        if (!mc.isPaused() || !active) {
            intensity = Mth.approach(intensity, active ? TripVisionClient.overall() : 0F, dt/(active ? 2F : 6F));
            time += dt;
            // Integrate speed rather than multiplying time: stopping a turn never snaps the pattern back.
            fractalRotation += dt * (0.035 + (alive ? headTurn : 0F) * 0.9);
        }
        if (active) {
            float health = mc.player.getHealth();
            float healthRange = Math.max(1F, mc.player.getMaxHealth()-4F);
            float desired = Mth.clamp((mc.player.getMaxHealth()-health)/healthRange, 0F, 1F);
            severity = Mth.approach(severity, desired, dt/1.5F);
        } else if (!alive && intensity > 0) severity = 1F;
        soundGain = Mth.approach(soundGain, 1F-intensity*severity, dt/3F);
        if (intensity <= 0.001F) { severity = 0; historyValid = eyeHistoryValid = false; return; }
        PostChain chain = mc.getShaderManager().getPostChain(OVERLAY, LevelTargetBundle.MAIN_TARGETS);
        if (chain == null) return;
        // Form scene eyes first; the fractal shader forms its own coherent eyes.
        // One pass over the completed scene, including composited local portals.
        // Match the existing Wisdom/bug depth range; outside remains faint.
        boolean schizm = mc.level.dimension().equals(net.beamex.shamaschizm.world.Schizm.KEY);
        float depth = Mth.clamp((160F - (float)mc.player.getY()) / 100F, 0F, 1F);
        float desiredEyes = TripVisionClient.eyeGain();
        eyeDepth = Mth.approach(eyeDepth, desiredEyes, mc.isPaused() && active ? 0F : dt / 2F);
        PostChain eyes = mc.getShaderManager().getPostChain(EYES, LevelTargetBundle.MAIN_TARGETS);
        if (eyes != null) {
            uniforms(eyes, "EyeConfig", (float)time, intensity * eyeDepth * 0.75F,
                    target.width / (float)Math.max(1, target.height), mc.isPaused() && active ? 0F : dt);
            uniforms(eyes, "EyeHistory", eyeHistoryValid && eyes == previousEyeChain ? 1F : 0F, motion, 0F, 0F);
            eyes.process(target, allocator);
        }
        previousEyeChain = eyes;
        eyeHistoryValid = eyes != null;
        uniforms(chain, "FractalConfig", (float)time, intensity, severity, (float)fractalRotation);
        uniforms(chain, "FractalEyes", eyeDepth, TripVisionClient.density(), 0F, 0F);
        Vec3 anchor = TripVisionClient.screenAnchor();
        uniforms(chain, "TripVision", (float)anchor.x, (float)anchor.y,
                TripVisionClient.age(), TripVisionClient.opacity()*(float)anchor.z);
        chain.process(target, allocator);

    }

    private static void uniforms(PostChain chain, String name, float a, float b, float c, float d) {
        var data = UNIFORMS;
        data.clear(); data.putFloat(a).putFloat(b).putFloat(c).putFloat(d).flip();
        for (var pass : ((TrippingPostChainAccessor)(Object)chain).shamaschizm$passes()) {
            var uniforms = ((TrippingPostPassAccessor)(Object)pass).shamaschizm$uniforms();
            GpuBuffer buffer = uniforms.get(name);
            if (buffer == null) continue;
            if ((buffer.usage() & GpuBuffer.USAGE_COPY_DST) == 0) {
                GpuBuffer dynamic = RenderSystem.getDevice().createBuffer(() -> "Shamaschizm trip parameters",
                        GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, data.duplicate());
                uniforms.put(name, dynamic); buffer.close();
            } else RenderSystem.getDevice().createCommandEncoder().writeToBuffer(buffer.slice(), data.duplicate());
        }
    }
}

