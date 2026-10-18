package net.beamex.shamaschizm.saints.vindication;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
/** Shared orientation for rendering and swept projectile collision. The spell ring rotates 1.6 times as fast as the bone ring. */
public final class RingGeometry {
 public static Quaternionf rotation(float ticks){return new Quaternionf().rotationXYZ(ticks*.014F,ticks*.020F,ticks*.011F);}
 public static Quaternionf spellRotation(float ticks){return rotation(ticks*1.6F);}
 public static Quaternionf outerRotation(float ticks){return rotation(ticks*.65F);}
 public static Vec3 local(Vec3 p,float ticks){var v=new Vector3f((float)p.x,(float)p.y,(float)p.z);rotation(ticks).conjugate().transform(v);return new Vec3(v.x,v.y,v.z);}
 public static int ringAt(Vec3 world,float ticks){
  Vec3 bone=local(world,ticks),spell=local(world,ticks*1.6F);
  if(Math.pow(Math.hypot(bone.y,bone.z)-1.55,2)+bone.x*bone.x<.32*.32)return 0;
  if(Math.pow(Math.hypot(spell.x,spell.z)-1.18,2)+spell.y*spell.y<.13*.13)return 1;
  Vec3 outer=local(world,ticks*.65F);
  if(Math.pow(Math.hypot(outer.y,outer.z)-2.6,2)+outer.x*outer.x<.38*.38)return 2;
  return -1;
 }
 public static boolean shell(Vec3 world,float ticks){return ringAt(world,ticks)>=0;}
 public static Vec3 hit(Vec3 from,Vec3 to,Vec3 center,float ticks){
  double length=from.distanceTo(to);int steps=Math.max(1,(int)Math.ceil(length/.04));
  // Clip very long segments to the small region around the boss before sampling.
  var box=new net.minecraft.world.phys.AABB(center.x-3.1,center.y-3.1,center.z-3.1,center.x+3.1,center.y+3.1,center.z+3.1);
  Vec3 start=box.contains(from)?from:box.clip(from,to).orElse(null);if(start==null)return null;
  double begin=length<1e-8?0:from.distanceTo(start)/length;
  for(double t=begin;t<=1;t+=1.0/steps){Vec3 point=from.lerp(to,t);if(!box.inflate(.05).contains(point))break;if(shell(point.subtract(center),ticks))return point;}
  return null;
 }
}
