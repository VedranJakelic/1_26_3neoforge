package net.beamex.shamaschizm.mixin;

import net.beamex.shamaschizm.ritual.RitualEvents;
import net.beamex.shamaschizm.ritual.RitualRegistry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Prevents obtaining the crafting result outside a completed circle; no inputs consumed. */
@Mixin(Slot.class)
public abstract class RitualResultSlotMixin {
    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void shamaschizm$checkRitualCraft(Player player, CallbackInfoReturnable<Boolean> ci) {
        Slot self = (Slot) (Object) this;
        if (!(self instanceof ResultSlot)) return;
        if (!self.getItem().is(RitualRegistry.EGG.get())) return;
        if (!RitualEvents.canCraftHere(player)) ci.setReturnValue(false);
    }
}
