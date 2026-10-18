package net.beamex.shamaschizm.nabonidus;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.*;

public final class StoneEyeBlock extends Block {
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 2);
    public StoneEyeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(STAGE, 0));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(STAGE); }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        activate(state, level, pos); return InteractionResult.SUCCESS;
    }
    @Override protected InteractionResult useItemOn(ItemStack item, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        activate(state, level, pos); return InteractionResult.SUCCESS;
    }
    public void activate(BlockState state, Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server) || state.getValue(STAGE) != 0) return;
        level.setBlock(pos, state.setValue(STAGE, 1), 3);
        server.scheduleTick(pos, this, 12);
        level.playSound(null, pos, SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.BLOCKS, 1.0F, 1.0F);
        for (var player : server.players()) if (!player.isSpectator() && player.isAlive() && player.distanceToSqr(Vec3.atCenterOf(pos)) <= 49) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 0));
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0));
        }
    }
    @Override protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(STAGE) != 1) return;
        level.setBlock(pos, state.setValue(STAGE, 2), 3);
        for (var player : level.players()) if (!player.isSpectator() && player.isAlive()
                && player.distanceToSqr(Vec3.atCenterOf(pos)) <= 100 && !player.hasEffect(Nabonidus.curse())) {
            player.addEffect(new MobEffectInstance(Nabonidus.curse(), 3600, 0, false, false, true));
        }
    }
}
