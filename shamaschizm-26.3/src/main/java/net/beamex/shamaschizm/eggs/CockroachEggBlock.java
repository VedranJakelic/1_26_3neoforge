package net.beamex.shamaschizm.eggs;

import com.mojang.serialization.MapCodec;
import net.beamex.shamaschizm.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TurtleEggBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Turtle-egg-sized clusters, without turtle incubation or sand requirements. */
public final class CockroachEggBlock extends Block {
    public static final IntegerProperty EGGS = TurtleEggBlock.EGGS;

    public CockroachEggBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(EGGS, 1));
    }



    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(EGGS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Blocks.TURTLE_EGG.defaultBlockState().setValue(TurtleEggBlock.EGGS, state.getValue(EGGS))
                .getShape(level, pos, context);
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return !context.isSecondaryUseActive() && context.getItemInHand().is(this.asItem())
                && state.getValue(EGGS) < 4;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState current = context.getLevel().getBlockState(context.getClickedPos());
        return current.is(this) ? current.setValue(EGGS, Math.min(4, current.getValue(EGGS) + 1))
                : this.defaultBlockState();
    }

    /** Player mining consumes one egg, keeping the rest of the cluster in place. */
    public void breakOne(ServerLevel level, BlockPos pos, BlockState state) {
        int eggs = state.getValue(EGGS);
        if (eggs == 1) level.removeBlock(pos, false);
        else level.setBlock(pos, state.setValue(EGGS, eggs - 1), 3);
        level.levelEvent(2001, pos, Block.getId(state));
        level.playSound(null, pos, SoundEvents.TURTLE_EGG_BREAK, SoundSource.BLOCKS, 0.8F, 1.0F);
        if (eggs > 1) releaseBabies(level, pos, 1);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos,
                                                boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        // Whole-cluster destruction (including the final player-broken egg,
        // explosions and mob breaking) releases one baby per remaining egg.
        if (!level.getBlockState(pos).is(this)) releaseBabies(level, pos, state.getValue(EGGS));
    }

    private void releaseBabies(ServerLevel level, BlockPos pos, int count) {
        for (int i = 0; i < count; i++) {
            var baby = ModEntities.BABY_ROACH.get().create(level, EntitySpawnReason.TRIGGERED);
            if (baby == null) continue;
            baby.snapTo(pos.getX() + 0.25D + level.getRandom().nextDouble() * 0.5D,
                    pos.getY() + 0.15D, pos.getZ() + 0.25D + level.getRandom().nextDouble() * 0.5D,
                    level.getRandom().nextFloat() * 360.0F, 0.0F);
            level.addFreshEntity(baby);
        }
    }
}
