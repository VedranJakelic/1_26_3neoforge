package net.beamex.shamaschizm.mixin;
import net.beamex.shamaschizm.entity.custom.GiantCentipedeEntity;
import net.beamex.shamaschizm.wisdom.giant.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Only bound summoned heads use this branch; ordinary centipede chains are untouched. */
@Mixin(GiantCentipedeEntity.class)
public abstract class GiantWisdomCentipedeMixin implements BroodCentipede {
    @Shadow private boolean initialized;
    @Shadow private boolean tickVerticalRoute(ServerLevel level,Player target){throw new AssertionError();}
    @Shadow private Vec3 supportNormal(){throw new AssertionError();}
    @Shadow private Vec3 surfaceDirection(Vec3 direction,Vec3 normal){throw new AssertionError();}
    @Shadow private Vec3 transitionOntoObstacle(ServerLevel level,Vec3 desired,Vec3 target,double speed){throw new AssertionError();}
    @Shadow private void moveChainPart(Vec3 desired,double speed){throw new AssertionError();}
    @Shadow private void faceDriverFromDistances(ServerLevel level,Vec3 desired){throw new AssertionError();}
    @Shadow private void tryContactAttack(ServerLevel level,Player target){throw new AssertionError();}
    @Override public void shamaschizm$initializeHead(){
        initialized=true;var self=(GiantCentipedeEntity)(Object)this;
        self.setPartKind(GiantCentipedeEntity.HEAD);self.setPartIndex(0);self.setDriver(true);
    }
    @Inject(method="tickDriver",at=@At("HEAD"),cancellable=true)
    private void shamaschizm$brood(ServerLevel level,CallbackInfo ci){
        var self=(GiantCentipedeEntity)(Object)this;if(!WisdomBrood.bound(self))return;
        var target=WisdomBrood.target(self);ci.cancel();
        if(target==null){self.setDeltaMovement(Vec3.ZERO);return;}
        self.setTarget(target);
        if(!tickVerticalRoute(level,target)){
            Vec3 to=target.getEyePosition().subtract(self.position());
            Vec3 desired=surfaceDirection(to,supportNormal()).scale(.43);
            desired=transitionOntoObstacle(level,desired,to,.43).add(supportNormal().scale(-.055));
            moveChainPart(desired,.48);faceDriverFromDistances(level,desired);
        }
        tryContactAttack(level,target);
    }
}
