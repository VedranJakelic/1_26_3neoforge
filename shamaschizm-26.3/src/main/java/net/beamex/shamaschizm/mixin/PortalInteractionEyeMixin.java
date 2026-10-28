package net.beamex.shamaschizm.mixin;

import net.beamex.shamaschizm.localportal.PortalInteractions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Scoped virtual reach/orientation during a validated remote callback only. */
@Mixin(Entity.class)
public abstract class PortalInteractionEyeMixin {
    @Inject(method="getEyePosition()Lnet/minecraft/world/phys/Vec3;",at=@At("HEAD"),cancellable=true)
    private void portal$eye(CallbackInfoReturnable<Vec3> ci){var p=PortalInteractions.eye((Entity)(Object)this);if(p!=null)ci.setReturnValue(p);}
    @Inject(method="getYRot()F",at=@At("HEAD"),cancellable=true)
    private void portal$yaw(CallbackInfoReturnable<Float> ci){var y=PortalInteractions.yaw((Entity)(Object)this);if(y!=null)ci.setReturnValue(y);}
    @Inject(method="getXRot()F",at=@At("HEAD"),cancellable=true)
    private void portal$pitch(CallbackInfoReturnable<Float> ci){var x=PortalInteractions.pitch((Entity)(Object)this);if(x!=null)ci.setReturnValue(x);}
}
