package net.beamex.shamaschizm.mixin;

import net.beamex.shamaschizm.grimoire.GrimoireRegistration;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Both vanilla input slots must accept an empty grimoire too. */
@Mixin(targets={"net.minecraft.world.inventory.GrindstoneMenu$2","net.minecraft.world.inventory.GrindstoneMenu$3"})
public abstract class GrimoireGrindstoneSlotMixin {
    @Inject(method="mayPlace",at=@At("HEAD"),cancellable=true)
    private void shamaschizm$allowGrimoire(ItemStack stack,CallbackInfoReturnable<Boolean> cir) {
        if(GrimoireRegistration.isBook(stack)) cir.setReturnValue(true);
    }
}
