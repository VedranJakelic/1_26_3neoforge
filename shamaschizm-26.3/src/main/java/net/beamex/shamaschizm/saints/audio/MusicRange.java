package net.beamex.shamaschizm.saints.audio;
/** Listening radii in blocks. Volume is constant inside each sphere, zero outside. */
public final class MusicRange {
 public static final double ARENA_RADIUS=64;
 public static final double DISC_RADIUS=32;
 private MusicRange(){}
 public static boolean audible(double distanceSquared,double radius){return distanceSquared<=radius*radius;}
}
