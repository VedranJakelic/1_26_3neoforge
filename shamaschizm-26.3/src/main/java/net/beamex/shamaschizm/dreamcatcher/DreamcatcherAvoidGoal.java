package net.beamex.shamaschizm.dreamcatcher;
import java.util.EnumSet;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.phys.Vec3;
public final class DreamcatcherAvoidGoal extends Goal {
    private final Phantom phantom;
    private final Consumer<Vec3> flightTarget;
    public DreamcatcherAvoidGoal(Phantom phantom, Consumer<Vec3> flightTarget) {
        this.phantom=phantom;
        this.flightTarget=flightTarget;
        setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK,Flag.TARGET));
    }
            private BlockPos ward;
            private int nextScan;

            @Override public boolean canUse() {
                if(phantom.tickCount<nextScan)return false;
                nextScan=phantom.tickCount+10;
                BlockPos center=phantom.blockPosition();double best=144;
                ward=null;
                for(BlockPos pos:BlockPos.betweenClosed(center.offset(-12,-12,-12),center.offset(12,12,12))) {
                    double distance=phantom.position().distanceToSqr(Vec3.atCenterOf(pos));
                    if(distance>best || !phantom.level().hasChunkAt(pos))continue;
                    if(phantom.level().getBlockState(pos).is(DreamcatcherRegistration.BLOCK)) {ward=pos.immutable();best=distance;}
                }
                return ward!=null;
            }
            @Override public boolean canContinueToUse() {
                return ward!=null && phantom.level().hasChunkAt(ward)
                        && phantom.level().getBlockState(ward).is(DreamcatcherRegistration.BLOCK)
                        && phantom.position().distanceToSqr(Vec3.atCenterOf(ward))<324;
            }
            @Override public boolean requiresUpdateEveryTick() { return true; }
            @Override public void start() { tick(); }
            @Override public void tick() {
                phantom.setTarget(null);
                Vec3 away=phantom.position().subtract(Vec3.atCenterOf(ward));
                if(away.lengthSqr()<.01)away=new Vec3(1,1,0);
                flightTarget.accept(phantom.position().add(away.normalize().scale(12)).add(0,3,0));
            }
            @Override public void stop() { ward=null; }
}
