package net.beamex.shamaschizm.astral;
/** Rules measured in server ticks. */
public final class AstralRules {
 public static final int MAX_TRIP_LEVEL=7,RETURN_TICKS=20,GAZE_DEATH_TICKS=240;
 public static final double RETURN_RADIUS=1.25,SPEED=.22,FAST_SPEED=.44;
 public static float gaze(float old,boolean looking){return Math.max(0,Math.min(1,old+(looking?1F/GAZE_DEATH_TICKS:-1F/100)));}
 public static boolean finite(float... values){for(float v:values)if(!Float.isFinite(v))return false;return true;}
 private AstralRules(){}
}
