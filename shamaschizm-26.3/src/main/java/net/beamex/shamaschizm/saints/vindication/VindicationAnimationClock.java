package net.beamex.shamaschizm.saints.vindication;

/** Monotonic client animation time, gently corrected to the server's phase samples. */
public final class VindicationAnimationClock {
    private boolean initialized;
    private int sampleAge, sampleTick;
    private float previous, current;

    public void sample(int age, int localTick) {
        if (!initialized || age < sampleAge - 10) {
            initialized = true;
            previous = current = age;
        }
        sampleAge = age;
        sampleTick = localTick;
    }

    public void tick(int authoritativeAge, int localTick) {
        if (!initialized) {
            sample(authoritativeAge, localTick);
            return;
        }
        previous = current;
        // Extrapolate short packet gaps, but stop advancing during a prolonged server stall.
        int elapsed = Math.max(0, localTick - sampleTick);
        float target = sampleAge + Math.min(elapsed, 5);
        float correction = Math.max(-0.10F, Math.min(0.10F, (target - (current + 1)) * 0.10F));
        float next = current + 1 + correction;
        current = Math.max(current, Math.min(next, sampleAge + 5));
    }

    public float value(int fallbackAge, float partial) {
        if (!initialized) return fallbackAge;
        float fraction = Math.max(0, Math.min(1, partial));
        return previous + (current - previous) * fraction;
    }
}
