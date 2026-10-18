package net.beamex.shamaschizm.world.block;

import com.mojang.serialization.MapCodec;
import java.util.Comparator;
import net.beamex.shamaschizm.entity.custom.GateEntity;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Invisible, unbreakable backing blocks that give a closed gate true block collision. */
public final class GateBarrierBlock extends Block {

    public GateBarrierBlock(BlockBehaviour.Properties properties) {
        super(properties);
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
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        GateEntity gate = level.getEntitiesOfClass(GateEntity.class, new AABB(pos).inflate(3.25D), entity -> !entity.isOpen())
                .stream()
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(Vec3.atCenterOf(pos))))
                .orElse(null);
        return gate == null ? InteractionResult.TRY_WITH_EMPTY_HAND : gate.tryOpen(player, hand);
    }
}
