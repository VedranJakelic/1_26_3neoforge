package net.beamex.shamaschizm.entity;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.entity.custom.CoffinEntity;
import net.beamex.shamaschizm.entity.custom.SpellVisualEntity;
import net.beamex.shamaschizm.item.CoffinSpawnEggItem;
import net.beamex.shamaschizm.world.block.CoffinCollisionBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class CoffinRegistration {
    public static final Identifier COFFIN_ID = Shamaschizm.id("coffin");
    public static final Identifier SPELL_ID = Shamaschizm.id("coffin_spell");
    public static final Identifier COLLISION_ID = Shamaschizm.id("coffin_collision");
    public static final Identifier SPAWN_EGG_ID = Shamaschizm.id("coffin_spawn_egg");
    public static final Identifier OPEN_SOUND_ID = Shamaschizm.id("coffin_open");

    public static EntityType<CoffinEntity> COFFIN;
    public static EntityType<SpellVisualEntity> SPELL;
    public static Block COLLISION;
    public static Item SPAWN_EGG;
    public static SoundEvent OPEN_SOUND;

    private CoffinRegistration() {}

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(Registries.BLOCK, helper -> {
            ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, COLLISION_ID);
            COLLISION = new CoffinCollisionBlock(BlockBehaviour.Properties.of()
                    .setId(key)
                    .mapColor(MapColor.NONE)
                    .strength(-1.0F, 3_600_000.0F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .noLootTable()
                    .isValidSpawn((state, level, pos, type) -> false)
                    .isRedstoneConductor((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos, box) -> false)
                    .pushReaction(PushReaction.IMMOVEABLE));
            helper.register(COLLISION_ID, COLLISION);
        });

        event.register(Registries.ENTITY_TYPE, helper -> {
            ResourceKey<EntityType<?>> coffinKey = ResourceKey.create(Registries.ENTITY_TYPE, COFFIN_ID);
            COFFIN = EntityType.Builder.<CoffinEntity>of(CoffinEntity::new, MobCategory.MISC)
                    .sized(0.85F, 0.6F)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .noLootTable()
                    .build(coffinKey);
            helper.register(COFFIN_ID, COFFIN);

            ResourceKey<EntityType<?>> spellKey = ResourceKey.create(Registries.ENTITY_TYPE, SPELL_ID);
            SPELL = EntityType.Builder.<SpellVisualEntity>of(SpellVisualEntity::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .noLootTable()
                    .build(spellKey);
            helper.register(SPELL_ID, SPELL);
        });

        event.register(Registries.SOUND_EVENT, helper -> {
            OPEN_SOUND = SoundEvent.createVariableRangeEvent(OPEN_SOUND_ID);
            helper.register(OPEN_SOUND_ID, OPEN_SOUND);
        });

        event.register(Registries.ITEM, helper -> {
            ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SPAWN_EGG_ID);
            SPAWN_EGG = new CoffinSpawnEggItem(new Item.Properties().spawnEgg(COFFIN).setId(key));
            helper.register(SPAWN_EGG_ID, SPAWN_EGG);
        });
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(COFFIN, CoffinEntity.createAttributes().build());
        event.put(SPELL, SpellVisualEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) event.accept(SPAWN_EGG);
    }
}
