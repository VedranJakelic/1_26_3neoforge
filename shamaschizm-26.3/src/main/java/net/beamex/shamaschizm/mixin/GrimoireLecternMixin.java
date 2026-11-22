package net.beamex.shamaschizm.mixin;

import net.beamex.shamaschizm.grimoire.GrimoireRegistration;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep vanilla lectern storage, saving, removal and drops; recognize the custom reader's book. */
@Mixin(LecternBlockEntity.class)
public abstract class GrimoireLecternMixin {
    @Shadow public abstract ItemStack getBook();
    @Inject(method="hasBook",at=@At("HEAD"),cancellable=true)
    private void shamaschizm$hasGrimoire(CallbackInfoReturnable<Boolean> cir){
        if(GrimoireRegistration.isBook(getBook()))cir.setReturnValue(true);
    }
    @Inject(method="getPageCount",at=@At("HEAD"),cancellable=true)
    private static void shamaschizm$grimoirePages(ItemStack stack,CallbackInfoReturnable<Integer> cir){
        // The illustrated reader paginates locally using the player's font; vanilla sees one logical book page.
        if(GrimoireRegistration.isBook(stack))cir.setReturnValue(1);
    }
}
