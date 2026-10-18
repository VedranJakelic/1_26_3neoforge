package net.beamex.shamaschizm.eggs;

import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class EggRegistration {
    public static CockroachEggBlock COCKROACH_EGG;
    public static GiantCentipedeEggBlock GIANT_CENTIPEDE_EGG;
    public static Item COCKROACH_EGG_ITEM;
    public static Item GIANT_CENTIPEDE_EGG_ITEM;
    private EggRegistration() {}

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        var roachId = Shamaschizm.id("cockroach_egg");
        var centipedeId = Shamaschizm.id("giant_centipede_egg");
        event.register(Registries.BLOCK, helper -> {
            COCKROACH_EGG = new CockroachEggBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.TURTLE_EGG)
                    .setId(ResourceKey.create(Registries.BLOCK, roachId)).noLootTable());
            GIANT_CENTIPEDE_EGG = new GiantCentipedeEggBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SNIFFER_EGG)
                    .randomTicks().setId(ResourceKey.create(Registries.BLOCK, centipedeId))
                    .overrideLootTable(java.util.Optional.of(ResourceKey.create(Registries.LOOT_TABLE,
                            Shamaschizm.id("blocks/giant_centipede_egg")))));
            helper.register(roachId, COCKROACH_EGG);
            helper.register(centipedeId, GIANT_CENTIPEDE_EGG);
        });
        event.register(Registries.ITEM, helper -> {
            COCKROACH_EGG_ITEM = new BlockItem(COCKROACH_EGG, new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, roachId)).useBlockDescriptionPrefix());
            GIANT_CENTIPEDE_EGG_ITEM = new BlockItem(GIANT_CENTIPEDE_EGG, new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, centipedeId)).useBlockDescriptionPrefix());
            helper.register(roachId, COCKROACH_EGG_ITEM);
            helper.register(centipedeId, GIANT_CENTIPEDE_EGG_ITEM);
        });
    }

    @SubscribeEvent
    public static void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(COCKROACH_EGG_ITEM);
            event.accept(GIANT_CENTIPEDE_EGG_ITEM);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void breakEgg(BreakBlockEvent event) {
        if (event.getState().is(COCKROACH_EGG) && event.getLevel() instanceof ServerLevel level) {
            event.setCanceled(true);
            event.setNotifyClient(true);
            COCKROACH_EGG.breakOne(level, event.getPos(), event.getState());
        }
    }
}
