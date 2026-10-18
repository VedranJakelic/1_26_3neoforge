package net.beamex.shamaschizm.item.custom;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public final class MagicMushroomItem extends BlockItem {

    private static final FoodProperties FOOD = new FoodProperties.Builder()
            .nutrition(1)
            .saturationModifier(0.1f)
            .alwaysEdible()
            .build();

    public MagicMushroomItem(Block block, Properties properties) {
        super(block, properties.food(FOOD));
    }

    @Override
    public ItemStack finishUsingItem(
            ItemStack stack,
            Level level,
            LivingEntity entity
    ) {
        ItemStack result = super.finishUsingItem(stack, level, entity);

        if (entity instanceof net.minecraft.server.level.ServerPlayer player) {
            net.beamex.shamaschizm.effect.MushroomOnset.schedule(player);
        }

        return result;
    }
}