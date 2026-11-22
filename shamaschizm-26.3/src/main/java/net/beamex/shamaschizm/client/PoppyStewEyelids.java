package net.beamex.shamaschizm.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.mixin.TrippingPostChainAccessor;
import net.beamex.shamaschizm.mixin.TrippingPostPassAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.util.Mth;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Draws the closing and reopening eyelids over world and HUD. */
public final class PoppyStewEyelids {
    private static final ByteBuffer DATA = ByteBuffer.allocateDirect(16).order(ByteOrder.nativeOrder());
    private static final int TOTAL_TICKS = 90;
    private static float closed;
    private static long lastFrame;
    private static Object lastPlayer, lastWorld;
    private static boolean missingShaderLogged;
    private static boolean shaderRenderedLogged;

    private PoppyStewEyelids() {}

    public static void render(RenderTarget target, GraphicsResourceAllocator allocator) {
        Minecraft mc = Minecraft.getInstance();
        long now = System.nanoTime();
        float dt = lastFrame == 0 ? 1F / 60F :
                (float) Math.min(0.1, (now - lastFrame) / 1e9);
        lastFrame = now;

        if (mc.player != lastPlayer || mc.level != lastWorld) {
            lastPlayer = mc.player;
            lastWorld = mc.level;
            closed = 0F;
        }
        if (mc.player == null || mc.level == null || !mc.player.isAlive()
                || mc.getCameraEntity() != mc.player) {
            closed = 0F;
            return;
        }

        // All timing is synchronized by a custom packet, not a visible MobEffect.
        int remaining = Mth.clamp(PoppyStewVisuals.eyelidTicks(), 0, TOTAL_TICKS);
        float targetClosed;
        if (remaining > 30) targetClosed = (TOTAL_TICKS - remaining) / 60F;
        else if (remaining > 20) targetClosed = 1F;
        else if (remaining > 0) targetClosed = remaining / 20F;
        else targetClosed = 0F;

        if (!mc.isPaused()) closed = Mth.approach(closed, targetClosed, dt * 12F);
        if (closed < 0.001F) return;

        PostChain chain = mc.getShaderManager().getPostChain(
                Shamaschizm.id("stew_eyelids"), LevelTargetBundle.MAIN_TARGETS);
        if (chain == null) {
            if (!missingShaderLogged) {
                Shamaschizm.LOGGER.warn("Poppy stew eyelid post chain missing: shamaschizm:stew_eyelids");
                missingShaderLogged = true;
            }
            return;
        }
        missingShaderLogged = false;

        DATA.clear();
        DATA.putFloat(Mth.clamp(closed, 0F, 1F)).putFloat(0).putFloat(0).putFloat(0).flip();
        for (var pass : ((TrippingPostChainAccessor) (Object) chain).shamaschizm$passes()) {
            var uniforms = ((TrippingPostPassAccessor) (Object) pass).shamaschizm$uniforms();
            GpuBuffer buffer = uniforms.get("EyelidConfig");
            if (buffer == null) continue;
            if ((buffer.usage() & GpuBuffer.USAGE_COPY_DST) == 0) {
                GpuBuffer dynamic = RenderSystem.getDevice().createBuffer(
                        () -> "Poppy stew eyelids",
                        GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                        DATA.duplicate());
                uniforms.put("EyelidConfig", dynamic);
                buffer.close();
            } else {
                RenderSystem.getDevice().createCommandEncoder()
                        .writeToBuffer(buffer.slice(), DATA.duplicate());
            }
        }
        if (!shaderRenderedLogged) {
            Shamaschizm.LOGGER.info("Poppy stew eyelid shader rendering active");
            shaderRenderedLogged = true;
        }
        chain.process(target, allocator);
    }
}
