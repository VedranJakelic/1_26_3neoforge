package net.beamex.shamaschizm.livingsmoke;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Shared pearl paths: identical on the server (death spawn) and in the renderer. */
public final class LivingSmokePearls {
    private static final float PI = (float)Math.PI;
    private static final float RADIUS = .8125F;
    private LivingSmokePearls() {}

    public static float angle(int index, float age) {
        float phase = (age / 240F * 10F) % 10F;
        return 2 * PI * index / 16F - 2 * PI * phase / 10F;
    }
    public static Vec3 position(int index, float age, float reaction) {
        float angle = angle(index, age);
        float x = -RADIUS * (float)Math.sin(angle);
        float y =  RADIUS * (float)Math.cos(angle);
        float z = -.14933F;
        if (reaction >= 0) {
            float t = Mth.clamp((reaction - (index % 8) * .006F) / .958F, 0, 1);
            float orbit = angle + t * PI * 1.3F;
            float bx = .23F * (float)Math.sin(orbit);
            float by = .23F * (float)Math.cos(orbit);
            if (t < .26F) {
                float a = smooth(t / .26F);
                x = Mth.lerp(a, x, bx); y = Mth.lerp(a, y, by); z = Mth.lerp(a, z, 1.25F);
            } else if (t < .53F) {
                float a = smooth((t - .26F) / .27F);
                x = Mth.lerp(a, bx, bx * .2F); y = Mth.lerp(a, by, by * .2F);
                z = Mth.lerp(a, 1.25F, 0);
            } else if (t < .71F) {
                float a = smooth((t - .53F) / .18F);
                x = Mth.lerp(a, bx * .2F, bx * .6F); y = Mth.lerp(a, by * .2F, by * .6F);
                z = Mth.lerp(a, 0, -1.10F);
            } else {
                float a = smooth((t - .71F) / .29F);
                x = Mth.lerp(a, bx * .6F, x); y = Mth.lerp(a, by * .6F, y);
                z = Mth.lerp(a, -1.10F, z);
            }
        }
        return new Vec3(x, y, z);
    }
    private static float smooth(float v) { v = Mth.clamp(v,0,1); return v*v*(3-2*v); }
}
