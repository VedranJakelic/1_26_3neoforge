package net.beamex.shamaschizm.livingsmoke;

import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/** Self-contained, additive registration. No changes to existing entity or mixin registries. */
@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class LivingSmokeRegistration {
    public static EntityType<LivingSmoke> TYPE;
    public static EntityType<Kugelblitz> KUGELBLITZ;
    public static EntityType<FallingPearl> FALLING_PEARL;
    public static Item EGG;

    @SubscribeEvent public static void register(RegisterEvent event) {
        event.register(Registries.ENTITY_TYPE, helper -> {
            var id = Shamaschizm.id("living_smoke");
            TYPE = EntityType.Builder.<LivingSmoke>of(LivingSmoke::new, MobCategory.MONSTER)
                    .sized(.72F, 1.2F).clientTrackingRange(12).updateInterval(1).noLootTable()
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, id));
            helper.register(id, TYPE);
            var orbId = Shamaschizm.id("kugelblitz");
            KUGELBLITZ = EntityType.Builder.<Kugelblitz>of(Kugelblitz::new, MobCategory.MISC)
                    .sized(.7F, .7F).clientTrackingRange(12).updateInterval(1).noLootTable()
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, orbId));
            helper.register(orbId, KUGELBLITZ);
            var pearlId = Shamaschizm.id("living_smoke_pearl");
            FALLING_PEARL = EntityType.Builder.<FallingPearl>of(FallingPearl::new, MobCategory.MISC)
                    .sized(.16F, .16F).clientTrackingRange(10).updateInterval(1).noLootTable()
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, pearlId));
            helper.register(pearlId, FALLING_PEARL);
        });
        event.register(Registries.ITEM, helper -> {
            var id = Shamaschizm.id("living_smoke_spawn_egg");
            EGG = new SpawnEggItem(new Item.Properties().spawnEgg(TYPE)
                    .setId(ResourceKey.create(Registries.ITEM, id)));
            helper.register(id, EGG);
        });
    }
    @SubscribeEvent public static void attributes(EntityAttributeCreationEvent e) {
        e.put(TYPE, LivingSmoke.attributes().build());
    }
    @SubscribeEvent public static void tab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) event.accept(EGG);
    }
    private LivingSmokeRegistration() {}
}
