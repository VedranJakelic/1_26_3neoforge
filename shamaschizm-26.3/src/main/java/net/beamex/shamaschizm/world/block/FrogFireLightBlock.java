package net.beamex.shamaschizm.world.block;

import net.beamex.shamaschizm.entity.custom.FrogGodEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Invisible low-level light which removes itself when no open-mouthed frog remains nearby. */
public final class FrogFireLightBlock extends LightBlock {
    public FrogFireLightBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(LEVEL, 6));
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean active = !level.getEntitiesOfClass(FrogGodEntity.class,
                new AABB(pos).inflate(2.0D), FrogGodEntity::isFireAttacking).isEmpty();
        if (active) {
            level.scheduleTick(pos, this, 5);
        } else {
            level.setBlock(pos, state.getValue(WATERLOGGED)
                    ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }
}
