package net.beamex.shamaschizm.wisdom.client;

/** Tick-driven envelope: six seconds to build; exactly three seconds to release from any level. */
public final class WisdomExposure {
    private float amount, releaseFrom, releaseTime;
    private boolean looking;
    public void reset(){amount=releaseFrom=releaseTime=0;looking=false;}
    public void tick(boolean gaze,float seconds){
        if(!Float.isFinite(seconds)||seconds<=0)return;
        if(gaze){amount=Math.min(1,amount+seconds/6);releaseTime=0;}
        else {
            if(looking){releaseFrom=amount;releaseTime=0;}
            releaseTime=Math.min(3,releaseTime+seconds);
            amount=releaseTime>=2.9999F?0:releaseFrom*(1-smooth(releaseTime/3));
        }
        looking=gaze;
    }
    private static float smooth(float x){return x*x*(3-2*x);}
    public float amount(){return amount;}
    /** Preserve the spatial lens response, adding up to 10% after sustained gaze. */
    public float lensStrength(float base){
        float spatial=Float.isFinite(base)?Math.max(0,Math.min(1,base)):0;
        return spatial*(1+.40F*smooth(amount));
    }
    public float soundGain(){return 1-.96F*smooth(amount);}
    public float visual(float proximity){
        float p=Float.isFinite(proximity)?Math.max(0,Math.min(1,proximity)):0;
        float ambient=.55F*smooth(p);
        return ambient+(1-ambient)*amount;
    }
}
