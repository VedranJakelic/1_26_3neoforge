package net.beamex.shamaschizm.item;

import net.beamex.shamaschizm.entity.GateRegistration;
import net.beamex.shamaschizm.entity.custom.GateEntity;
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

/** Places the gate on a block-aligned center and snaps its front toward the player. */
public final class GateSpawnEggItem extends SpawnEggItem {
    public GateSpawnEggItem(Item.Properties properties) {
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

        GateEntity gate = GateRegistration.GATE.create(serverLevel, EntitySpawnReason.SPAWN_ITEM_USE);
        if (gate == null) return InteractionResult.FAIL;
        gate.snapTo(x, base.getY(), z, 0.0F, 0.0F);
        gate.setGateFacing(facing);
        if (!gate.canInstallCollisionBlocks() || !serverLevel.noCollision(gate)) {
            return InteractionResult.FAIL;
        }

        gate.applyComponentsFromItemStack(stack);
        serverLevel.addFreshEntity(gate);
        gate.installCollisionBlocks();
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
