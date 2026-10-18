package net.beamex.shamaschizm.building;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;

/** Compatibility for webs already placed by the earlier block-tile patch. New webs are entities. */
public final class SpanningWebBlock extends WebBlock implements EntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty FLIPPED = BooleanProperty.create("flipped");
    public static final IntegerProperty TILE = IntegerProperty.create("tile", 0, 224);
    public static final BooleanProperty LINKED = BooleanProperty.create("linked");
    public static final BooleanProperty MIRRORED = BooleanProperty.create("mirrored");
    private static final ThreadLocal<Boolean> REMOVING = ThreadLocal.withInitial(() -> false);
    public SpanningWebBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FLIPPED, false).setValue(TILE, 0).setValue(LINKED,false).setValue(MIRRORED,false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, FLIPPED, TILE, LINKED, MIRRORED); }
    private static VoxelShape shape(BlockState state) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? Block.box(0, 0, 7.5, 16, 16, 8.5)
                : Block.box(7.5, 0, 0, 8.5, 16, 16);
    }
    @Override public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new WebBlockEntity(pos,state);}
    @Override protected RenderShape getRenderShape(BlockState state){return state.getValue(LINKED)?RenderShape.INVISIBLE:RenderShape.MODEL;}
    private VoxelShape linkedShape(BlockState state,BlockGetter level,BlockPos pos){
        if(!state.getValue(LINKED))return shape(state);
        return level.getBlockEntity(pos) instanceof WebBlockEntity be?be.shape():Shapes.empty();
    }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return linkedShape(state,level,pos); }
    @Override protected VoxelShape getEntityInsideCollisionShape(BlockState state, BlockGetter level, BlockPos pos, Entity entity) { return linkedShape(state,level,pos); }
    @Override protected BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override protected BlockState mirror(BlockState state, Mirror mirror) {
        if(!state.getValue(LINKED))return state.rotate(mirror.getRotation(state.getValue(FACING)));
        if(mirror==Mirror.NONE)return state;
        int angle=switch(state.getValue(FACING)){case EAST->1;case SOUTH->2;case WEST->3;default->0;};
        int next=Math.floorMod((mirror==Mirror.LEFT_RIGHT?2:0)-angle,4);
        Direction[] directions={Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST};
        return state.setValue(FACING,directions[next]).cycle(MIRRORED);
    }
    @Override protected boolean isRandomlyTicking(BlockState state){return !state.getValue(LINKED);}
    @Override protected BlockState updateShape(BlockState state,LevelReader level,ScheduledTickAccess ticks,BlockPos pos,
            Direction direction,BlockPos neighborPos,BlockState neighborState,net.minecraft.util.RandomSource random){
        if(!WebBlocks.changing() && neighborState.isAir() && state.getValue(LINKED) && level instanceof ServerLevel server
                && level.getBlockEntity(pos) instanceof WebBlockEntity be && be.configured())
            server.scheduleTick(be.owner(),this,2);
        return state;
    }
    @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,net.minecraft.util.RandomSource random){
        if(level.getBlockEntity(pos) instanceof WebBlockEntity be)WebBlocks.repair(level,be);
    }
    @Override protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) { return new ItemStack(BuildingRegistration.WEB_ITEM); }
    @Override protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return Blocks.COBWEB.defaultBlockState().getDestroyProgress(player, level, pos);
    }
    @Override public boolean canHarvestBlock(BlockState state, BlockGetter level, BlockPos pos, Player player) {
        return net.neoforged.neoforge.event.EventHooks.doPlayerHarvestCheck(player, Blocks.COBWEB.defaultBlockState(), level, pos);
    }
    @Override protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
        // Upgrade loaded old tile webs without touching any intervening solid blocks.
        int[] size = layout(state.getValue(TILE));
        Direction along = switch (state.getValue(FACING)) { case EAST -> Direction.SOUTH; case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH; default -> Direction.EAST; };
        BlockPos origin = pos.relative(along, -size[2]).below(size[3]);
        for (int row = 0; row < size[1]; row++) for (int col = 0; col < size[0]; col++)
            if (!level.hasChunkAt(origin.relative(along, col).above(row))) return;
        var center = new net.minecraft.world.phys.Vec3(origin.getX() + .5 + along.getStepX() * (size[0] - 1) / 2.0,
                origin.getY(), origin.getZ() + .5 + along.getStepZ() * (size[0] - 1) / 2.0);
        boolean flip = state.getValue(FLIPPED);
        var rectangle = new WebRectangle(center,size[0],size[1],Math.toDegrees(Math.atan2(along.getStepZ(),along.getStepX())));
        WebBlocks.install(level,rectangle,flip);
    }
    @Override protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean piston) {
        super.affectNeighborsAfterRemoval(state, level, pos, piston);
        if (REMOVING.get() || WebBlocks.changing() || state.getValue(LINKED)) return;
        int[] layout = layout(state.getValue(TILE));
        Direction facing = state.getValue(FACING);
        Direction along = switch (facing) { case EAST -> Direction.SOUTH; case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH; default -> Direction.EAST; };
        BlockPos origin = pos.relative(along, -layout[2]).below(layout[3]);
        REMOVING.set(true);
        try {
            for (int row = 0; row < layout[1]; row++) for (int col = 0; col < layout[0]; col++) {
                BlockPos other = origin.relative(along, col).above(row);
                BlockState found = level.getBlockState(other);
                if (found.is(this) && found.getValue(FACING) == facing && found.getValue(FLIPPED).equals(state.getValue(FLIPPED))
                        && found.getValue(TILE) == tile(layout[0], layout[1], col, row))
                    level.removeBlock(other, false);
            }
        } finally { REMOVING.remove(); }
    }
    public static int tile(int width, int height, int column, int row) {
        int index = 0;
        for (int w = 1; w <= 5; w++) for (int h = 1; h <= 5; h++) {
            if (w == width && h == height) return index + row * width + column;
            index += w * h;
        }
        throw new IllegalArgumentException("Web dimensions must be 1..5");
    }
    private static int[] layout(int tile) {
        int index = 0;
        for (int w = 1; w <= 5; w++) for (int h = 1; h <= 5; h++) {
            if (tile < index + w * h) return new int[]{w, h, (tile - index) % w, (tile - index) / w};
            index += w * h;
        }
        throw new IllegalArgumentException("Invalid web tile");
    }
}
