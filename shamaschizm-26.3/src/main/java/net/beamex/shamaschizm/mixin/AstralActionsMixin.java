package net.beamex.shamaschizm.mixin;
import net.beamex.shamaschizm.astral.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class AstralActionsMixin {
 @Shadow public ServerPlayer player;
 @Inject(method={"handleInteract","handleAttack","handleUseItem","handleUseItemOn","handlePlayerAction","handleSetCarriedItem","handleContainerClick","handleContainerButtonClick","handleSetCreativeModeSlot","handleContainerSlotStateChanged"},at=@At("HEAD"),cancellable=true)
 private void astral$noHands(CallbackInfo ci){if(AstralProjection.active(player))ci.cancel();}
}
