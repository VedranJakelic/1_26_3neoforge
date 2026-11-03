package net.beamex.shamaschizm.item.custom;

import net.beamex.shamaschizm.effect.OvertreeDewCountdown;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Edible, harvestable droplet. Its timed effect is rolled once per consumption. */
public final class DewOfTheOvertreeItem extends Item {
    private static final FoodProperties FOOD = new FoodProperties.Builder()
            .nutrition(2)
            .saturationModifier(0.1F)
            .build();

    public DewOfTheOvertreeItem(Properties properties) {
        super(properties.food(FOOD));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity eater) {
        ItemStack result = super.finishUsingItem(stack, level, eater);
        if (eater instanceof ServerPlayer player && player.getRandom().nextFloat() < 0.40F) {
            OvertreeDewCountdown.arm(player);
        }
        return result;
    }
}
