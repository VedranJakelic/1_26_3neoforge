package net.beamex.shamaschizm.orrery;

/** Phase remains in original 30-second animation loops: ten Earth years per loop. */
public final class OrreryMotion {
    public static final double CYCLE=10, BASE_SPEED=1.0/600, DRAG=.86;
    public static final double CLICK_IMPULSE=1.0/1800, MAX_SPEED=.0125;
    public static final double EARTH_DAYS=365.256;
    // Mercury, Venus, Earth, Mars, Jupiter, Saturn, Uranus, Neptune, Pluto (NASA approximate periods).
    public static final double[] DAYS={87.969,224.701,365.256,686.980,4332.59,10759.22,30688.5,60182,90560};
    public static final double[] OFFSETS={.3,1.2,0,2.1,2.8,3.5,4.2,4.9,5.6};
    public static final double[] RADII={.50,.74,1,1.32,1.64,2.00,2.36,2.68,2.96};
    private OrreryMotion(){}
    public static double wrap(double phase){return phase-Math.floor(phase/CYCLE)*CYCLE;}
    public static double phase(double start,double speed,boolean powered,double ticks){
        ticks=Math.max(0,ticks);
        // Never reset the shared clock at Nibiru's period: that would teleport the outer planets.
        return start+(powered?BASE_SPEED*ticks:0)+speed*(1-Math.pow(DRAG,ticks))/(1-DRAG);
    }
    public static double speed(double start,double ticks){double v=start*Math.pow(DRAG,Math.max(0,ticks));return v<1e-9?0:v;}
    public static double angle(double phase,double years){double turns=phase*10/years;return (turns-Math.floor(turns))*Math.PI*2;}
    public static double planetAngle(double phase,int planet){return angle(phase,DAYS[planet]/EARTH_DAYS)+OFFSETS[planet];}
    public static double moonAngle(double phase){return angle(phase,27.322/EARTH_DAYS);}
    /** Slanted ellipse; closest pass is at years 50, 150, 250... and aligned with Earth's starting longitude. */
    public static double[] nibiru(double loops){return nibiruAtAngle(wrap(loops)*Math.PI*2/CYCLE);}
    public static double[] nibiruAtAngle(double angle){
        double x=9.5*Math.sin(angle),dz=30*(1+Math.cos(angle));
        double tilt=Math.toRadians(-22.5);
        return new double[]{x,12-Math.sin(tilt)*dz,-16+Math.cos(tilt)*dz};
    }
    public static boolean nearEarth(double loops){
        double[] n=nibiru(loops);double a=planetAngle(loops,2);
        double dx=n[0]/16+Math.sin(a),dy=n[1]/16-9.0/16,dz=n[2]/16+Math.cos(a);
        return dx*dx+dy*dy+dz*dz<=.25;
    }
}
