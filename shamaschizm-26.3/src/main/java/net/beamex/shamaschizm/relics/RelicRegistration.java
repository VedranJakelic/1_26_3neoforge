package net.beamex.shamaschizm.relics;

import java.util.List;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid=Shamaschizm.MOD_ID)
public final class RelicRegistration {
    public static Item FINGER, TONGUE, DIAMOND_CLUSTER_ITEM;
    public static Block DIAMOND_CLUSTER;
    private RelicRegistration() {}
    private static Item.Properties named(String id,String name) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM,Shamaschizm.id(id)))
                .component(DataComponents.ITEM_NAME,Component.translatableWithFallback("item.shamaschizm."+id,name));
    }
    private static Item.Properties relic(String id,String name) {
        return named(id,name).stacksTo(1).rarity(Rarity.EPIC)
                .component(DataComponents.LORE,new ItemLore(List.of(Component.translatableWithFallback(
                        "item.shamaschizm.legendary_loot","Legendary Loot").withStyle(ChatFormatting.GOLD))));
    }
    @SubscribeEvent public static void register(RegisterEvent event) {
        event.register(Registries.BLOCK,helper->{
            var id=Shamaschizm.id("diamond_cluster");
            DIAMOND_CLUSTER=new AmethystClusterBlock(5.0F,3.0F,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.LARGE_AMETHYST_BUD)
                            .setId(ResourceKey.create(Registries.BLOCK,id))
                            .overrideDescription("block.shamaschizm.diamond_cluster")
                            .overrideLootTable(java.util.Optional.of(ResourceKey.create(Registries.LOOT_TABLE,Shamaschizm.id("blocks/diamond_cluster")))));
            helper.register(id,DIAMOND_CLUSTER);
        });
        event.register(Registries.ITEM,helper->{
            FINGER=new Item(relic("finger_of_great_anthias","Finger of great Anthias"));
            TONGUE=new UncorruptedTongueItem(relic("uncorrupted_tongue","Uncorrupted tongue"));
            DIAMOND_CLUSTER_ITEM=new BlockItem(DIAMOND_CLUSTER,named("diamond_cluster","Diamond cluster"));
            helper.register(Shamaschizm.id("finger_of_great_anthias"),FINGER);
            helper.register(Shamaschizm.id("uncorrupted_tongue"),TONGUE);
            helper.register(Shamaschizm.id("diamond_cluster"),DIAMOND_CLUSTER_ITEM);
        });
    }
    @SubscribeEvent public static void tab(BuildCreativeModeTabContentsEvent event) {
        String tab=event.getTabKey().identifier().toString();
        if(tab.equals("minecraft:tools_and_utilities")){event.accept(FINGER);event.accept(TONGUE);}
        if(tab.equals("minecraft:natural_blocks"))event.accept(DIAMOND_CLUSTER_ITEM);
    }
}
