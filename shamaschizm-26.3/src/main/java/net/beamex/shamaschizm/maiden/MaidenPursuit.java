package net.beamex.shamaschizm.maiden;

/** Pure movement/capture geometry, shared by the mob and regression checks. */
public final class MaidenPursuit {
    private MaidenPursuit() {}
    public static double hopSpeed(double distance) {
        return Math.max(.12, Math.min(.43, (distance - .65) * .24));
    }
    /** Sweep a player's body through the front opening, in the maiden's moving frame. */
    public static boolean crossesMouth(float yaw, double ax, double ay, double az,
            double bx, double by, double bz, double halfWidth, double halfHeight) {
        double angle = Math.toRadians(yaw - 180), c = Math.cos(angle), s = Math.sin(angle);
        double width = halfWidth * (Math.abs(c) + Math.abs(s));
        double[] a = {ax*c + az*s, ay, az*c - ax*s};
        double[] b = {bx*c + bz*s, by, bz*c - bx*s};
        double[] lo = {-.52-width, .25-halfHeight, -.95-width};
        double[] hi = {.52+width, 1.95+halfHeight, -.20+width};
        // Require the player's center to remain in front: no captures through the back shell.
        hi[2] = Math.min(hi[2], -.10);
        double enter = 0, leave = 1;
        for (int i=0; i<3; i++) {
            double d = b[i]-a[i];
            if (Math.abs(d)<1e-9) { if(a[i]<lo[i]||a[i]>hi[i])return false; }
            else {
                double x=(lo[i]-a[i])/d, y=(hi[i]-a[i])/d;
                enter=Math.max(enter, Math.min(x,y)); leave=Math.min(leave, Math.max(x,y));
                if(enter>leave)return false;
            }
        }
        return true;
    }
}
