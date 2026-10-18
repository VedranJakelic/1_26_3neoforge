package net.beamex.shamaschizm.entity;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.entity.custom.GateEntity;
import net.beamex.shamaschizm.item.GateSpawnEggItem;
import net.beamex.shamaschizm.world.block.GateBarrierBlock;
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

/** Self-contained registration for the animated gate feature. */
@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class GateRegistration {
    public static final Identifier ENTITY_ID = Shamaschizm.id("gate");
    public static final Identifier BARRIER_ID = Shamaschizm.id("gate_barrier");
    public static final Identifier SPAWN_EGG_ID = Shamaschizm.id("gate_spawn_egg");
    public static final Identifier OPEN_SOUND_ID = Identifier.fromNamespaceAndPath("gate", "gate1");

    public static EntityType<GateEntity> GATE;
    public static Block GATE_BARRIER;
    public static Item GATE_SPAWN_EGG;
    public static SoundEvent GATE_OPEN;

    private GateRegistration() {}

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(Registries.BLOCK, helper -> {
            ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, BARRIER_ID);
            GATE_BARRIER = new GateBarrierBlock(BlockBehaviour.Properties.of()
                    .setId(key)
                    .mapColor(MapColor.NONE)
                    .strength(-1.0F, 3_600_000.0F)
                    .sound(SoundType.EMPTY)
                    .noOcclusion()
                    .noLootTable()
                    .isValidSpawn((state, level, pos, entityType) -> false)
                    .isRedstoneConductor((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos, box) -> false)
                    .pushReaction(PushReaction.IMMOVEABLE));
            helper.register(BARRIER_ID, GATE_BARRIER);
        });

        event.register(Registries.ENTITY_TYPE, helper -> {
            ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, ENTITY_ID);
            GATE = EntityType.Builder.<GateEntity>of(GateEntity::new, MobCategory.MISC)
                    .sized(3.0F, 3.0F)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .build(key);
            helper.register(ENTITY_ID, GATE);
        });

        event.register(Registries.SOUND_EVENT, helper -> {
            GATE_OPEN = SoundEvent.createVariableRangeEvent(OPEN_SOUND_ID);
            helper.register(OPEN_SOUND_ID, GATE_OPEN);
        });

        event.register(Registries.ITEM, helper -> {
            ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SPAWN_EGG_ID);
            GATE_SPAWN_EGG = new GateSpawnEggItem(new Item.Properties()
                    .spawnEgg(GATE)
                    .setId(key));
            helper.register(SPAWN_EGG_ID, GATE_SPAWN_EGG);
        });
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(GATE, GateEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(GATE_SPAWN_EGG);
        }
    }
}
