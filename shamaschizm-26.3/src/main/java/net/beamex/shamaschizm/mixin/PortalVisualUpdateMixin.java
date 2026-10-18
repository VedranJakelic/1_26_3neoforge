package net.beamex.shamaschizm.mixin;

import net.beamex.shamaschizm.event.client.PortalVisualClient;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps the connected-portal render cache current when individual blocks change. */
@Mixin(ClientLevel.class)
public abstract class PortalVisualUpdateMixin {
    @Inject(method = "sendBlockUpdated", at = @At("TAIL"))
    private void shamaschizm$portalVisualChanged(BlockPos pos, BlockState oldState,
            BlockState newState, int updateFlags, CallbackInfo callback) {
        PortalVisualClient.blockChanged((ClientLevel)(Object)this, pos, oldState, newState);
    }
}
