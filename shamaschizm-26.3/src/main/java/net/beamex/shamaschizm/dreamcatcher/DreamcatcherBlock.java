package net.beamex.shamaschizm.dreamcatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HangingRootsBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
/** Vanilla underside support and waterlogging; selection outline only, no physical collision. */
public final class DreamcatcherBlock extends HangingRootsBlock {
    private static final VoxelShape OUTLINE = Block.box(4, 0, 4, 12, 16, 12);
    public DreamcatcherBlock(Properties properties) { super(properties); }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return OUTLINE;
    }
}
