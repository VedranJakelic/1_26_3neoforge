package net.beamex.shamaschizm.jewelfish.client;

/** A continuous head-to-tail travelling wave; shared surface points never separate. */
public final class JewelfishMotion {
    private static double along(double z){return Math.max(0,Math.min(1,(z+1.3125)/2.625));}
    public static double offset(double z,double phase,double activity){
        double u=along(z);return (.008+.22*u*u)*activity*Math.sin(u*7.5-phase);
    }
    public static double slope(double z,double phase,double activity){
        double u=along(z);
        return activity*(.44*u*Math.sin(u*7.5-phase)+(.008+.22*u*u)*7.5*Math.cos(u*7.5-phase))/2.625;
    }
}
