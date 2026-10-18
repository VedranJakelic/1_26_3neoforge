package net.beamex.shamaschizm.mixin;

import net.beamex.shamaschizm.entity.custom.CoffinEntity;
import net.beamex.shamaschizm.slab.SlabRegistration;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adds a sixteenth equally weighted coffin reward without replacing the coffin file. */
@Mixin(CoffinEntity.class)
public abstract class CoffinSlabLootMixin {
    @Inject(method = "randomLoot", at = @At("HEAD"), cancellable = true)
    private void shamaschizm$addSlabReward(ServerLevel level,
                                            CallbackInfoReturnable<ItemStack> cir) {
        CoffinEntity coffin = (CoffinEntity) (Object) this;
        if (coffin.getRandom().nextInt(16) == 0) {
            cir.setReturnValue(net.beamex.shamaschizm.slab.SlabAppearance.choose(new ItemStack(SlabRegistration.SLAB),coffin.getRandom()));
        }
    }
}
