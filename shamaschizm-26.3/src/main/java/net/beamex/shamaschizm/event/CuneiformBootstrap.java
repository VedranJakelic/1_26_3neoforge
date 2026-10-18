package net.beamex.shamaschizm.event;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.block.CuneiformBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class CuneiformBootstrap {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(Shamaschizm.MOD_ID, "cuneiform1");
    public static CuneiformBlock CUNEIFORM1;
    public static Item CUNEIFORM1_ITEM;

    private CuneiformBootstrap() {}

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(Registries.BLOCK, helper -> {
            CUNEIFORM1 = new CuneiformBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, ID))
                    .mapColor(MapColor.STONE).sound(SoundType.STONE)
                    .strength(1.5F, 6.0F).noLootTable().pushReaction(PushReaction.IMMOVEABLE));
            helper.register(ID, CUNEIFORM1);
        });
        event.register(Registries.ITEM, helper -> {
            CUNEIFORM1_ITEM = new BlockItem(CUNEIFORM1, new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, ID)).useBlockDescriptionPrefix());
            helper.register(ID, CUNEIFORM1_ITEM);
        });
    }

    @SubscribeEvent
    public static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().identifier().equals(Identifier.parse("minecraft:building_blocks"))) {
            event.accept(CUNEIFORM1_ITEM);
        }
    }
}
