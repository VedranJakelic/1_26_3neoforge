package net.beamex.shamaschizm.item;

import net.beamex.shamaschizm.entity.FrogGodRegistration;
import net.beamex.shamaschizm.entity.custom.FrogGodEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

/** Places the frog at block center and snaps it toward the player, like the gates. */
public final class FrogGodSpawnEggItem extends SpawnEggItem {
    public FrogGodSpawnEggItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (player == null) return InteractionResult.FAIL;
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;

        BlockPos base = context.getClickedPos().above();
        double x = base.getX() + 0.5D;
        double z = base.getZ() + 0.5D;
        Direction facing = cardinalToward(x, z, player);
        FrogGodEntity frog = FrogGodRegistration.FROG_GOD.create(
                serverLevel, EntitySpawnReason.SPAWN_ITEM_USE);
        if (frog == null) return InteractionResult.FAIL;

        float yaw = facing.toYRot();
        frog.snapTo(x, base.getY(), z, yaw, 0.0F);
        frog.setYRot(yaw);
        frog.yBodyRot = yaw;
        frog.yHeadRot = yaw;
        if (!serverLevel.noCollision(frog)) return InteractionResult.FAIL;

        frog.applyComponentsFromItemStack(stack);
        serverLevel.addFreshEntity(frog);
        stack.consume(1, player);
        player.awardStat(Stats.ITEM_USED.get(this));
        serverLevel.gameEvent(player, GameEvent.ENTITY_PLACE, base);
        return InteractionResult.SUCCESS_SERVER;
    }

    private static Direction cardinalToward(double x, double z, Player player) {
        double dx = player.getX() - x;
        double dz = player.getZ() - z;
        if (Math.abs(dx) > Math.abs(dz)) return dx >= 0.0D ? Direction.EAST : Direction.WEST;
        return dz >= 0.0D ? Direction.SOUTH : Direction.NORTH;
    }
}
