package net.beamex.shamaschizm.mixin;

import java.util.List;
import java.util.Map;
import net.beamex.shamaschizm.world.Schizm;
import net.beamex.shamaschizm.world.SchizmPopulation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LocalMobCapCalculator;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalMobCapCalculator.class)
public abstract class SchizmLocalMobCapMixin {
    @Shadow @Final private Map<ServerPlayer, ?> playerMobCounts;
    @Shadow private List<ServerPlayer> getPlayersNear(ChunkPos pos) { throw new AssertionError(); }
    @Inject(method = "canSpawn", at = @At("HEAD"), cancellable = true)
    private void shamaschizm$cap(MobCategory category, ChunkPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (category != MobCategory.AMBIENT) return;
        List<ServerPlayer> players = getPlayersNear(pos);
        if (players.isEmpty() || !players.getFirst().level().dimension().equals(Schizm.KEY)) return;
        // Use vanilla's live per-player counts, including entities created earlier in this pass.
        for (ServerPlayer player : players) {
            Object counts = playerMobCounts.get(player);
            if (counts == null || ((SchizmMobCountsAccessor)counts).shamaschizm$counts().getInt(category)
                    < (net.beamex.shamaschizm.world.dungeon.DeepRoomZones.bugHabitat(player.level(), player.blockPosition())
                    ? SchizmPopulation.cap(category) : category.getMaxInstancesPerChunk())) {
                cir.setReturnValue(true);
                return;
            }
        }
        cir.setReturnValue(false);
    }
}
