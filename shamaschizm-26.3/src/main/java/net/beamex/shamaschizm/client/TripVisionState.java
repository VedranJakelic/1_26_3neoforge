package net.beamex.shamaschizm.client;
/** Pure progression model; all times are seconds, with no camera or world mutation. */
public final class TripVisionState {
    public float charge,eyeGain,density,overall=.75F,power,age,opacity,cooldown;
    public boolean anchored,interrupted;
    static float clamp(float x,float a,float b){return Math.max(a,Math.min(b,x));}
    static float approach(float x,float t,float d){return x<t?Math.min(t,x+d):Math.max(t,x-d);}
    public static float moon(long clock){
        float day=Math.floorMod(clock,24000L);
        float night=clamp((day-12000)/2000,0,1)*clamp((24000-day)/2000,0,1);
        int phase=(int)Math.floorMod(Math.floorDiv(clock,24000L),8L);
        float darkness=(float)(.5-.5*Math.cos(phase*Math.PI/4));
        return 1+night*(-.15F+.60F*darkness);
    }
    public void update(float dt,boolean active,int amplifier,boolean schizm,float depth,
                       float moon,float darkness,boolean steady,boolean aligned){
        dt=clamp(dt,0,.1F);depth=clamp(depth,0,1);int extra=Math.max(0,Math.min(7,amplifier));
        boolean staring=active&&darkness>.65F&&steady;
        charge=approach(charge,staring?1:0,dt/(staring?35:5));
        overall=clamp(.75F+extra*.08F+(schizm?depth*.12F:0),0,1);
        power=active?(1+extra*.45F)*(schizm?1+depth*.6F:.45F)*(1+charge)*moon:0;
        float raw=(.12F+extra*.20F+charge*1.05F)*(schizm?.5F+.5F*depth:.18F)*moon;
        eyeGain=approach(eyeGain,active?(float)(1-Math.exp(-raw)):0,dt/2);
        density=approach(density,active?clamp((.06F+extra*.15F+charge*.9F)*moon,0,1):0,dt/3);
        cooldown=Math.max(0,cooldown-dt);
        if(!anchored&&cooldown<=0&&staring&&charge>.65F&&power>3.5F){anchored=true;interrupted=false;age=0;opacity=0;}
        if(!anchored)return;
        if(!active||!aligned||darkness<.4F||power<2.6F)interrupted=true;
        if(interrupted){opacity=Math.max(0,opacity-dt/2);if(opacity<=0){anchored=false;age=0;cooldown=8;}}
        else {age=Math.min(40,age+dt);opacity=Math.min(1,opacity+dt/2);}
    }
}
