package net.beamex.shamaschizm.ritual;

import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class RitualRegistry {
    private RitualRegistry() {}

    public static final Identifier BRAIN_ID = Shamaschizm.id("corrupted_brain");
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, Shamaschizm.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(BuiltInRegistries.ITEM, Shamaschizm.MOD_ID);

    private static final ResourceKey<EntityType<?>> QUESTION_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, Shamaschizm.id("question_villager"));
    public static final DeferredHolder<EntityType<?>, EntityType<QuestionVillager>> QUESTION_VILLAGER =
            ENTITY_TYPES.register("question_villager", () -> EntityType.Builder
                    .<QuestionVillager>of(QuestionVillager::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .build(QUESTION_KEY));

    private static final ResourceKey<Item> EGG_KEY =
            ResourceKey.create(Registries.ITEM, Shamaschizm.id("question_villager_spawn_egg"));
    public static final DeferredHolder<Item, SpawnEggItem> EGG =
            ITEMS.register("question_villager_spawn_egg", () ->
                    new SpawnEggItem(new Item.Properties()
                            .setId(EGG_KEY)
                            .spawnEgg(QUESTION_VILLAGER.get())
                            .stacksTo(64)));
}
