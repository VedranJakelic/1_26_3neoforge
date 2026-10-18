package net.beamex.shamaschizm.entity;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.entity.custom.FrogGodEntity;
import net.beamex.shamaschizm.item.FrogGodSpawnEggItem;
import net.beamex.shamaschizm.world.block.FrogFireLightBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Self-contained common registration for the frog god, its sounds, egg, and temporary light. */
@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class FrogGodRegistration {
    public static final Identifier ENTITY_ID = Shamaschizm.id("frog_god");
    public static final Identifier EGG_ID = Shamaschizm.id("frog_god_spawn_egg");
    public static final Identifier LIGHT_ID = Shamaschizm.id("frog_fire_light");
    public static final Identifier CROAK_ID = Shamaschizm.id("frog_croak");
    public static final Identifier FROG_1_ID = Shamaschizm.id("frog_1");
    public static final Identifier FROG_FIRE_ID = Shamaschizm.id("frog_fire");

    public static EntityType<FrogGodEntity> FROG_GOD;
    public static Item SPAWN_EGG;
    public static FrogFireLightBlock FIRE_LIGHT;
    public static SoundEvent CROAK;
    public static SoundEvent FROG_1;
    public static SoundEvent FROG_FIRE;

    private FrogGodRegistration() {}

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(Registries.BLOCK, helper -> {
            ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, LIGHT_ID);
            FIRE_LIGHT = new FrogFireLightBlock(BlockBehaviour.Properties.of()
                    .setId(key).replaceable().noCollision().noOcclusion().noLootTable()
                    .strength(0.0F).lightLevel(state -> 6)
                    .pushReaction(PushReaction.POPPED)
                    .isValidSpawn((state, level, pos, type) -> false));
            helper.register(LIGHT_ID, FIRE_LIGHT);
        });

        event.register(Registries.ENTITY_TYPE, helper -> {
            ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, ENTITY_ID);
            FROG_GOD = EntityType.Builder.<FrogGodEntity>of(FrogGodEntity::new, MobCategory.CREATURE)
                    .sized(1.4F, 1.6F)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .fireImmune()
                    .noLootTable()
                    .build(key);
            helper.register(ENTITY_ID, FROG_GOD);
        });

        event.register(Registries.SOUND_EVENT, helper -> {
            CROAK = SoundEvent.createVariableRangeEvent(CROAK_ID);
            FROG_1 = SoundEvent.createVariableRangeEvent(FROG_1_ID);
            FROG_FIRE = SoundEvent.createVariableRangeEvent(FROG_FIRE_ID);
            helper.register(CROAK_ID, CROAK);
            helper.register(FROG_1_ID, FROG_1);
            helper.register(FROG_FIRE_ID, FROG_FIRE);
        });

        event.register(Registries.ITEM, helper -> {
            ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, EGG_ID);
            SPAWN_EGG = new FrogGodSpawnEggItem(new Item.Properties().spawnEgg(FROG_GOD).setId(key));
            helper.register(EGG_ID, SPAWN_EGG);
        });
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(FROG_GOD, FrogGodEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) event.accept(SPAWN_EGG);
    }
}
