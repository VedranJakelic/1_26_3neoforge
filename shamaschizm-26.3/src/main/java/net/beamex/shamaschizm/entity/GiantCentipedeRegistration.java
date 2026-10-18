package net.beamex.shamaschizm.entity;

import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class GiantCentipedeRegistration {
    public static Item SPAWN_EGG;

    private GiantCentipedeRegistration() {}

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(Registries.ITEM, helper -> {
            var id = Shamaschizm.id("giant_centipede_spawn_egg");
            ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
            SPAWN_EGG = new SpawnEggItem(new Item.Properties()
                    .spawnEgg(ModEntities.GIANT_CENTIPEDE.get()).setId(key));
            helper.register(id, SPAWN_EGG);
        });
    }

    @SubscribeEvent
    public static void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) event.accept(SPAWN_EGG);
    }
}
