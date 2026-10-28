package net.beamex.shamaschizm.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerPlayerGameMode.class)
public interface PortalMiningAccessor {
    @Accessor("isDestroyingBlock") boolean portal$isDestroying();
    @Accessor("destroyPos") BlockPos portal$destroyPos();
    @Accessor("gameTicks") int portal$ticks();
    @Accessor("destroyProgressStart") int portal$start();
}
