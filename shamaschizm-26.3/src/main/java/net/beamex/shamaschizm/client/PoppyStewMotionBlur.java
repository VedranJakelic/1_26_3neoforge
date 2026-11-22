package net.beamex.shamaschizm.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;

/**
 * Compatibility shim for the old stew render hook. The stew Poison phase is
 * now rendered by CentipedeBiteBlur.render(), which the existing mixin already
 * calls BEFORE this method. Keeping this entry point avoids changing the mixin
 * and, crucially, stops the former soft-focus stew post-process entirely.
 */
public final class PoppyStewMotionBlur {
    private PoppyStewMotionBlur() {}

    public static void render(RenderTarget target, GraphicsResourceAllocator allocator) {
        // Deliberately empty; do not render stew_motion_blur again.
    }
}
