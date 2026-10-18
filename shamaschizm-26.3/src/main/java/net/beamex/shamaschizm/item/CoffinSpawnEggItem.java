package net.beamex.shamaschizm.item;

import net.beamex.shamaschizm.entity.CoffinRegistration;
import net.beamex.shamaschizm.entity.custom.CoffinEntity;
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

/** Block-aligned coffin placement, with its long axis pointing toward the player. */
public final class CoffinSpawnEggItem extends SpawnEggItem {
    public CoffinSpawnEggItem(Item.Properties properties) {
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
        CoffinEntity coffin = CoffinRegistration.COFFIN.create(serverLevel, EntitySpawnReason.SPAWN_ITEM_USE);
        if (coffin == null) return InteractionResult.FAIL;
        coffin.snapTo(x, base.getY(), z, 0.0F, 0.0F);
        coffin.setCoffinFacing(facing);
        if (!coffin.canInstallCollisionBlocks() || !serverLevel.noCollision(coffin)) {
            return InteractionResult.FAIL;
        }

        coffin.applyComponentsFromItemStack(stack);
        serverLevel.addFreshEntity(coffin);
        coffin.installCollisionBlocks();
        stack.consume(1, player);
        player.awardStat(Stats.ITEM_USED.get(this));
        serverLevel.gameEvent(player, GameEvent.ENTITY_PLACE, base);
        return InteractionResult.SUCCESS_SERVER;
    }

    private static Direction cardinalToward(double x, double z, Player player) {
        double dx = player.getX() - x;
        double dz = player.getZ() - z;
        if (Math.abs(dx) > Math.abs(dz)) return dx >= 0 ? Direction.EAST : Direction.WEST;
        return dz >= 0 ? Direction.SOUTH : Direction.NORTH;
    }
}
