package net.beamex.shamaschizm.ascent;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

/** Invisible, replaceable, self-expiring source for server-side block lighting. */
public final class AscentLightBlock extends LightBlock {
    public AscentLightBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(LEVEL, AscentGlow.LIGHT_LEVEL));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState,
                           boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide()) level.scheduleTick(pos, this, AscentGlow.UPDATE_TICKS);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Compare against the chosen source cell, not the wearer's feet. Keep a
        // four-block overlap behind and below a moving wearer. This gives the
        // new source time to propagate before the previous source disappears,
        // including during diagonal sprinting and jumping over snow layers.
        boolean occupied = !level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(5),
                entity -> {
                    if (!AscentGlow.isWearingGlow(entity)) return false;
                    BlockPos preferred = AscentGlow.sourcePosition(entity);
                    if (preferred == null) return false;
                    long dx = (long) preferred.getX() - pos.getX();
                    long dy = (long) preferred.getY() - pos.getY();
                    long dz = (long) preferred.getZ() - pos.getZ();
                    return dx * dx + dy * dy + dz * dz <= 16;
                }).isEmpty();
        if (occupied) {
            level.scheduleTick(pos, this, AscentGlow.UPDATE_TICKS);
        } else {
            // The WATERLOGGED bit remembers source water; no terrain is lost on cleanup.
            level.setBlock(pos, state.getValue(WATERLOGGED)
                    ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }
}
