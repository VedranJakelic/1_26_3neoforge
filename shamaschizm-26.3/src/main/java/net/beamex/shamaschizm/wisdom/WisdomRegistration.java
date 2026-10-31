package net.beamex.shamaschizm.wisdom;

import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.food.Foods;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class WisdomRegistration {
    public static EntityType<TaintedWisdom> TYPE;
    public static Item BRAIN, EGG;
    @SubscribeEvent public static void register(RegisterEvent event) {
        event.register(Registries.ENTITY_TYPE, helper -> {
            var id = Shamaschizm.id("tainted_wisdom");
            TYPE = EntityType.Builder.<TaintedWisdom>of(TaintedWisdom::new, MobCategory.MONSTER)
                    .sized(.9F, .75F).clientTrackingRange(10).updateInterval(1)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, id));
            helper.register(id, TYPE);
        });
        event.register(Registries.ITEM, helper -> {
            var brain = Shamaschizm.id("corrupted_brain");
            BRAIN = new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, brain))
                    .food(Foods.ROTTEN_FLESH, Consumables.ROTTEN_FLESH));
            helper.register(brain, BRAIN);
            var egg = Shamaschizm.id("tainted_wisdom_spawn_egg");
            EGG = new SpawnEggItem(new Item.Properties().spawnEgg(TYPE).setId(ResourceKey.create(Registries.ITEM, egg)));
            helper.register(egg, EGG);
        });
    }
    @SubscribeEvent public static void attributes(EntityAttributeCreationEvent event) { event.put(TYPE, TaintedWisdom.attributes().build()); }
    @SubscribeEvent public static void tab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) event.accept(EGG);
        if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) event.accept(BRAIN);
    }
}
