package net.beamex.shamaschizm.client;
import net.beamex.shamaschizm.effect.ModEffects;
import net.beamex.shamaschizm.world.Schizm;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
/** Five world-light samples in a small viewing cone, independent of overlays/HUD. */
public final class TripVisionClient {
    private static TripVisionState state=new TripVisionState();
    private static Vec3 anchor,previousLook,previousPosition;
    private static float anchorAge;
    private static Vec3 lastProjection=new Vec3(.5,.5,1);
    private static int ritual; // 0: original ritual; 1: Homo Nucleus. Stable until the next revelation.
    private TripVisionClient(){}
    public static void reset(){state=new TripVisionState();anchor=null;previousLook=null;previousPosition=null;ritual=0;anchorAge=0;lastProjection=new Vec3(.5,.5,1);}
    public static float eyeGain(){return state.eyeGain;}
    public static float density(){return state.density;}
    public static float overall(){return state.overall;}
    public static float anchorAge(){return anchorAge;}
    public static int ritual(){return ritual;}
    public static float age(){return state.age;}
    public static float opacity(){return state.opacity;}
    public static void tick(){
        if(net.beamex.shamaschizm.astral.AstralClient.active()){reset();return;}
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null){reset();return;}
        if(mc.isPaused())return;
        var camera=mc.gameRenderer.mainCamera();Vec3 pos=camera.position(),look=new Vec3(camera.forwardVector()).normalize();
        if(previousPosition!=null&&pos.distanceToSqr(previousPosition)>16)reset();
        var effect=mc.player.getEffect(ModEffects.TRIPPING);
        boolean active=mc.player.isAlive()&&effect!=null&&mc.getCameraEntity()==mc.player;
        boolean steady=previousLook==null||look.dot(previousLook)>.999;
        float darkness=0;
        if(active){
            Vec3 right=look.cross(new Vec3(0,1,0));right=right.lengthSqr()<.001?new Vec3(1,0,0):right.normalize();
            Vec3 up=right.cross(look).normalize();
            for(int i=0;i<5;i++){
                Vec3 ray=look.add(right.scale(i==1?.055:i==2?-.055:0)).add(up.scale(i==3?.055:i==4?-.055:0)).normalize();
                var hit=mc.level.clip(new ClipContext(pos,pos.add(ray.scale(12)),ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,mc.player));
                Vec3 sample=hit.getLocation().subtract(ray.scale(.15));
                darkness+=1-Mth.clamp(mc.level.getMaxLocalRawBrightness(BlockPos.containing(sample))/12F,0,1);
            }
            darkness/=5;
        }
        boolean was=state.anchored,aligned=!was||anchor==null||look.dot(anchor)>.9781476; // 12 degrees
        float depth=Mth.clamp((160F-(float)pos.y)/100F,0,1);
        state.update(.05F,active,effect==null?0:effect.getAmplifier(),mc.level.dimension().equals(Schizm.KEY),depth,
                     TripVisionState.moon(mc.level.getOverworldClockTime()),darkness,steady,aligned);
        if(!was&&state.anchored){
            anchor=look;anchorAge=0;lastProjection=new Vec3(.5,.5,1);
            ritual=java.util.concurrent.ThreadLocalRandom.current().nextInt(2);
        }
        // Keep the world direction while the apparition fades. Once it is gone,
        // the shader has fully returned to its independent normal-fractal layer.
        if(state.anchored) {
            if(!state.interrupted)anchorAge=Mth.clamp(anchorAge+.05F,0F,6F);
        }
        else {anchor=null;anchorAge=0;lastProjection=new Vec3(.5,.5,1);}
        previousLook=look;previousPosition=pos;
    }
    /** Fixed world direction, not a forced camera rotation. */
    public static Vec3 screenAnchor(){
        if(anchor==null)return new Vec3(.5,.5,1);
        Vec3 projected=SurfaceEyeEffects.projectDirection(anchor);
        if(projected.z>0 && Double.isFinite(projected.x) && Double.isFinite(projected.y)) {
            // Do not clamp a far-away center into a visible frozen position.
            if(Math.abs(projected.x-.5)>8 || Math.abs(projected.y-.5)>8)
                return new Vec3(lastProjection.x,lastProjection.y,0);
            lastProjection=projected;
            return lastProjection;
        }
        // Behind the camera or no valid world projection: hide the apparition, never recenter it.
        return new Vec3(lastProjection.x,lastProjection.y,0);
    }
}
