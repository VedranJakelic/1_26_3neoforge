package net.beamex.shamaschizm.slab;

import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class SlabRegistration {
    public static final Identifier SLAB_ID = Shamaschizm.id("slab01");
    public static final Identifier SLOT_ID = Shamaschizm.id("slab_slot");

    public static Item SLAB;
    public static Block SLOT;
    public static Item SLOT_ITEM;

    private SlabRegistration() {}

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(Registries.BLOCK, helper -> {
            ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, SLOT_ID);
            SLOT = new SlabSlotBlock(BlockBehaviour.Properties.of()
                    .setId(key)
                    .mapColor(MapColor.STONE)
                    .strength(3.0F, 9.0F)
                    .sound(SoundType.STONE)
                    .lightLevel(state -> state.getValue(SlabSlotBlock.SLOTTED) ? 15 : 0));
            helper.register(SLOT_ID, SLOT);
        });

        event.register(Registries.ITEM, helper -> {
            ResourceKey<Item> slabKey = ResourceKey.create(Registries.ITEM, SLAB_ID);
            SLAB = new SlabItem(new Item.Properties().setId(slabKey)
                    .stacksTo(1).rarity(Rarity.RARE));
            helper.register(SLAB_ID, SLAB);

            ResourceKey<Item> slotKey = ResourceKey.create(Registries.ITEM, SLOT_ID);
            SLOT_ITEM = new BlockItem(SLOT, new Item.Properties().setId(slotKey));
            helper.register(SLOT_ID, SLOT_ITEM);
        });
    }

    @SubscribeEvent
    public static void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) event.accept(SLAB);
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) event.accept(SLOT_ITEM);
    }
}
