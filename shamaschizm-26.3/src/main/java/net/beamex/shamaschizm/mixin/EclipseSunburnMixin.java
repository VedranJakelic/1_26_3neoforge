package net.beamex.shamaschizm.mixin;
import net.beamex.shamaschizm.eclipse.Eclipses;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Mob.class)
public abstract class EclipseSunburnMixin {
    @Inject(method="isSunBurnTick",at=@At("HEAD"),cancellable=true)
    private void eclipseShade(CallbackInfoReturnable<Boolean> ci) {
        Mob mob=(Mob)(Object)this;
        if(Eclipses.active(mob.level(),mob.blockPosition())) ci.setReturnValue(false);
    }
}
