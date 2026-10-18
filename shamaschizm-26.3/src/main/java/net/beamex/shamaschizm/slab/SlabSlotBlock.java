package net.beamex.shamaschizm.slab;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;

public final class SlabSlotBlock extends Block {
    public static final BooleanProperty SLOTTED = BlockStateProperties.POWERED;
    private static final int EFFECT_DURATION = 5 * 60 * 20;

    public SlabSlotBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(SLOTTED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SLOTTED);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                          BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        if (state.getValue(SLOTTED) || !stack.is(SlabRegistration.SLAB)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (!player.getAbilities().instabuild) stack.shrink(1);
        level.setBlock(pos, state.setValue(SLOTTED, true), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 0.9F);
        level.updateNeighborsAt(pos, this);

        ServerLevel server = (ServerLevel) level;
        for (Player nearby : server.getEntitiesOfClass(Player.class,
                new AABB(pos).inflate(5.0D), p -> p.isAlive() && p.distanceToSqr(
                        pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 25.0D)) {
            nearby.addEffect(new MobEffectInstance(MobEffects.REGENERATION,
                    EFFECT_DURATION, 0, false, true, true));
            nearby.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,
                    EFFECT_DURATION, 0, false, true, true));
            // Vanilla Absorption I grants four health points; this relic grants
            // exactly one heart (two health points) as requested.
            nearby.setAbsorptionAmount(Math.max(nearby.getAbsorptionAmount(), 2.0F));
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int ownSignal(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(SLOTTED) ? 15 : 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(SLOTTED) ? 15 : 0;
    }
}
