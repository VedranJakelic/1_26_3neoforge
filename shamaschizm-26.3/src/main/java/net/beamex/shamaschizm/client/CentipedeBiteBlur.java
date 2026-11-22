package net.beamex.shamaschizm.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.centipede.BiteVisuals;
import net.beamex.shamaschizm.mixin.TrippingPostChainAccessor;
import net.beamex.shamaschizm.mixin.TrippingPostPassAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.world.phys.Vec3;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * The existing centipede motion-trail post-process, also driven by poppy stew's
 * five-second Poison phase. No additional softness/defocus shader is applied.
 * The client render hook already calls this renderer before the HUD.
 */
public final class CentipedeBiteBlur {
    private static final ByteBuffer DATA = ByteBuffer.allocateDirect(16).order(ByteOrder.nativeOrder());
    private static Object world, player;
    private static PostChain previous;
    private static Vec3 position;
    private static int width, height;
    private static long frame;
    private static boolean history;

    private CentipedeBiteBlur() {}

    public static void render(RenderTarget target, GraphicsResourceAllocator allocator) {
        var mc = Minecraft.getInstance();
        long now = System.nanoTime();
        float dt = frame == 0 ? 1F / 60F : (float) Math.min(0.1, (now - frame) / 1e9);
        frame = now;

        if (world != mc.level || player != mc.player) {
            world = mc.level;
            player = mc.player;
            history = false;
            position = null;
        }

        float age = (float) ((now - BiteVisuals.receivedNanos) / 1e9);
        boolean centipedeBite = mc.player != null && mc.player.getId() == BiteVisuals.playerId
                && age >= 0F && age <= 1.25F;
        // The five-second timer is sent from PoppyStewEvents when the stew is consumed.
        // It is not a separate visible MobEffect.
        boolean stewPoison = PoppyStewVisuals.blurActive();

        if (mc.player == null || mc.level == null || !mc.player.isAlive()
                || mc.getCameraEntity() != mc.player || mc.isPaused()
                || (!centipedeBite && !stewPoison)) {
            history = false;
            return;
        }

        // Reuse EXACTLY the same shader and persistent-history render target as
        // the centipede bite; no stew-specific blur pass is run.
        PostChain chain = mc.getShaderManager().getPostChain(
                Shamaschizm.id("centipede_bite"), LevelTargetBundle.MAIN_TARGETS);
        if (chain == null) {
            history = false;
            return;
        }

        if (chain != previous || width != target.width || height != target.height
                || (position != null && position.distanceToSqr(mc.player.position()) > 16)) {
            history = false;
        }
        previous = chain;
        width = target.width;
        height = target.height;
        position = mc.player.position();

        // The centipede's short radial impact still only happens on a bite.
        float radial = centipedeBite
                ? (float) Math.pow(Math.max(0, 1 - age / .24F), 2)
                : 0F;

        float biteFade = 0F;
        if (centipedeBite) {
            biteFade = Math.max(0, Math.min(1, (1.25F - age) / .65F));
            biteFade = biteFade * biteFade * (3 - 2 * biteFade);
        }

        // Stew uses the SAME temporal trail as the centipede. It fades in/out
        // across the 100-tick Poison interval, without any stationary soft focus.
        float stewFade = 0F;
        if (stewPoison) {
            int remaining = Math.max(0, Math.min(100, PoppyStewVisuals.blurTicks()));
            float fadeIn = Math.min(1F, (100 - remaining) / 8F);
            float fadeOut = Math.min(1F, remaining / 8F);
            stewFade = Math.max(0F, fadeIn * fadeOut);
        }

        float trail = history
                ? (float) Math.exp(-dt / .14F) * Math.max(biteFade, stewFade)
                : 0F;
        DATA.clear();
        DATA.putFloat(radial).putFloat(trail).putFloat(0).putFloat(0).flip();

        for (var pass : ((TrippingPostChainAccessor) (Object) chain).shamaschizm$passes()) {
            var uniforms = ((TrippingPostPassAccessor) (Object) pass).shamaschizm$uniforms();
            var buffer = uniforms.get("BiteConfig");
            if (buffer == null) continue;
            if ((buffer.usage() & GpuBuffer.USAGE_COPY_DST) == 0) {
                var dynamic = RenderSystem.getDevice().createBuffer(
                        () -> "Centipede/stew motion blur",
                        GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                        DATA.duplicate());
                uniforms.put("BiteConfig", dynamic);
                buffer.close();
            } else {
                RenderSystem.getDevice().createCommandEncoder()
                        .writeToBuffer(buffer.slice(), DATA.duplicate());
            }
        }
        chain.process(target, allocator);
        history = true;
    }
}
