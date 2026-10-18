package net.beamex.shamaschizm.mixin;

import net.beamex.shamaschizm.event.CiglunProvocationEvents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Detects real item removal from chest-style container slots. */
@Mixin(Slot.class)
public abstract class CiglunLootSlotMixin {
    @Shadow @Final public Container container;

    @Inject(method = "onTake", at = @At("TAIL"))
    private void shamaschizm$noticeLooting(Player player, ItemStack taken,
                                           CallbackInfo callback) {
        if (!taken.isEmpty()) {
            CiglunProvocationEvents.playerLootedContainer(player, this.container);
        }
    }
}
