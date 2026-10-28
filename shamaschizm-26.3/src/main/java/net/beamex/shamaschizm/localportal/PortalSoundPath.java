package net.beamex.shamaschizm.localportal;

import net.minecraft.world.phys.Vec3;

/** One acoustic route through an upright aperture; no recursive sound propagation. */
public final class PortalSoundPath {
    public static final double LISTENER_RADIUS=24, SOUND_DEPTH=15;
    public record Path(Vec3 aperture,double distance) {}
    public static Path find(Doorway source,Doorway target,Vec3 listener,Vec3 sound,double range){
        if(!finite(listener)||!finite(sound)||!Double.isFinite(range)||range<=0
            ||!source.enabled()||!target.enabled()||!source.target().equals(target.name())||!target.target().equals(source.name()))return null;
        Vec3 ear=source.local(listener),far=target.local(sound);
        if(ear.z<0||listener.distanceToSqr(source.origin())>LISTENER_RADIUS*LISTENER_RADIUS
            ||far.z<0||far.z>SOUND_DEPTH||Math.abs(far.x)>target.width()/2+SOUND_DEPTH
            ||far.y< -SOUND_DEPTH||far.y>target.height()+SOUND_DEPTH)return null;
        // Unfold the destination into source coordinates. The straight ray gives the
        // unconstrained aperture point; clamp it to the opening when the sound is off-axis.
        Vec3 unfolded=target.transformPoint(source,sound),local=source.local(unfolded);
        double fraction=ear.z+far.z>1e-9?ear.z/(ear.z+far.z):0.5;
        double x=Math.clamp(ear.x+(local.x-ear.x)*fraction,-source.width()/2,source.width()/2);
        double y=Math.clamp(ear.y+(local.y-ear.y)*fraction,0,source.height());
        Vec3 aperture=source.world(x,y,0);
        double distance=listener.distanceTo(aperture)+sound.distanceTo(source.transformPoint(target,aperture));
        return distance<Math.min(64,range)?new Path(aperture,distance):null;
    }
    public static float gain(float volume,double distance,float range){
        if(!Float.isFinite(volume)||!Double.isFinite(distance)||!Float.isFinite(range)||range<=0)return 0;
        return Math.clamp(volume,0,1)*(float)Math.clamp(1-distance/range,0,1);
    }
    public static boolean finite(Vec3 p){return Double.isFinite(p.x)&&Double.isFinite(p.y)&&Double.isFinite(p.z);}
}
