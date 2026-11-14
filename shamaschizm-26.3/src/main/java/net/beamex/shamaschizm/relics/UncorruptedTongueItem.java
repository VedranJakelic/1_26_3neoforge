package net.beamex.shamaschizm.relics;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class UncorruptedTongueItem extends Item {
    public UncorruptedTongueItem(Properties properties) {
        super(properties.food(new FoodProperties.Builder().nutrition(0).saturationModifier(0).alwaysEdible().build()));
    }
    @Override public InteractionResult use(Level level,Player player,InteractionHand hand) {
        if(TongueProtection.active(player))return InteractionResult.FAIL;
        return super.use(level,player,hand);
    }
    @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity entity) {
        // Recheck at completion: never consume a second relic for the same protection.
        if(TongueProtection.active(entity))return stack;
        ItemStack result=super.finishUsingItem(stack,level,entity);
        if(entity instanceof ServerPlayer player)TongueProtection.grant(player);
        return result;
    }
}
