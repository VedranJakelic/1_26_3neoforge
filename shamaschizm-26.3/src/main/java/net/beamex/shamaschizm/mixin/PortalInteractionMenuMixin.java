package net.beamex.shamaschizm.mixin;

import net.beamex.shamaschizm.localportal.PortalInteractions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PortalInteractionMenuMixin {
    @Inject(method="isWithinBlockInteractionRange",at=@At("HEAD"),cancellable=true)
    private void portal$menu(BlockPos pos,double padding,CallbackInfoReturnable<Boolean> ci){
        if(PortalInteractions.menuReach((Player)(Object)this,pos,padding))ci.setReturnValue(true);
    }
}
