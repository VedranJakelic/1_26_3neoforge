package net.beamex.shamaschizm.mixin;
import net.beamex.shamaschizm.astral.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
@Mixin(ChunkMap.class)
public abstract class AstralChunkMixin {
 @Shadow private void updateChunkTracking(ServerPlayer p){throw new AssertionError();}
 @Inject(method="move",at=@At("TAIL"))
 private void astral$refresh(ServerPlayer p,CallbackInfo ci){updateChunkTracking(p);}
 @Redirect(method="updateChunkTracking",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerPlayer;chunkPosition()Lnet/minecraft/world/level/ChunkPos;"))
 private ChunkPos astral$view(ServerPlayer p){return AstralProjection.trackingChunk(p);}
}
