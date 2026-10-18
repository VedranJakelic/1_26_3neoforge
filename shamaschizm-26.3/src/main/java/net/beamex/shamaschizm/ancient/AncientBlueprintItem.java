// FILE: src/main/java/net/beamex/shamaschizm/ancient/AncientBlueprintItem.java
package net.beamex.shamaschizm.ancient;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public final class AncientBlueprintItem extends SmithingTemplateItem {
    private static final Component APPLIES_TO    = Component.translatable("tooltip.shamaschizm.applies_to_any_gear");
    private static final Component INGREDIENTS   = Component.translatable("tooltip.shamaschizm.ingredients_lapis");
    private static final Component BASE_SLOT     = Component.translatable("tooltip.shamaschizm.slot_base");
    private static final Component ADDITION_SLOT = Component.translatable("tooltip.shamaschizm.slot_lapis");

    public AncientBlueprintItem(Item.Properties props) {
        // Keep your fixed, modern sprites
        super(APPLIES_TO, INGREDIENTS, BASE_SLOT, ADDITION_SLOT, baseIcons(), addIcons(), props);
    }

    // Mapping variant A (older): Level-based
    public void appendHoverText(ItemStack stack,
                                Level level,
                                List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.shamaschizm.ancient_blueprint"));
    }

    // Mapping variant B (newer): Item.TooltipContext-based
    public void appendHoverText(ItemStack stack,
                                Item.TooltipContext ctx,
                                List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.shamaschizm.ancient_blueprint"));
    }

    private static List<Identifier> baseIcons() {
        return List.of(
                rl("minecraft","container/slot/helmet"),
                rl("minecraft","container/slot/chestplate"),
                rl("minecraft","container/slot/leggings"),
                rl("minecraft","container/slot/boots")
        );
    }

    private static List<Identifier> addIcons() {
        return List.of(rl("minecraft","container/slot/lapis_lazuli"));
    }

    private static Identifier rl(String ns, String path) {
        return Identifier.fromNamespaceAndPath(ns, path);
    }
}
