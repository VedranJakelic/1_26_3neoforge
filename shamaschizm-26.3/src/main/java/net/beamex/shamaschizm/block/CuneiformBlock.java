package net.beamex.shamaschizm.block;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;

/** A decorative 3x3 wall. The placement position is its bottom centre. */
public final class CuneiformBlock extends HorizontalDirectionalBlock {
    public static final IntegerProperty COLUMN = IntegerProperty.create("column", 0, 2);
    public static final IntegerProperty ROW = IntegerProperty.create("row", 0, 2);

    public CuneiformBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)
                .setValue(COLUMN, 1).setValue(ROW, 0));
    }



    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, COLUMN, ROW);
    }

    private static BlockPos partPos(BlockPos anchor, Direction facing, int column, int row) {
        return anchor.relative(facing.getClockWise(), column - 1).above(row);
    }

    private static BlockPos anchorPos(BlockPos pos, BlockState state) {
        return pos.relative(state.getValue(FACING).getClockWise(), 1 - state.getValue(COLUMN))
                .below(state.getValue(ROW));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos anchor = context.getClickedPos();
        Direction facing = context.getHorizontalDirection().getOpposite();
        Player player = context.getPlayer();
        BlockState state = defaultBlockState().setValue(FACING, facing);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                BlockPos part = partPos(anchor, facing, column, row);
                if (part.getY() < level.getMinY() || part.getY() > level.getMaxY()
                        || !level.getWorldBorder().isWithinBounds(part)
                        || !level.getBlockState(part).canBeReplaced(context)
                        || !level.isUnobstructed(state, part, CollisionContext.empty())) return null;
                if (player != null && (!level.mayInteract(player, part)
                        || !player.mayUseItemAt(part, context.getClickedFace(), context.getItemInHand()))) {
                    return null;
                }
            }
        }
        return state;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()) return;
        Direction facing = state.getValue(FACING);
        // Write every part before notifying neighbours, so they see a complete wall.
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                if (row == 0 && column == 1) continue;
                level.setBlock(partPos(pos, facing, column, row),
                        state.setValue(COLUMN, column).setValue(ROW, row), Block.UPDATE_CLIENTS);
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                level.updateNeighborsAt(partPos(pos, facing, column, row), this);
            }
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos,
                                               boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        Direction facing = state.getValue(FACING);
        BlockPos anchor = anchorPos(pos, state);
        List<BlockPos> removed = new ArrayList<>();
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                BlockPos part = partPos(anchor, facing, column, row);
                if (part.equals(pos)) continue;
                BlockState existing = level.getBlockState(part);
                if (existing.is(this) && existing.getValue(FACING) == facing
                        && existing.getValue(COLUMN) == column && existing.getValue(ROW) == row) {
                    // UPDATE_CLIENTS avoids recursive removal callbacks; notify after all writes.
                    level.setBlock(part, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    removed.add(part);
                }
            }
        }
        for (BlockPos part : removed) level.updateNeighborsAt(part, this);
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        if (mirror == Mirror.NONE) return state;
        // A reflection reverses the column order as well as the facing.
        return state.rotate(mirror.getRotation(state.getValue(FACING)))
                .setValue(COLUMN, 2 - state.getValue(COLUMN));
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of();
    }
}
