package net.beamex.shamaschizm.ascent;

import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class AscentLightEvents {
    private AscentLightEvents() {}

    @SubscribeEvent
    public static void afterEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity entity)
                || !(entity.level() instanceof ServerLevel level)
                || entity.tickCount % AscentGlow.UPDATE_TICKS != 0
                || !AscentGlow.isWearingGlow(entity)) return;
        BlockPos pos = AscentGlow.sourcePosition(entity);
        if (pos == null || !level.getWorldBorder().isWithinBounds(pos)
                || pos.getY() < level.getMinY() || pos.getY() > level.getMaxY()) return;
        BlockState existing = level.getBlockState(pos);
        if (!existing.is(AscentBootstrap.LIGHT)) {
            BlockState light = AscentBootstrap.LIGHT.defaultBlockState()
                    .setValue(LightBlock.WATERLOGGED, !existing.getFluidState().isEmpty());
            level.setBlock(pos, light, Block.UPDATE_CLIENTS);
        }
        // Scheduling again also repairs missing schedules after external block edits.
        level.scheduleTick(pos, AscentBootstrap.LIGHT, AscentGlow.UPDATE_TICKS);
    }
}
