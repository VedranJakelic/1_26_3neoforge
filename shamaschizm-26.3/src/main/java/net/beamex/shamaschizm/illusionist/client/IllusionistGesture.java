package net.beamex.shamaschizm.illusionist.client;
/** Three-second sequence: a small wave, opposite index-up hold, conversational gestures.
 * Channels: outward x/y/z, pitch/yaw/roll, legacy grip/point, five finger curls. */
final class IllusionistGesture {
 static final float PERIOD=3.0F;
 private static final float[] TIMES={0.0F,0.32F,0.49F,0.69F,0.90F,1.15F,1.55F,1.95F,2.35F,2.7F,3.0F};
 private static final float[][] LEFT={
  {-0.1F,-0.05F,-0.22F,45.0F,0.0F,-32.0F,0.252F,0.0F,0.16F,0.23F,0.28F,0.34F,0.25F},
  {0.02F,0.20F,-0.27F,12.0F,-8.0F,-12.0F,0.124F,0.0F,0.04F,0.08F,0.12F,0.18F,0.2F},
  {0.03F,0.21F,-0.27F,12.0F,0.0F,3.0F,0.124F,0.0F,0.04F,0.08F,0.12F,0.18F,0.2F},
  {0.02F,0.20F,-0.27F,12.0F,6.0F,-12.0F,0.124F,0.0F,0.04F,0.08F,0.12F,0.18F,0.2F},
  {-0.08F,0.02F,-0.23F,35.0F,0.0F,-25.0F,0.252F,0.0F,0.16F,0.23F,0.28F,0.34F,0.25F},
  {-0.1F,-0.05F,-0.22F,45.0F,0.0F,-32.0F,0.252F,0.0F,0.16F,0.23F,0.28F,0.34F,0.25F},
  {-0.1F,-0.05F,-0.22F,45.0F,0.0F,-32.0F,0.252F,0.0F,0.16F,0.23F,0.28F,0.34F,0.25F},
  {0.08F,0.02F,-0.32F,60.0F,12.0F,-45.0F,0.124F,0.0F,0.04F,0.08F,0.12F,0.18F,0.2F},
  {-0.02F,0.09F,-0.29F,38.0F,-8.0F,-22.0F,0.252F,0.0F,0.16F,0.23F,0.28F,0.34F,0.25F},
  {-0.08F,-0.03F,-0.24F,48.0F,2.0F,-30.0F,0.252F,0.0F,0.16F,0.23F,0.28F,0.34F,0.25F},
  {-0.1F,-0.05F,-0.22F,45.0F,0.0F,-32.0F,0.252F,0.0F,0.16F,0.23F,0.28F,0.34F,0.25F},
 };
 private static final float[][] RIGHT={
  {-0.1F,-0.05F,-0.22F,45.0F,0.0F,-32.0F,0.252F,0.0F,0.16F,0.23F,0.28F,0.34F,0.25F},
  {-0.1F,-0.05F,-0.22F,45.0F,0.0F,-32.0F,0.252F,0.0F,0.16F,0.23F,0.28F,0.34F,0.25F},
  {-0.1F,-0.05F,-0.22F,45.0F,0.0F,-32.0F,0.252F,0.0F,0.16F,0.23F,0.28F,0.34F,0.25F},
  {-0.1F,-0.05F,-0.22F,45.0F,0.0F,-32.0F,0.252F,0.0F,0.16F,0.23F,0.28F,0.34F,0.25F},
  {-0.08F,0.08F,-0.26F,25.0F,0.0F,-18.0F,0.252F,0.0F,0.16F,0.23F,0.28F,0.34F,0.25F},
  {-0.1F,0.26F,-0.29F,0.0F,-5.0F,-3.0F,0.72F,1.0F,0.0F,0.95F,1.0F,1.0F,0.65F},
  {-0.1F,0.26F,-0.29F,0.0F,-5.0F,-3.0F,0.72F,1.0F,0.0F,0.95F,1.0F,1.0F,0.65F},
  {-0.02F,0.13F,-0.32F,35.0F,-8.0F,-18.0F,0.252F,0.0F,0.16F,0.23F,0.28F,0.34F,0.25F},
  {0.09F,0.03F,-0.35F,62.0F,12.0F,-42.0F,0.124F,0.0F,0.04F,0.08F,0.12F,0.18F,0.2F},
  {0.01F,0.03F,-0.27F,48.0F,3.0F,-28.0F,0.252F,0.0F,0.16F,0.23F,0.28F,0.34F,0.25F},
  {-0.1F,-0.05F,-0.22F,45.0F,0.0F,-32.0F,0.252F,0.0F,0.16F,0.23F,0.28F,0.34F,0.25F},
 };
 // Periodic, monotone key velocities. Passing poses retain momentum; extrema and
 // genuine holds settle naturally. Shared zero accelerations give C2 joins.
 private static float velocity(float[][] keys,int i,int channel){
  int n=TIMES.length-1;i=(i+n)%n;
  int prev=(i+n-1)%n,next=(i+1)%n;
  float before=i==0?PERIOD-TIMES[n-1]:TIMES[i]-TIMES[i-1];
  float after=TIMES[i+1]-TIMES[i];
  float a=(keys[i][channel]-keys[prev][channel])/before;
  float b=(keys[next][channel]-keys[i][channel])/after;
  if(a*b<=0)return 0;
  float w1=2*after+before,w2=after+2*before;
  return .85F*(w1+w2)/(w1/a+w2/b);
 }
 static float sample(float time,int side,int channel){
  float[][] keys=side<0?LEFT:RIGHT;
  // Wrist orientation follows the hand's travel instead of rotating in lockstep.
  float lag=channel>=3&&channel<=5?.035F:0;
  float phase=((time-lag)%PERIOD+PERIOD)%PERIOD;
  int i=0;while(i<TIMES.length-2 && phase>=TIMES[i+1])i++;
  float h=TIMES[i+1]-TIMES[i],u=(phase-TIMES[i])/h;
  float start=keys[i][channel],d=keys[i+1][channel]-start;
  float m0=h*velocity(keys,i,channel),m1=h*velocity(keys,i+1,channel);
  float a3=10*d-6*m0-4*m1,a4=-15*d+8*m0+7*m1,a5=6*d-3*m0-3*m1;
  return start+u*(m0+u*u*(a3+u*(a4+u*a5)));
 }
 static float curl(float time,int side,int finger){
  return Math.clamp(sample(time-finger*.012F,side,8+finger),0,1);
 }
 static float fingerSpread(int side,int finger){return side*switch(finger){case 0->-16F;case 1->-2F;case 2->18F;case 3->38F;default->0F;};}
 private IllusionistGesture(){}
}
