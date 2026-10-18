package net.beamex.shamaschizm.client;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.registry.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
@EventBusSubscriber(modid = Shamaschizm.MOD_ID, value = Dist.CLIENT)
public final class UpgradeTooltip {
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event) {
        if (event.getItemStack().getOrDefault(ModDataComponents.RAISED_ENCHANT_CAP, false)) {
            event.getToolTip().add(Component.translatable("tooltip.shamaschizm.ancient_cap"));
        }
        if (event.getItemStack().getOrDefault(ModDataComponents.ANCIENT_APPEARANCE, false)) {
            event.getToolTip().add(Component.translatable("tooltip.shamaschizm.ancient_appearance"));
        }
    }
}
