package net.beamex.shamaschizm.block;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Four facing directions, floor or wall attachment, and two or four linked cloth sections. */
public final class HangingBannerBlock extends HorizontalDirectionalBlock {
    public static final IntegerProperty ROW = IntegerProperty.create("row", 0, 3);
    public static final BooleanProperty WALL = BooleanProperty.create("wall");
    private static final VoxelShape FLOOR_OUTLINE = Block.box(0, 0, 7, 16, 16, 9);
    private static final VoxelShape WALL_NORTH = Block.box(0, 0, 13, 16, 16, 15);
    private static final VoxelShape WALL_SOUTH = Block.box(0, 0, 1, 16, 16, 3);
    private static final VoxelShape WALL_EAST = Block.box(1, 0, 0, 3, 16, 16);
    private static final VoxelShape WALL_WEST = Block.box(13, 0, 0, 15, 16, 16);
    private final int height;

    public HangingBannerBlock(Properties properties, int height) {
        super(properties);
        this.height = height;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)
                .setValue(WALL, false).setValue(ROW, 0));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WALL, ROW);
    }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level,
                                            BlockPos pos, CollisionContext context) {
        if (!state.getValue(WALL)) return FLOOR_OUTLINE;
        return switch (state.getValue(FACING)) {
            case NORTH -> WALL_NORTH;
            case SOUTH -> WALL_SOUTH;
            case EAST -> WALL_EAST;
            case WEST -> WALL_WEST;
            default -> FLOOR_OUTLINE;
        };
    }
    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level,
                                                     BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        boolean wall = context.getClickedFace().getAxis().isHorizontal();
        Direction facing = wall ? context.getClickedFace() : context.getHorizontalDirection().getOpposite();
        BlockPos placed = context.getClickedPos();
        BlockPos top = wall ? placed : placed.above(height - 1);
        BlockState state = defaultBlockState().setValue(FACING, facing).setValue(WALL, wall);
        Player player = context.getPlayer();
        for (int row = 0; row < height; row++) {
            BlockPos part = top.below(row);
            if (part.getY() < level.getMinY() || part.getY() > level.getMaxY()
                    || !level.getWorldBorder().isWithinBounds(part)
                    || !level.getBlockState(part).canBeReplaced(context)
                    || !level.isUnobstructed(state, part, CollisionContext.empty())) return null;
            if (player != null && (!level.mayInteract(player, part)
                    || !player.mayUseItemAt(part, context.getClickedFace(), context.getItemInHand()))) return null;
        }
        if (wall) {
            BlockPos support = top.relative(facing.getOpposite());
            if (!level.getBlockState(support).isFaceSturdy(level, support, facing)) return null;
        } else {
            BlockPos support = placed.below();
            if (!level.getBlockState(support).isFaceSturdy(level, support, Direction.UP)) return null;
        }
        return state.setValue(ROW, wall ? 0 : height - 1);
    }

    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                                      LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()) return;
        BlockPos top = pos.above(state.getValue(ROW));
        for (int row = 0; row < height; row++) {
            BlockPos part = top.below(row);
            if (!part.equals(pos)) level.setBlock(part, state.setValue(ROW, row), Block.UPDATE_CLIENTS);
        }
        for (int row = 0; row < height; row++) level.updateNeighborsAt(top.below(row), this);
    }

    @Override protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        int row = state.getValue(ROW);
        boolean wall = state.getValue(WALL);
        if (row < 0 || row >= height) return false;
        BlockPos top = pos.above(row);
        BlockPos support = wall ? top.relative(state.getValue(FACING).getOpposite()) : top.below(height);
        Direction side = wall ? state.getValue(FACING) : Direction.UP;
        // BlockItem checks this before setPlacedBy creates the other sections.
        // Requiring neighboring banner blocks here makes every placement fail.
        return level.getBlockState(support).isFaceSturdy(level, support, side);
    }
    private boolean partMatches(LevelReader level, BlockPos pos, BlockState state, int row) {
        BlockState other = level.getBlockState(pos);
        return other.is(this) && other.getValue(ROW) == row
                && other.getValue(WALL) == state.getValue(WALL)
                && other.getValue(FACING) == state.getValue(FACING);
    }
    @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
                                               BlockPos pos, Direction direction, BlockPos neighborPos,
                                               BlockState neighborState, RandomSource random) {
        return canSurvive(state, level, pos)
                ? super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random)
                : Blocks.AIR.defaultBlockState();
    }

    @Override protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level,
                                                           BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        BlockPos top = pos.above(state.getValue(ROW));
        for (int row = 0; row < height; row++) {
            BlockPos part = top.below(row);
            if (!part.equals(pos) && partMatches(level, part, state, row)) {
                level.setBlock(part, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                level.updateNeighborsAt(part, this);
            }
        }
    }
    @Override protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(new ItemStack(this.asItem()));
    }
}
