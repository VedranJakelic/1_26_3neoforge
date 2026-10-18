package net.beamex.shamaschizm.event;

import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.core.registries.BuiltInRegistries;


@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class ModCreativeTabContents {
    private ModCreativeTabContents() {}

    private static final Identifier MAGIC_MUSHROOM_ID =
            Identifier.fromNamespaceAndPath(Shamaschizm.MOD_ID, "magic_mushroom");

    @SubscribeEvent
    public static void addToTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
            Item item = BuiltInRegistries.ITEM.getValue(MAGIC_MUSHROOM_ID);
            if (item != null) event.accept(item);
        }
    }
}
