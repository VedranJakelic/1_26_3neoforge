package net.beamex.shamaschizm.earthworm;
/** Original walk keys, with cumulative forward travel across the loop seam. */
public final class WormMotion {
 public static final double PERIOD=2.75, STRIDE=1.5/16.0;
 public static final int PARTS=9;
 public static final double[] REST={0.0,0.0625,0.125,0.21875,0.3125,0.375,0.4375,0.5,0.5625};
 private static final double[][][] POSITION={{{0,0.0},{0.25,-0.8},{0.5,-1.0},{0.625,-1.2},{0.75,-1.4},{0.875,-1.5}},{{0,0.0},{0.25,-0.25},{0.5,-0.45},{0.625,-0.8},{0.75,-1.15},{0.875,-1.3},{1,-1.45},{1.125,-1.5}},{{0,0.0},{0.25,-0.1},{0.5,-0.1},{0.625,-0.3},{0.75,-0.6},{0.875,-0.8},{1,-1.1},{1.125,-1.3},{1.25,-1.5},{1.375,-1.5}},{{0,0.0},{0.5,-0.1},{0.625,-0.3},{0.75,-0.5},{0.875,-0.65},{1,-0.9},{1.125,-1.1},{1.25,-1.3},{1.375,-1.5},{1.75,-1.5}},{{0,0.0},{0.625,-0.1},{0.75,-0.2},{0.875,-0.4},{1,-0.5},{1.125,-0.7},{1.25,-0.8},{1.375,-1.2},{1.75,-1.3},{2,-1.5},{2.375,-1.5}},{{0,0.0},{0.625,0.0},{0.75,-0.05},{0.875,-0.1},{1,-0.15},{1.125,-0.2},{1.25,-0.3},{1.375,-0.7},{1.75,-0.95},{2,-1.4},{2.375,-1.5}},{{0,0.0},{1.125,0.0},{1.25,-0.05},{1.375,-0.3},{1.75,-0.45},{2,-1.1},{2.375,-1.5}},{{0,0.0},{1.25,0.0},{1.375,-0.05},{1.75,-0.1},{2,-0.65},{2.375,-1.5}},{{0,0.0},{1.75,0.0},{2,-0.2},{2.375,-1.5}}}, SCALE={{{0,1.0},{0.25,1.8},{0.5,1.6},{0.625,1.2},{0.75,1.0},{0.875,1.0},{1.125,1.03},{1.25,1.0}},{{0,1.0},{0.25,1.3},{0.5,1.5},{0.625,1.6},{0.75,1.5},{0.875,1.4},{1,1.1},{1.125,1.0},{1.25,1.0}},{{0,1.0},{0.5,1.2},{0.625,1.4},{0.75,1.6},{1.125,1.4},{1.25,1.0},{1.375,1.0}},{{0,1.0},{0.5,1.1},{0.75,1.1},{1.25,1.1},{1.375,1.0},{1.75,1.0}},{{0,1.0},{0.625,1.2},{0.75,1.2},{0.875,1.4},{1,1.4},{1.125,1.6},{1.25,1.6},{1.375,1.6},{1.75,1.2},{2,1.0},{2.375,1.0}},{{0,1.0},{0.625,1.0},{0.75,1.1},{0.875,1.2},{1,1.3},{1.125,1.4},{1.75,1.5},{2,1.2},{2.375,1.0}},{{0,1.0},{1.125,1.0},{1.25,1.1},{1.375,1.4},{1.75,1.5},{2,1.4},{2.375,1.0}},{{0,1.0},{1.25,1.0},{1.375,1.1},{1.75,1.2},{2,1.5},{2.375,1.0}},{{0,1.0},{1.75,1.0},{2,1.4},{2.375,1.0}}};
 public static double sample(double[][] keys,double t){
  if(t<=keys[0][0])return keys[0][1];
  for(int i=1;i<keys.length;i++)if(t<=keys[i][0]){double f=(t-keys[i-1][0])/(keys[i][0]-keys[i-1][0]);return keys[i-1][1]+f*(keys[i][1]-keys[i-1][1]);}
  return keys[keys.length-1][1];
 }
 public static double forward(int part,double seconds){double cycle=Math.floor(seconds/PERIOD);return cycle*STRIDE-sample(POSITION[part],seconds-cycle*PERIOD)/16;}
 public static double behind(int part,double seconds){return REST[part]+forward(0,seconds)-forward(part,seconds);}
 public static float stretch(int part,double seconds){return (float)sample(SCALE[part],seconds-Math.floor(seconds/PERIOD)*PERIOD);}
 public static double wrap(double a){return Math.atan2(Math.sin(a),Math.cos(a));}
 public static double steer(double heading,double target,double distance){double limit=distance/0.35;return heading+Math.max(-limit,Math.min(limit,wrap(target-heading)));}
 private WormMotion(){}
}
