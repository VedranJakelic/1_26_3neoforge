package net.beamex.shamaschizm.serpent;

/** Braking makes a smooth swimmer able to finish a turn inside biting range. */
public final class SerpentPursuit {
 private SerpentPursuit(){}
 public static double speed(double distance,double alignment){
  double speed=.18+.54*Math.max(0,Math.min(1,(distance-2)/8));
  if(alignment<0)speed=Math.min(speed,.12);
  else if(alignment<.65)speed=Math.min(speed,.25);
  return speed;
 }
 public static double acceleration(double current,double target){return target<current?.40:.14;}
}
