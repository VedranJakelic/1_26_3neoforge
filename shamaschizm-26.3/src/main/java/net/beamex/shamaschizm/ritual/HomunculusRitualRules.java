package net.beamex.shamaschizm.ritual;
public final class HomunculusRitualRules {
    public static final int BIRTH_TICKS=140;
    // Avoid level 7, which schedules vanilla maturation back to level 8.
    public static int compostLevel(int ticks){return Math.max(0,6-Math.max(0,ticks)/20);}
    public static boolean inside(double x,double y,double z,int cx,int cy,int cz){
        return x>=cx-3 && x<cx+4 && z>=cz-3 && z<cz+4 && y>=cy-.25 && y<=cy+2;
    }
    private HomunculusRitualRules(){}
}
