package net.beamex.shamaschizm.world;

import net.minecraft.world.entity.MobCategory;

/** Budgets are per 289 eligible chunks globally and per spawning player locally. */
public final class SchizmPopulation {
    private SchizmPopulation() {}
    public static int cap(MobCategory category) {
        return switch (category) {
            case MONSTER -> 70;
            case AMBIENT -> 264;
            default -> category.getMaxInstancesPerChunk();
        };
    }
    public interface SpawnState {
        void shamaschizm$setSchizm(boolean schizm);
    }
}
