package net.beamex.shamaschizm.grimoire;

import java.util.List;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid=Shamaschizm.MOD_ID)
public final class GrimoireRegistration {
    public static Item GRIMOIRE;
    public static boolean isBook(ItemStack stack) { return GRIMOIRE != null && stack.is(GRIMOIRE); }
    @SubscribeEvent public static void register(RegisterEvent event) {
        event.register(Registries.ITEM, helper -> {
            var id=Shamaschizm.id("grimoire_of_nerodemus");
            GRIMOIRE=new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id))
                .stacksTo(1).rarity(Rarity.EPIC)
                .component(DataComponents.STORED_ENCHANTMENTS,ItemEnchantments.EMPTY)
                .component(DataComponents.ITEM_NAME,Component.translatableWithFallback("item.shamaschizm.grimoire_of_nerodemus","Grimoire of Nerodemus"))
                .component(DataComponents.LORE,new ItemLore(List.of(Component.translatableWithFallback("item.shamaschizm.legendary_loot","Legendary Loot").withStyle(ChatFormatting.GOLD))))) {
                @Override public InteractionResult use(Level level,Player player,InteractionHand hand) { return InteractionResult.SUCCESS; }
                @Override public boolean isFoil(ItemStack stack) { return !stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS,ItemEnchantments.EMPTY).isEmpty(); }
            };
            helper.register(id,GRIMOIRE);
        });
    }
    @SubscribeEvent public static void creative(BuildCreativeModeTabContentsEvent event) {
        if(event.getTabKey().identifier().toString().equals("minecraft:tools_and_utilities")) event.accept(GRIMOIRE);
    }
}
