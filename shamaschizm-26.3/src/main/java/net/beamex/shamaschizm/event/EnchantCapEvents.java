package net.beamex.shamaschizm.event;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.registry.ModDataComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;

@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class EnchantCapEvents {
    @SubscribeEvent public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        if (!left.getOrDefault(ModDataComponents.RAISED_ENCHANT_CAP, false)) return;
        // Repair materials and renaming continue through the ordinary anvil path.
        if (!right.is(Items.ENCHANTED_BOOK) && !right.is(left.getItem())) return;
        var incoming = right.getOrDefault(right.is(Items.ENCHANTED_BOOK)
                ? DataComponents.STORED_ENCHANTMENTS : DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (incoming.isEmpty()) return;
        var current = left.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        var merged = new ItemEnchantments.Mutable(current);
        long xp = 0;
        for (var enchantment : incoming.keySet()) {
            int oldLevel = current.getLevel(enchantment);
            int incomingLevel = incoming.getLevel(enchantment);
            int next = Math.max(oldLevel, Math.min(10,
                    oldLevel == incomingLevel ? oldLevel + 1 : incomingLevel));
            if (next > oldLevel) {
                merged.set(enchantment, next);
                xp += (next - oldLevel) * 3L + Math.max(0, next - 5)
                        + (oldLevel == incomingLevel ? 1 : 0);
            }
        }
        if (xp == 0) return;
        // Preserve vanilla repair/rename work, but use our full enchantment map (including conflicting enchants).
        ItemStack result = event.getVanillaResult().output();
        if (result.isEmpty()) result = left.copyWithCount(1);
        result.set(DataComponents.ENCHANTMENTS, merged.toImmutable());
        result.set(ModDataComponents.RAISED_ENCHANT_CAP, true);
        String name = event.getName();
        if (name != null) {
            if (name.isBlank()) result.remove(DataComponents.CUSTOM_NAME);
            else if (!name.equals(left.getHoverName().getString())) result.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        }
        int prior = left.getOrDefault(DataComponents.REPAIR_COST, 0);
        result.set(DataComponents.REPAIR_COST, (int) Math.min(Integer.MAX_VALUE, (long) prior + 3));
        xp += Math.max(0, prior) + (long) Math.max(0, right.getOrDefault(DataComponents.REPAIR_COST, 0));
        if (!right.is(Items.ENCHANTED_BOOK)) xp += 5;
        event.setOutput(result);
        event.setXpCost((int) Math.min(39, Math.max(1, xp)));
        event.setMaterialCost(1);
    }
}
