package net.beamex.shamaschizm.maiden;
import org.joml.Matrix4f;
/** Shared animation for server geometry and client/portal rendering; time in ticks. */
public final class MaidenMotion {
 public static final int DORMANT=0,OPENING=1,HUNTING=2,SWALLOWING=3,DIGESTING=4,CLOSING=5;
 public static final int MIN_CYCLE=10,MAX_CYCLE=13,WAKE_TICKS=40;
 public record Pose(float left,float right,float lidPitch,float darkness,float pitch,float roll){
  public float aperture(){return Math.min(Math.abs(left),Math.abs(right))/145F;}
 }
 public static float smooth(float x){x=Math.clamp(x,0,1);return x*x*(3-2*x);}
 private static float cat(float a,float b,float c,float d,float t){return .5F*(2*b+(-a+c)*t+(2*a-5*b+4*c-d)*t*t+(-a+3*b-3*c+d)*t*t*t);}
 private static float open(float age,float middle,float end){if(age<17.5F)return cat(0,0,middle,end,Math.clamp(age/17.5F,0,1));if(age<25)return cat(0,middle,end,end,(age-17.5F)/7.5F);return end;}
 public static float flap(float t){if(t<.12F)return 1;if(t<.28F)return 1-smooth((t-.12F)/.16F);if(t<.42F)return 0;if(t<.72F)return smooth((t-.42F)/.30F);return 1;}
 public static Pose pose(int state,float age,int cycle,int variant){
  age=Math.max(0,age);
  if(state==DORMANT)return new Pose(0,0,0,1,0,0);
  if(state==OPENING){age*=65F/WAKE_TICKS;float dark=age<25?1:age<30?1-.0749F*(age-25)/5:age<40?.9251F:cat(.9251F,.9251F,.4F,.4F,Math.clamp((age-40)/23.3334F,0,1));return new Pose(open(age,-135,-145),open(age,130,145),open(age,-4,-5),dark,0,0);}
  if(state==HUNTING){float t=Math.clamp(age/Math.max(1,cycle),0,1),l=flap(t),r=flap(Math.clamp(t+(variant%3-1)*.018F,0,1));float pitch=t<.12F?4*smooth(t/.12F):t<.28F?4-15*smooth((t-.12F)/.16F):t<.8F?-11*(1-smooth((t-.28F)/.52F)):0;return new Pose(-145*l,145*r,-5*Math.min(l,r),.4F,pitch,(variant%2==0?1:-1)*4*(float)Math.sin(Math.PI*t));}
  if(state==DIGESTING){float t=age%58;float shake=t<19?(float)Math.sin(t*1.15)*7*(float)Math.sin(Math.PI*t/19):0;return new Pose(0,0,0,.4F,shake*.3F,shake);}
  float f=1-smooth(age/(state==SWALLOWING?7:22));return new Pose(-145*f,145*f,-5*f,.4F+.6F*(state==CLOSING?1-f:0),0,0);
 }
 public static Matrix4f[] matrices(float yaw,Pose p){
  Matrix4f root=new Matrix4f().rotateY((float)Math.toRadians(180-yaw)).rotateZ((float)Math.toRadians(p.roll)).rotateX((float)Math.toRadians(p.pitch));
  Matrix4f[] m={new Matrix4f(root),new Matrix4f(root),new Matrix4f(root),new Matrix4f(root),new Matrix4f(root)};
  m[1].translate(0,0,.25F).scale(1,1,p.darkness).translate(0,0,-.25F);
  hinge(m[3],.5F,2.50238F+p.lidPitch,-2.49762F+p.left,-.10912F);hinge(m[4],-.5F,2.50238F+p.lidPitch,2.49762F+p.right,.10912F);return m;
 }
 private static void hinge(Matrix4f m,float x,float rx,float ry,float rz){m.translate(x,19F/16,-5F/16).rotateZ((float)Math.toRadians(rz)).rotateY((float)Math.toRadians(ry)).rotateX((float)Math.toRadians(rx)).translate(-x,-19F/16,5F/16);}
}
