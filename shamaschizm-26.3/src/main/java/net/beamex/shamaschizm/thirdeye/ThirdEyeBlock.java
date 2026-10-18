package net.beamex.shamaschizm.thirdeye;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class ThirdEyeBlock extends BaseEntityBlock {
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 2);
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");

    private static final int OPENING_TICKS = 20;
    private static final int OPEN_TICKS = 60;

    public ThirdEyeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(STAGE, 0)
                .setValue(POWERED, false));
    }



    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE, POWERED);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ThirdEyeBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hit) {
        activateForPlayer(state, level, pos, player);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack item, BlockState state, Level level,
                                           BlockPos pos, Player player, InteractionHand hand,
                                           BlockHitResult hit) {
        activateForPlayer(state, level, pos, player);
        return InteractionResult.SUCCESS;
    }

    public void activateForPlayer(BlockState state, Level level, BlockPos pos, Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            activate(state, (ServerLevel) level, pos, List.of(serverPlayer));
        }
    }

    private void activateFromRedstone(BlockState state, ServerLevel level, BlockPos pos) {
        List<ServerPlayer> targets = level.players().stream()
                .filter(player -> player.isAlive() && !player.isSpectator()
                        && player.distanceToSqr(Vec3.atCenterOf(pos)) <= 100.0D)
                .toList();
        activate(state, level, pos, targets);
    }

    private void activate(BlockState state, ServerLevel level, BlockPos pos,
                          List<ServerPlayer> targets) {
        if (state.getValue(STAGE) != 0) return;

        if (level.getBlockEntity(pos) instanceof ThirdEyeBlockEntity eye) {
            eye.setPendingPlayers(targets);
        }

        level.setBlock(pos, state.setValue(STAGE, 1), Block.UPDATE_ALL);
        level.scheduleTick(pos, this, OPENING_TICKS);
        level.playSound(null, pos, SoundEvents.ELDER_GUARDIAN_CURSE,
                SoundSource.BLOCKS, 1.0F, 1.0F);

        for (ServerPlayer player : targets) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 1));
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int stage = state.getValue(STAGE);
        if (stage == 1) {
            level.setBlock(pos, state.setValue(STAGE, 2), Block.UPDATE_ALL);
            level.scheduleTick(pos, this, OPEN_TICKS);
            if (level.getBlockEntity(pos) instanceof ThirdEyeBlockEntity eye) {
                eye.applyNausea(level);
            }
        } else if (stage == 2) {
            // Close the eye and remove its light on the same tick as teleportation.
            level.setBlock(pos, state.setValue(STAGE, 0), Block.UPDATE_ALL);
            if (level.getBlockEntity(pos) instanceof ThirdEyeBlockEntity eye) {
                eye.teleportPendingPlayers(level, pos);
            }
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor,
                                   @Nullable Orientation orientation, boolean movedByPiston) {
        if (!(level instanceof ServerLevel server)) return;
        boolean poweredNow = level.hasNeighborSignal(pos);
        boolean poweredBefore = state.getValue(POWERED);
        if (poweredNow == poweredBefore) return;

        BlockState updated = state.setValue(POWERED, poweredNow);
        level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
        if (poweredNow && updated.getValue(STAGE) == 0) {
            activateFromRedstone(updated, server, pos);
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState,
                           boolean movedByPiston) {
        if (!(level instanceof ServerLevel server) || oldState.is(this)) return;
        boolean powered = level.hasNeighborSignal(pos);
        if (!powered) return;

        BlockState updated = state.setValue(POWERED, true);
        level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
        activateFromRedstone(updated, server, pos);
    }
}
