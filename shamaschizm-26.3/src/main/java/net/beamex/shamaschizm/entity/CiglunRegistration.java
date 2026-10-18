package net.beamex.shamaschizm.entity;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.entity.custom.CiglunEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Self-contained common registration so the feature does not replace central mod files. */
@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class CiglunRegistration {
    public static final Identifier ENTITY_ID = Shamaschizm.id("ciglun");
    public static final Identifier SPAWN_EGG_ID = Shamaschizm.id("ciglun_spawn_egg");
    public static final Identifier CIGLUN_3_ID = Identifier.fromNamespaceAndPath("ciglun", "ciglun3");
    public static final Identifier CIGLUN_4_ID = Identifier.fromNamespaceAndPath("ciglun", "ciglun4");
    public static final Identifier LASER_SOUND_ID = Identifier.fromNamespaceAndPath("ciglun", "ciglun6");

    public static EntityType<CiglunEntity> CIGLUN;
    public static SoundEvent CIGLUN_3;
    public static SoundEvent CIGLUN_4;
    public static SoundEvent CIGLUN_LASER;
    public static Item CIGLUN_SPAWN_EGG;

    private CiglunRegistration() {}

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(Registries.ENTITY_TYPE, helper -> {
            ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, ENTITY_ID);
            CIGLUN = EntityType.Builder.<CiglunEntity>of(CiglunEntity::new, MobCategory.CREATURE)
                    .sized(1.0F, 1.0F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build(key);
            helper.register(ENTITY_ID, CIGLUN);
        });

        event.register(Registries.SOUND_EVENT, helper -> {
            CIGLUN_3 = SoundEvent.createVariableRangeEvent(CIGLUN_3_ID);
            CIGLUN_4 = SoundEvent.createVariableRangeEvent(CIGLUN_4_ID);
            CIGLUN_LASER = SoundEvent.createVariableRangeEvent(LASER_SOUND_ID);
            helper.register(CIGLUN_3_ID, CIGLUN_3);
            helper.register(CIGLUN_4_ID, CIGLUN_4);
            helper.register(LASER_SOUND_ID, CIGLUN_LASER);
        });

        event.register(Registries.ITEM, helper -> {
            ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SPAWN_EGG_ID);
            CIGLUN_SPAWN_EGG = new SpawnEggItem(new Item.Properties()
                    .spawnEgg(CIGLUN)
                    .setId(key));
            helper.register(SPAWN_EGG_ID, CIGLUN_SPAWN_EGG);
        });
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(CIGLUN, CiglunEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(CIGLUN_SPAWN_EGG);
        }
    }
}
