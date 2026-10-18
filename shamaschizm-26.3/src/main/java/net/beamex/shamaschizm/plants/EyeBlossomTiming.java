package net.beamex.shamaschizm.plants;

/** Tick-based animation timing, shared by both upright and hanging blossoms. */
final class EyeBlossomTiming {
    static final double RANGE_SQUARED = 25.0;
    // Ten four-tick intervals: no closing frame before 40 ticks of absence.
    static final int ABSENCE_STEPS = 10;
    record Step(int stage, boolean opening, int absent, int delay) {}

    static Step next(int stage, boolean opening, int absent, boolean near, int stagger) {
        if (near) {
            if (stage == 0 && !opening) return new Step(0, true, 0, 1 + stagger);
            if (stage < 3) return new Step(stage + 1, false, 0, 4);
            return new Step(3, false, 0, 2);
        }
        if (stage == 0) return new Step(0, false, 0, 2);
        if (absent < ABSENCE_STEPS) return new Step(stage, false, absent + 1, 4);
        // Keep the completed absence timer while closing, so the hold happens once.
        return new Step(stage - 1, false, stage == 1 ? 0 : ABSENCE_STEPS, 6);
    }
}
