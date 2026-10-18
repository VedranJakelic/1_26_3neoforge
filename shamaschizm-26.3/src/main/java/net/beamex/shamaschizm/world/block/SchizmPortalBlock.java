package net.beamex.shamaschizm.world.block;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import net.beamex.shamaschizm.registry.ModBlocks;
import net.beamex.shamaschizm.world.PortalAtmosphereData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Thin, non-colliding portal pane with ambient sound/particles and server teleport hook. */
public class SchizmPortalBlock extends Block {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    private static final VoxelShape SHAPE_X = Block.box(0, 0, 6, 16, 16, 10); // 4px thick plane
    private static final VoxelShape SHAPE_Z = Block.box(6, 0, 0, 10, 16, 16);
    private static final int QUIET_AXIS_FLAGS = Block.UPDATE_CLIENTS
            | Block.UPDATE_KNOWN_SHAPE
            | Block.UPDATE_SKIP_SHAPE_UPDATE_ON_WIRE
            | Block.UPDATE_SKIP_ON_PLACE;

    public SchizmPortalBlock(BlockBehaviour.Properties props) {
        super(props.noCollision().noOcclusion().lightLevel(s -> 11));
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(AXIS); }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // Flip axis so pane faces the player like a nether portal
        Direction.Axis a = ctx.getHorizontalDirection().getAxis() == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        return defaultBlockState().setValue(AXIS, a);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos,
                           BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel server && server.dimension().equals(Level.OVERWORLD)
                && !oldState.is(this)) {
            PortalAtmosphereData.get(server).add(pos);
        }
        if (level instanceof ServerLevel server && !oldState.is(this)) {
            server.scheduleTick(pos, this, 2);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) { reorientConnected(level,pos); }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level,
                                                BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        if (level.dimension().equals(Level.OVERWORLD)) {
            PortalAtmosphereData.get(level).remove(pos);
        }
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            if (level.getBlockState(neighbor).is(ModBlocks.SCHIZM_PORTAL)) {
                reorientConnected(level, neighbor);
            }
        }
    }

    /**
     * Chooses one plane for a face-connected portal component from the direction
     * in which its blocks actually extend. Axis X means an X/Y surface; axis Z
     * means a Z/Y surface. State updates are deliberately redstone-silent.
     */
    public static void reorientConnected(ServerLevel level, BlockPos start) {
        BlockState startState = level.getBlockState(start);
        if (!startState.is(ModBlocks.SCHIZM_PORTAL)) return;

        Set<BlockPos> component = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        BlockPos immutableStart = start.immutable();
        component.add(immutableStart);
        queue.add(immutableStart);
        int minX = start.getX(), maxX = start.getX();
        int minZ = start.getZ(), maxZ = start.getZ();

        while (!queue.isEmpty() && component.size() <= 4096) {
            BlockPos current = queue.removeFirst();
            minX = Math.min(minX, current.getX());
            maxX = Math.max(maxX, current.getX());
            minZ = Math.min(minZ, current.getZ());
            maxZ = Math.max(maxZ, current.getZ());
            for (Direction direction : Direction.values()) {
                BlockPos next = current.relative(direction);
                if (!level.hasChunkAt(next) || component.contains(next) || !level.getBlockState(next).is(ModBlocks.SCHIZM_PORTAL)) continue;
                BlockPos immutable = next.immutable();
                component.add(immutable);
                queue.addLast(immutable);
            }
        }

        int xLinks = 0;
        int zLinks = 0;
        for (BlockPos pos : component) {
            if (component.contains(pos.relative(Direction.EAST))) xLinks++;
            if (component.contains(pos.relative(Direction.SOUTH))) zLinks++;
        }

        Direction.Axis desired = startState.getValue(AXIS);
        if (xLinks > zLinks) {
            desired = Direction.Axis.X;
        } else if (zLinks > xLinks) {
            desired = Direction.Axis.Z;
        } else if (xLinks > 0) {
            int xSpan = maxX - minX;
            int zSpan = maxZ - minZ;
            if (xSpan > zSpan) desired = Direction.Axis.X;
            else if (zSpan > xSpan) desired = Direction.Axis.Z;
        }

        for (BlockPos pos : component) {
            BlockState state = level.getBlockState(pos);
            if (state.getValue(AXIS) != desired) {
                level.setBlock(pos, state.setValue(AXIS, desired), QUIET_AXIS_FLAGS);
            }
        }
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return s.getValue(AXIS) == Direction.Axis.X ? SHAPE_X : SHAPE_Z;
    }

    // Client: ambient audio only. The connected renderer supplies the visual.
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        if (rand.nextInt(100) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS, 0.5F, 0.8F + rand.nextFloat() * 0.4F, false);
        }
    }

    // Keep unobtainable via middle-click (“pick block”)

    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player) {
        return ItemStack.EMPTY;
    }
}
