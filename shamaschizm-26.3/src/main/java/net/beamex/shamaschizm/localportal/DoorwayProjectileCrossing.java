package net.beamex.shamaschizm.localportal;

import java.util.List;
import net.minecraft.world.phys.Vec3;

/** Orders portal intersections along a shot, before a possible physical impact. */
public final class DoorwayProjectileCrossing {
    private DoorwayProjectileCrossing() {}
    public record Crossing(Doorway source,Doorway target,double fraction) {}
    public static Crossing first(List<Doorway> doors,Vec3 before,Vec3 end,double width,double height) {
        double length=before.distanceToSqr(end);
        if(length<1e-12||length>16*16)return null;
        Crossing first=null;
        for(var source:doors) {
            if(!source.enabled()||!source.crossed(before,end,width,height))continue;
            Doorway target=null;
            for(var door:doors)if(door.name().equals(source.target())){target=door;break;}
            if(target==null||!target.enabled()||!target.target().equals(source.name()))continue;
            double a=source.distance(before),b=source.distance(end),fraction=a/(a-b);
            if(first==null||fraction<first.fraction())first=new Crossing(source,target,fraction);
        }
        return first;
    }
}
