package net.beamex.shamaschizm.localportal;

import java.util.List;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

/** A single aperture, with reach measured along both segments together. */
public final class PortalInteractionRay {
    public record Ray(Doorway source, Doorway target, Vec3 eye, Vec3 start, Vec3 end, double before) {}
    public static Ray trace(Player player,List<Doorway> doors,double reach) {
        if(!Double.isFinite(reach)||reach<=0)return null;
        reach=Math.min(15,reach);
        Vec3 eye=player.getEyePosition(), end=eye.add(player.getLookAngle().scale(reach));
        var crossing=DoorwayProjectileCrossing.first(doors,eye,end,0,0);
        if(crossing==null||crossing.fraction()>=1-1e-5)return null;
        Vec3 plane=eye.lerp(end,crossing.fraction());
        Vec3 aperture=crossing.source().local(plane);
        if(aperture.y<0||aperture.y>crossing.source().height()||Math.abs(aperture.x)>crossing.source().width()/2)return null;
        var obstruction=player.level().clip(new ClipContext(eye,plane,ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,player));
        if(obstruction.getType()!=HitResult.Type.MISS && eye.distanceToSqr(obstruction.getLocation())+1e-6<eye.distanceToSqr(plane))return null;
        // An entity in front of the entrance must retain ordinary interaction priority.
        for(var e:player.level().getEntities(player,new AABB(eye,plane).inflate(1),e->e.isPickable()&&!e.isSpectator())) {
            var hit=e.getBoundingBox().inflate(e.getPickRadius()).clip(eye,plane);
            if(hit.isPresent()&&eye.distanceToSqr(hit.get())+1e-6<eye.distanceToSqr(plane))return null;
        }
        var a=crossing.source();var b=crossing.target();
        Vec3 start=a.transformPoint(b,plane).add(b.facing().normal.scale(0.0001));
        return new Ray(a,b,a.transformPoint(b,eye),start,a.transformPoint(b,end),eye.distanceTo(plane));
    }
}
