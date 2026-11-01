package net.beamex.shamaschizm.mixin;

import net.beamex.shamaschizm.ritual.RitualEvents;
import net.beamex.shamaschizm.ritual.RitualRegistry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Cover shift-click crafting, which can otherwise bypass a result-slot pickup check. */
@Mixin(CraftingMenu.class)
public abstract class RitualCraftingMenuMixin {
    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void shamaschizm$checkQuickCraft(Player player, int slotIndex,
                                              CallbackInfoReturnable<ItemStack> ci) {
        if (slotIndex != 0) return;
        CraftingMenu menu = (CraftingMenu) (Object) this;
        if (menu.getSlot(0).getItem().is(RitualRegistry.EGG.get()) &&
                !RitualEvents.canCraftHere(player)) {
            ci.setReturnValue(ItemStack.EMPTY);
        }
    }
}
