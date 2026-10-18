package net.beamex.shamaschizm.world.block;

import com.mojang.serialization.MapCodec;
import net.beamex.shamaschizm.entity.custom.MegalithicGateEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** A three-quarter-block-tall puzzle brazier with campfire-style interactions. */
public final class BrazierBlock extends Block implements SimpleWaterloggedBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final double GATE_RADIUS = 10.0D;
    private static final VoxelShape SHAPE = Block.column(16.0D, 0.0D, 12.0D);

    public BrazierBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false).setValue(WATERLOGGED, false));
    }



    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        boolean water = context.getLevel().getFluidState(context.getClickedPos()).is(Fluids.WATER);
        return defaultBlockState().setValue(WATERLOGGED, water).setValue(LIT, false);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (!state.getValue(LIT) && !state.getValue(WATERLOGGED)
                && (stack.is(Items.FLINT_AND_STEEL) || stack.is(Items.FIRE_CHARGE))) {
            if (level instanceof ServerLevel serverLevel) {
                level.playSound(null, pos, stack.is(Items.FLINT_AND_STEEL)
                        ? SoundEvents.FLINTANDSTEEL_USE : SoundEvents.FIRECHARGE_USE,
                        SoundSource.BLOCKS, 1.0F, 0.9F + level.getRandom().nextFloat() * 0.2F);
                level.setBlock(pos, state.setValue(LIT, true), 11);
                level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
                if (!player.hasInfiniteMaterials()) {
                    if (stack.is(Items.FLINT_AND_STEEL)) {
                        stack.hurtAndBreak(1, player, hand);
                    } else {
                        stack.shrink(1);
                    }
                }
                notifyNearbyGates(serverLevel, pos);
                return InteractionResult.SUCCESS_SERVER;
            }
            return InteractionResult.SUCCESS;
        }
        if (state.getValue(LIT) && stack.is(ItemTags.SHOVELS)) {
            if (level instanceof ServerLevel serverLevel) {
                extinguish(serverLevel, pos, state, player);
                if (!player.hasInfiniteMaterials()) stack.hurtAndBreak(1, player, hand);
                return InteractionResult.SUCCESS_SERVER;
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (level instanceof ServerLevel serverLevel && projectile.isOnFire()
                && projectile.mayInteract(serverLevel, hit.getBlockPos())
                && !state.getValue(LIT) && !state.getValue(WATERLOGGED)) {
            serverLevel.setBlock(hit.getBlockPos(), state.setValue(LIT, true), 11);
            serverLevel.gameEvent(projectile.getOwner(), GameEvent.BLOCK_CHANGE, hit.getBlockPos());
            notifyNearbyGates(serverLevel, hit.getBlockPos());
        }
    }

    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluid) {
        if (state.getValue(WATERLOGGED) || !fluid.is(Fluids.WATER)) return false;
        if (!level.isClientSide() && state.getValue(LIT)) {
            level.playSound(null, pos, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        level.setBlock(pos, state.setValue(WATERLOGGED, true).setValue(LIT, false), 3);
        level.scheduleTick(pos, fluid.getType(), fluid.getType().getTickDelay(level));
        if (level instanceof ServerLevel serverLevel) notifyNearbyGates(serverLevel, pos);
        return true;
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
                                     BlockPos pos, Direction direction, BlockPos neighborPos,
                                     BlockState neighborState, RandomSource random) {
        if (state.getValue(WATERLOGGED)) ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (level instanceof ServerLevel serverLevel
                && (!oldState.is(this) || oldState.getValue(LIT) != state.getValue(LIT))) {
            notifyNearbyGates(serverLevel, pos);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        if (!movedByPiston) notifyNearbyGates(level, pos);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) return;
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.79D;
        double z = pos.getZ() + 0.5D;
        level.addParticle(ParticleTypes.SMALL_FLAME, x + random.triangle(0.0D, 0.11D), y,
                z + random.triangle(0.0D, 0.11D), 0.0D, 0.012D, 0.0D);
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.SMOKE, x, y + 0.1D, z, 0.0D, 0.025D, 0.0D);
        }
        if (random.nextInt(12) == 0) {
            level.playLocalSound(x, y, z, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS,
                    0.45F + random.nextFloat() * 0.35F, 0.8F + random.nextFloat() * 0.3F, false);
        }
    }

    private static void extinguish(ServerLevel level, BlockPos pos, BlockState state, Player player) {
        level.setBlock(pos, state.setValue(LIT, false), 11);
        level.levelEvent(player, 1009, pos, 0);
        level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        notifyNearbyGates(level, pos);
    }

    public static void notifyNearbyGates(ServerLevel level, BlockPos pos) {
        AABB area = new AABB(pos).inflate(GATE_RADIUS + 2.0D);
        for (MegalithicGateEntity gate : level.getEntitiesOfClass(MegalithicGateEntity.class, area)) {
            double dx = gate.getX() - (pos.getX() + 0.5D);
            double dy = gate.getY() + 1.5D - (pos.getY() + 0.5D);
            double dz = gate.getZ() - (pos.getZ() + 0.5D);
            if (dx * dx + dy * dy + dz * dz <= GATE_RADIUS * GATE_RADIUS) {
                gate.requestBrazierCheck();
            }
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT, WATERLOGGED);
    }
}
