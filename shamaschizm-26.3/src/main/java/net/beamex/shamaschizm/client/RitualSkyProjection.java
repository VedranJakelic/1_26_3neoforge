package net.beamex.shamaschizm.client;

/** Projects an immutable world direction with this frame's main camera matrices.
 * Matrix storage is column-major, matching Matrix4f.get(float[]).
 * A direction has w=0: neither camera translation nor portal/HUD positions can drag it.
 */
public final class RitualSkyProjection {
    private RitualSkyProjection() {}
    public static double[] project(float[] projection, float[] view, double x, double y, double z) {
        double vx=view[0]*x+view[4]*y+view[8]*z;
        double vy=view[1]*x+view[5]*y+view[9]*z;
        double vz=view[2]*x+view[6]*y+view[10]*z;
        double vw=view[3]*x+view[7]*y+view[11]*z;
        double cx=projection[0]*vx+projection[4]*vy+projection[8]*vz+projection[12]*vw;
        double cy=projection[1]*vx+projection[5]*vy+projection[9]*vz+projection[13]*vw;
        double cw=projection[3]*vx+projection[7]*vy+projection[11]*vz+projection[15]*vw;
        if(!Double.isFinite(cx)||!Double.isFinite(cy)||!Double.isFinite(cw)||cw<=.0001)
            return new double[]{0,0,0};
        return new double[]{.5+.5*cx/cw,.5+.5*cy/cw,1};
    }
}
