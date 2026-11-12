package net.beamex.shamaschizm.mixin;
import net.beamex.shamaschizm.astral.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.level.ServerPlayer;
@Mixin(PlayerList.class)
public abstract class AstralSoundPositionMixin {
 @Redirect(method="broadcast",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerPlayer;getX()D"))
 private double astral$x(ServerPlayer p){return AstralProjection.trackingPosition(p).x;}
 @Redirect(method="broadcast",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerPlayer;getY()D"))
 private double astral$y(ServerPlayer p){return AstralProjection.trackingPosition(p).y;}
 @Redirect(method="broadcast",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerPlayer;getZ()D"))
 private double astral$z(ServerPlayer p){return AstralProjection.trackingPosition(p).z;}
}
