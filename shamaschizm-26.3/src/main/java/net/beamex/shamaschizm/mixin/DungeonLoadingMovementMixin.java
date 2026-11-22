package net.beamex.shamaschizm.mixin;
import net.beamex.shamaschizm.loading.DungeonLoading;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class DungeonLoadingMovementMixin {
    @Shadow public ServerPlayer player;
    @Inject(method={"handleMovePlayer","handlePlayerAction","handleUseItem","handleUseItemOn","handleInteract","handleContainerClick","handleContainerButtonClick","handleSetCarriedItem"},at=@At("HEAD"),cancellable=true)
    private void shamaschizm$hold(CallbackInfo ci){
        // The first invocation may be on Netty; let vanilla schedule it before reading server state.
        if(player.level().getServer().isSameThread()&&DungeonLoading.active(player))ci.cancel();
    }
}
