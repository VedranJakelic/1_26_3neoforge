package net.beamex.shamaschizm.mixin;

import net.beamex.shamaschizm.ritual.RitualCompletionData;
import net.beamex.shamaschizm.ritual.RitualRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents automated crafting from bypassing the player result-slot checks. */
@Mixin(CrafterBlock.class)
public abstract class RitualCrafterMixin {
    @Inject(method="dispenseFrom",at=@At("HEAD"),cancellable=true)
    private void shamaschizm$requireActivatedCircle(BlockState state,ServerLevel level,BlockPos pos,CallbackInfo ci){
        if(!(level.getBlockEntity(pos) instanceof CrafterBlockEntity crafter))return;
        var input=crafter.asCraftInput();
        var recipe=CrafterBlock.getPotentialResults(level,input);
        if(recipe.isPresent()&&recipe.get().value().assemble(input).is(RitualRegistry.EGG.get())
                &&!RitualCompletionData.canCraftNear(level,pos)){
            level.levelEvent(1050,pos,0);
            ci.cancel(); // Before crafting effects, output, remainders or ingredient consumption.
        }
    }
}
