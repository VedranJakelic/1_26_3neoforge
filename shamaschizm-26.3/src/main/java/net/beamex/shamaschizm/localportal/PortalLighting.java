package net.beamex.shamaschizm.localportal;

import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.phys.Vec3;

/** Keep destination shading except for the local aperture transition. */
public final class PortalLighting {
    private PortalLighting() {}

    public static CardinalLighting oriented(CardinalLighting base, Doorway source, Doorway target) {
        return base;
    }

    /** Source-facing shading, used only for vertices at the aperture. */
    public static CardinalLighting atAperture(CardinalLighting base, Doorway source, Doorway target) {
        return new CardinalLighting(base.down(),base.up(),
                face(base,target.transformVector(source,new Vec3(0,0,-1))),
                face(base,target.transformVector(source,new Vec3(0,0,1))),
                face(base,target.transformVector(source,new Vec3(-1,0,0))),
                face(base,target.transformVector(source,new Vec3(1,0,0))));
    }

    private static float face(CardinalLighting base, Vec3 normal) {
        if(normal.x>.5)return base.east();
        if(normal.x<-.5)return base.west();
        return normal.z>.5?base.south():base.north();
    }

    private static double destinationWeight(double depth) {
        double t=Math.max(0.0,Math.min(1.0,depth/2.0));
        return t*t*(3.0-2.0*t);
    }

    public static int boundary(int source,int destination,double depth) {
        return (int)Math.round(source+(destination-source)*destinationWeight(depth));
    }

    /** Interpolate vanilla's final vertex shading without multiplying tint twice. */
    public static int vertexColor(int source,int destination,double depth) {
        double t=destinationWeight(depth);
        if(t<=0)return source;
        if(t>=1)return destination;
        int result=0;
        for(int shift=0;shift<=24;shift+=8) {
            int a=(source>>>shift)&255,b=(destination>>>shift)&255;
            result|=((int)Math.round(a+(b-a)*t))<<shift;
        }
        return result;
    }
}
