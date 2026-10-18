package net.beamex.shamaschizm.recipe;

import java.util.stream.Stream;
import net.beamex.shamaschizm.registry.ModDataComponents;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

/** Component-based equipment recognition includes third-party items without a hardcoded item list. */
public record UpgradeableIngredient() implements ICustomIngredient {
    public static boolean equipment(ItemStack stack) {
        return !stack.isEmpty() && (stack.isDamageableItem()
                || stack.has(DataComponents.EQUIPPABLE) || stack.has(DataComponents.TOOL)
                || stack.has(DataComponents.WEAPON) || stack.has(DataComponents.ENCHANTABLE));
    }
    @Override public boolean test(ItemStack stack) {
        return equipment(stack) && !stack.getOrDefault(ModDataComponents.RAISED_ENCHANT_CAP, false);
    }
    @Override public Stream<Holder<Item>> items() {
        return BuiltInRegistries.ITEM.listElements().filter(h -> equipment(h.value().getDefaultInstance()))
                .map(h -> h);
    }
    @Override public boolean isSimple() { return false; }
    @Override public IngredientType<?> getType() { return ModRecipeSerializers.UPGRADEABLE; }
}
