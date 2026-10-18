package net.beamex.shamaschizm.event;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.block.CuneiformBlock;
import net.beamex.shamaschizm.block.HangingBannerBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class BannerSunBootstrap {
    private static final String[] NAMES = {"torn_eye_banner", "torn_sun_banner", "stag_banner", "stag_banner_tall", "sun_stone_wall"};
    private static final Block[] BLOCKS = new Block[NAMES.length];
    private static final Item[] ITEMS = new Item[NAMES.length];
    private BannerSunBootstrap() {}
    @SubscribeEvent public static void register(RegisterEvent event) {
        event.register(Registries.BLOCK, helper -> {
            for (int i = 0; i < NAMES.length; i++) {
                Identifier id = Shamaschizm.id(NAMES[i]);
                BlockBehaviour.Properties props = BlockBehaviour.Properties.of()
                        .setId(ResourceKey.create(Registries.BLOCK, id))
                        .mapColor(i == 4 ? MapColor.STONE : MapColor.COLOR_GREEN)
                        .sound(i == 4 ? SoundType.STONE : SoundType.WOOL)
                        .strength(1.5F).noLootTable().pushReaction(PushReaction.IMMOVEABLE);
                if (i != 4) props.noOcclusion();
                BLOCKS[i] = i == 4 ? new CuneiformBlock(props) : new HangingBannerBlock(props, i == 1 || i == 3 ? 4 : 2);
                helper.register(id, BLOCKS[i]);
            }
        });
        event.register(Registries.ITEM, helper -> {
            for (int i = 0; i < NAMES.length; i++) {
                Identifier id = Shamaschizm.id(NAMES[i]);
                ITEMS[i] = new BlockItem(BLOCKS[i], new Item.Properties()
                        .setId(ResourceKey.create(Registries.ITEM, id)).useBlockDescriptionPrefix());
                helper.register(id, ITEMS[i]);
            }
        });
    }
    @SubscribeEvent public static void tab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().identifier().equals(Identifier.parse("minecraft:building_blocks")))
            for (Item item : ITEMS) if (item != null) event.accept(item);
    }
}
