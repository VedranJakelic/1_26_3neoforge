package net.beamex.shamaschizm.sacredheart;

import java.util.List;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemLore;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Legendary collectible; chest placement is deliberately left to the room author. */
@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class SacredHeartRegistration {
    public static Item SACRED_HEART;
    private SacredHeartRegistration() {}

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(Registries.ITEM, helper -> {
            var id = Shamaschizm.id("sacred_heart");
            SACRED_HEART = new Item(new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, id))
                    .stacksTo(1).rarity(Rarity.EPIC)
                    .component(DataComponents.ITEM_NAME, Component.translatableWithFallback(
                            "item.shamaschizm.sacred_heart", "Sacred Heart"))
                    .component(DataComponents.LORE, new ItemLore(List.of(
                            Component.translatableWithFallback("item.shamaschizm.legendary_loot", "Legendary Loot")
                                    .withStyle(ChatFormatting.GOLD)))));
            helper.register(id, SACRED_HEART);
        });
    }

    @SubscribeEvent
    public static void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().identifier().toString().equals("minecraft:tools_and_utilities"))
            event.accept(SACRED_HEART);
    }
}
