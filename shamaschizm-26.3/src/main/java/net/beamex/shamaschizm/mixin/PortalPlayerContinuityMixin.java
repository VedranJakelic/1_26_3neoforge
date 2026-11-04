package net.beamex.shamaschizm.mixin;

import net.beamex.shamaschizm.localportal.client.DoorwayHandContinuity;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class PortalPlayerContinuityMixin {
    // Both hooks run after vanilla's main-thread check, around its position update.
    @Inject(method = "handleMovePlayer", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/multiplayer/ClientPacketListener;setValuesFromPositionPacket(Lnet/minecraft/world/entity/PositionMoveRotation;Ljava/util/Set;Lnet/minecraft/world/entity/Entity;Z)Z"))
    private void shamaschizm$beforePortalMove(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
        DoorwayHandContinuity.beforeTeleport();
    }
    @Inject(method = "handleMovePlayer", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/multiplayer/ClientPacketListener;setValuesFromPositionPacket(Lnet/minecraft/world/entity/PositionMoveRotation;Ljava/util/Set;Lnet/minecraft/world/entity/Entity;Z)Z", shift = At.Shift.AFTER))
    private void shamaschizm$afterPortalMove(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
        DoorwayHandContinuity.afterTeleport();
    }
}
