package net.beamex.shamaschizm.mixin;
import net.beamex.shamaschizm.astral.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
@Mixin(targets="net.minecraft.server.level.ChunkMap$TrackedEntity")
public abstract class AstralEntityTrackingMixin {
 @Redirect(method="updatePlayer",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerPlayer;position()Lnet/minecraft/world/phys/Vec3;"))
 private Vec3 astral$view(ServerPlayer p){return AstralProjection.trackingPosition(p);}
}
