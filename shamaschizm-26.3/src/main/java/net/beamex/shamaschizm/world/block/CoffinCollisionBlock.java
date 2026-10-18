package net.beamex.shamaschizm.world.block;

import com.mojang.serialization.MapCodec;
import java.util.Comparator;
import net.beamex.shamaschizm.entity.custom.CoffinEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Invisible two-block shell: solid when closed and hollow like a cauldron when open. */
public final class CoffinCollisionBlock extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final BooleanProperty FRONT = BooleanProperty.create("front");

    private static final VoxelShape CLOSED = Block.box(0, 0, 0, 16, 10, 16);
    private static final VoxelShape FLOOR = Block.box(0, 0, 0, 16, 2, 16);

    public CoffinCollisionBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(OPEN, false)
                .setValue(FRONT, true));
    }



    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN, FRONT);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    protected int getLightDampening(BlockState state) {
        return 0;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                  CollisionContext context) {
        return collision(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                           CollisionContext context) {
        return collision(state);
    }

    private static VoxelShape collision(BlockState state) {
        if (!state.getValue(OPEN)) return CLOSED;
        Direction facing = state.getValue(FACING);
        Direction outerEnd = state.getValue(FRONT) ? facing : facing.getOpposite();
        VoxelShape sideA;
        VoxelShape sideB;
        VoxelShape end;
        if (facing.getAxis() == Direction.Axis.Z) {
            sideA = Block.box(0, 0, 0, 2, 10, 16);
            sideB = Block.box(14, 0, 0, 16, 10, 16);
            end = outerEnd == Direction.NORTH
                    ? Block.box(0, 0, 0, 16, 10, 2)
                    : Block.box(0, 0, 14, 16, 10, 16);
        } else {
            sideA = Block.box(0, 0, 0, 16, 10, 2);
            sideB = Block.box(0, 0, 14, 16, 10, 16);
            end = outerEnd == Direction.WEST
                    ? Block.box(0, 0, 0, 2, 10, 16)
                    : Block.box(14, 0, 0, 16, 10, 16);
        }
        return Shapes.or(FLOOR, sideA, sideB, end);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        CoffinEntity coffin = findCoffin(level, pos);
        return coffin == null ? InteractionResult.TRY_WITH_EMPTY_HAND : coffin.tryInteract(player, hand);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        CoffinEntity coffin = findCoffin(level, pos);
        return coffin == null ? InteractionResult.PASS : coffin.tryInteract(player, InteractionHand.MAIN_HAND);
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        CoffinEntity coffin = findCoffin(level, pos);
        if (coffin != null) coffin.tryDig(player);
    }

    private static CoffinEntity findCoffin(Level level, BlockPos pos) {
        return level.getEntitiesOfClass(CoffinEntity.class, new AABB(pos).inflate(2.5D))
                .stream()
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(Vec3.atCenterOf(pos))))
                .orElse(null);
    }
}
