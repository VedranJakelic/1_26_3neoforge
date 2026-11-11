package net.beamex.shamaschizm.sacredheart.client;

/** Two smooth contractions per 20-tick heartbeat, followed by a resting interval. */
public final class HeartbeatMotion {
    private HeartbeatMotion() {}
    public static float amount(float ticks) {
        float t = ((ticks % 20F) + 20F) % 20F;
        return pulse(t, 1F, 5F) + .55F * pulse(t, 6F, 9F);
    }
    private static float pulse(float t, float start, float end) {
        if (t <= start || t >= end) return 0F;
        float wave = (float)Math.sin(Math.PI * (t - start) / (end - start));
        return wave * wave;
    }
}
