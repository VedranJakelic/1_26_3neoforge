package net.beamex.shamaschizm.mixin;

import net.beamex.shamaschizm.world.Schizm;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Covers stone, polished blackstone, wooden and weighted plates in the Schizm. */
@Mixin(BasePressurePlateBlock.class)
public abstract class SchizmPressurePlateMixin {
    @Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
    private void shamaschizm$rejectNonPlayers(BlockState state, Level level, BlockPos pos,
                                             Entity entity, InsideBlockEffectApplier effects,
                                             boolean precise, CallbackInfo callback) {
        if (level.dimension().equals(Schizm.KEY)
                && (!(entity instanceof Player) || entity.isSpectator() || entity.isIgnoringBlockTriggers())) {
            callback.cancel();
        }
    }

    // All vanilla plate types share this query, including their scheduled
    // rechecks. Keep the original AABB so detection follows vanilla exactly.
    @Inject(method = "getEntityCount", at = @At("HEAD"), cancellable = true)
    private static void shamaschizm$countPlayers(Level level, AABB box,
                                                 Class<? extends Entity> entityClass,
                                                 CallbackInfoReturnable<Integer> callback) {
        if (level.dimension().equals(Schizm.KEY)) {
            callback.setReturnValue(level.getEntitiesOfClass(Player.class, box,
                    EntitySelector.NO_SPECTATORS.and(entity -> !entity.isIgnoringBlockTriggers())).size());
        }
    }
}
