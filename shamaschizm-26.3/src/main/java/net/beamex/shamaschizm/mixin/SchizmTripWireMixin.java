package net.beamex.shamaschizm.mixin;

import java.util.List;
import net.beamex.shamaschizm.world.Schizm;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TripWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TripWireBlock.class)
public abstract class SchizmTripWireMixin {
    @Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
    private void shamaschizm$rejectNonPlayers(BlockState state, Level level, BlockPos pos,
                                             Entity entity, InsideBlockEffectApplier effects,
                                             boolean precise, CallbackInfo callback) {
        if (level.dimension().equals(Schizm.KEY)
                && (!(entity instanceof Player) || entity.isSpectator() || entity.isIgnoringBlockTriggers())) {
            callback.cancel();
        }
    }

    // Both initial contact and scheduled rechecks use this overload. Filtering
    // only entityInside would let mobs hold the wire on after a player leaves.
    @ModifyVariable(method = "checkPressed(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Ljava/util/List;)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private List<? extends Entity> shamaschizm$playersOnly(List<? extends Entity> entities,
                                                          Level level, BlockPos pos,
                                                          List<? extends Entity> original) {
        return level.dimension().equals(Schizm.KEY)
                ? entities.stream().filter(entity -> entity instanceof Player && !entity.isSpectator()).toList()
                : entities;
    }
}
