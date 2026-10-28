package net.beamex.shamaschizm.mixin;

import net.beamex.shamaschizm.localportal.PortalInteractionPacket.Action;
import net.beamex.shamaschizm.localportal.client.PortalInteractionClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(Minecraft.class)
public abstract class PortalInteractionInputMixin {
    @Inject(method="startAttack",at=@At("HEAD"),cancellable=true)
    private void portal$attack(CallbackInfoReturnable<Boolean> ci){if(PortalInteractionClient.input(Action.ATTACK,true))ci.setReturnValue(false);}
    @Inject(method="continueAttack",at=@At("HEAD"),cancellable=true)
    private void portal$hold(boolean down,CallbackInfo ci){if(PortalInteractionClient.input(Action.HOLD,down))ci.cancel();}
    @Inject(method="startUseItem",at=@At("HEAD"),cancellable=true)
    private void portal$use(CallbackInfo ci){if(PortalInteractionClient.input(Action.USE,true))ci.cancel();}
}
