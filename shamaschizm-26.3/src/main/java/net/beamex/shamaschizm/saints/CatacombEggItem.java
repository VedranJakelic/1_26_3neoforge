package net.beamex.shamaschizm.saints;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;

public final class CatacombEggItem extends SpawnEggItem {
    public CatacombEggItem(Properties properties) { super(properties); }
    @Override public InteractionResult use(net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
        return InteractionResult.PASS; // Place the arena on a solid block, never in a fluid.
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        if (context.getPlayer() == null) return InteractionResult.FAIL;
        var pos = context.getClickedPos().above();
        if (!CatacombEncounterEntity.createEncounter(level, Vec3.atBottomCenterOf(pos), context.getPlayer()))
            return InteractionResult.FAIL;
        context.getItemInHand().consume(1, context.getPlayer());
        return InteractionResult.SUCCESS_SERVER;
    }
}
