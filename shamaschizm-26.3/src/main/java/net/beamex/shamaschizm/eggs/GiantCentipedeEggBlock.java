package net.beamex.shamaschizm.eggs;

import com.mojang.serialization.MapCodec;
import net.beamex.shamaschizm.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Incubation uses saved scheduled block ticks, independently of random tick speed. */
public final class GiantCentipedeEggBlock extends Block {
    private static final int MIN_HATCH_TICKS = 20 * 60 * 20;
    private static final int HATCH_VARIATION_TICKS = 5 * 60 * 20;

    public GiantCentipedeEggBlock(BlockBehaviour.Properties properties) { super(properties); }


    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Blocks.SNIFFER_EGG.defaultBlockState().getShape(level, pos, context);
    }

    private void startIncubation(ServerLevel level, BlockPos pos) {
        if (!level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, MIN_HATCH_TICKS + level.getRandom().nextInt(HATCH_VARIATION_TICKS + 1));
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!oldState.is(this) && level instanceof ServerLevel server) startIncubation(server, pos);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Also covers templates placed without onPlace notifications.
        startIncubation(level, pos);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.is(this)) return;
        var head = ModEntities.GIANT_CENTIPEDE.get().create(level, EntitySpawnReason.TRIGGERED);
        if (head == null) { level.scheduleTick(pos, this, 20); return; }
        head.snapTo(pos.getX() + 0.5D, pos.getY() + 0.1D, pos.getZ() + 0.5D,
                random.nextInt(4) * 90.0F, 0.0F);
        if (!level.addFreshEntity(head)) { level.scheduleTick(pos, this, 20); return; }
        // Removal does not call the slimeball loot table.
        level.removeBlock(pos, false);
        level.levelEvent(2001, pos, Block.getId(state));
        level.playSound(null, pos, SoundEvents.TURTLE_EGG_HATCH, SoundSource.BLOCKS, 1.0F, 0.65F);
        head.initializeEggChain(level);
    }
}
