package net.beamex.shamaschizm.eclipse;

import net.beamex.shamaschizm.world.Schizm;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public final class Eclipses {
    private Eclipses() {}
    public static boolean active(Level level, BlockPos pos) {
        return level.dimension().equals(Level.OVERWORLD)
                && EclipseOrbit.active(level.getOverworldClockTime(), pos.getX()+.5, pos.getZ()+.5);
    }
    public static boolean habitat(Level level, BlockPos pos) {
        return level.dimension().equals(Schizm.KEY) || active(level,pos);
    }
    public static int y(Level level, BlockPos pos) { return active(level,pos) ? 0 : pos.getY(); }
}
