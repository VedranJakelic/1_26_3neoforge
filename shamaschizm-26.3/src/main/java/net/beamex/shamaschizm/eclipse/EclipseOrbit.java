package net.beamex.shamaschizm.eclipse;

/** Circular inclined lunar orbit, precessing nodes, observer parallax, and projected shadow. */
public final class EclipseOrbit {
    public static final long DAY=24000, MONTH=16*DAY, EPOCH=6000, HALF_WINDOW=3000;
    public static final double RADIUS=250, DISTANCE=4096, REFERENCE_Y=64;
    public static final double INCLINATION=Math.toRadians(5.145), NODE_PERIOD=128.0*DAY;
    // Equal apparent radii at the origin. Relative angular travel across both discs takes 6,000 ticks.
    public static final double SUN_RADIUS=Math.PI*(2*HALF_WINDOW)/(2.0*MONTH);
    public static final double MOON_RADIUS=DISTANCE*Math.sin(SUN_RADIUS);
    private EclipseOrbit() {}
    public record V(double x,double y,double z) {
        public V plus(V v){return new V(x+v.x,y+v.y,z+v.z);}
        public V minus(V v){return new V(x-v.x,y-v.y,z-v.z);}
        public V times(double a){return new V(x*a,y*a,z*a);}
        public double dot(V v){return x*v.x+y*v.y+z*v.z;}
        public V cross(V v){return new V(y*v.z-z*v.y,z*v.x-x*v.z,x*v.y-y*v.x);}
        public double length(){return Math.sqrt(dot(this));}
        public V unit(){double l=length();return l>1e-12?times(1/l):new V(0,1,0);}
    }
    public record Sky(V sun,V moon,double moonRadius,double separation) {}
    public record Shadow(boolean aboveHorizon,double x,double z) {}
    public record Alignment(long lunation,long peak,double x,double z,boolean eclipse) {
        public boolean active(long time){return eclipse && time>=peak-HALF_WINDOW && time<peak+HALF_WINDOW;}
        public boolean contains(double px,double pz){return Math.hypot(px-x,pz-z)<=RADIUS;}
    }
    public static double lunarPhase(long time){return Math.floorMod(time-EPOCH,MONTH)/(double)MONTH;}
    public static double sunAngle(double time){return (time-EPOCH)%DAY*(2*Math.PI/DAY);}
    public static V sun(double time){double a=sunAngle(time);return new V(-Math.sin(a),Math.cos(a),0);}
    public static V moonPosition(double time){
        double t=time-EPOCH;
        double lambda=t%MONTH*(2*Math.PI/MONTH);
        double node=-(t%NODE_PERIOD)*(2*Math.PI/NODE_PERIOD);
        double u=lambda-node;
        double x=Math.cos(node)*Math.sin(u)*Math.cos(INCLINATION)+Math.sin(node)*Math.cos(u);
        double y=-Math.sin(node)*Math.sin(u)*Math.cos(INCLINATION)+Math.cos(node)*Math.cos(u);
        double z=Math.sin(u)*Math.sin(INCLINATION);
        double a=sunAngle(time),c=Math.cos(a),s=Math.sin(a);
        return new V(DISTANCE*(c*x-s*y),REFERENCE_Y+DISTANCE*(s*x+c*y),DISTANCE*z);
    }
    public static Sky sky(double time,double x,double y,double z){
        V s=sun(time),delta=moonPosition(time).minus(new V(x,y,z)),m=delta.unit();
        double r=Math.asin(Math.min(.999,MOON_RADIUS/Math.max(MOON_RADIUS,delta.length())));
        return new Sky(s,m,r,Math.acos(clamp(s.dot(m),-1,1)));
    }
    public static Shadow shadow(double time){
        V s=sun(time),m=moonPosition(time);
        if(s.y<=.1 || m.y<=REFERENCE_Y)return new Shadow(false,0,0);
        double travel=(m.y-REFERENCE_Y)/s.y;
        return new Shadow(true,m.x-travel*s.x,m.z-travel*s.z);
    }
    public static Alignment alignment(long n){long peak=EPOCH+n*MONTH;Shadow s=shadow(peak);
        return new Alignment(n,peak,s.x,s.z,s.aboveHorizon);}
    public static Alignment nearest(long time){return alignment(Math.floorDiv(time-EPOCH+MONTH/2,MONTH));}
    public static Alignment next(long time){long n=Math.floorDiv(time-EPOCH,MONTH);Alignment a=alignment(n);
        return a.peak+HALF_WINDOW>time?a:alignment(n+1);}
    public static boolean active(long time,double x,double z){
        if(!nearest(time).active(time))return false;
        Shadow s=shadow(time);return s.aboveHorizon && Math.hypot(x-s.x,z-s.z)<=RADIUS;
    }
    public static double darkness(long time,double x,double z){return darkness(time,x,REFERENCE_Y,z);}
    public static double darkness(double time,double x,double y,double z){
        Sky s=sky(time,x,y,z);if(s.sun.y<=0 || s.moon.y<=0)return 0;
        return overlap(SUN_RADIUS,s.moonRadius,s.separation);
    }
    /** Fraction of the solar disc hidden by the Moon; standard intersecting-circle area. */
    public static double overlap(double r,double R,double d){
        if(d>=r+R)return 0;
        if(d<=Math.abs(R-r))return Math.min(1,R*R/(r*r));
        double a=Math.acos(clamp((d*d+r*r-R*R)/(2*d*r),-1,1));
        double b=Math.acos(clamp((d*d+R*R-r*r)/(2*d*R),-1,1));
        double lens=r*r*a+R*R*b-.5*Math.sqrt(Math.max(0,(-d+r+R)*(d+r-R)*(d-r+R)*(d+r+R)));
        return clamp(lens/(Math.PI*r*r),0,1);
    }
    private static double clamp(double x,double lo,double hi){return Math.max(lo,Math.min(hi,x));}
}
