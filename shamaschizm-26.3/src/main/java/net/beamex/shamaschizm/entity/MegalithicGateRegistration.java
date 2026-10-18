package net.beamex.shamaschizm.entity;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.entity.custom.MegalithicGateEntity;
import net.beamex.shamaschizm.item.MegalithicGateSpawnEggItem;
import net.beamex.shamaschizm.world.block.BrazierBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Self-contained registration for the brazier puzzle and megalithic gate. */
@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class MegalithicGateRegistration {
    public static final Identifier GATE_ID = Shamaschizm.id("megalithic_gate");
    public static final Identifier EGG_ID = Shamaschizm.id("megalithic_gate_spawn_egg");
    public static final Identifier BRAZIER_ID = Shamaschizm.id("brazier");
    public static final Identifier OPEN_SOUND_ID = Shamaschizm.id("meggate");

    public static EntityType<MegalithicGateEntity> GATE;
    public static Item GATE_SPAWN_EGG;
    public static BrazierBlock BRAZIER;
    public static Item BRAZIER_ITEM;
    public static SoundEvent OPEN_SOUND;

    private MegalithicGateRegistration() {}

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(Registries.BLOCK, helper -> {
            ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, BRAZIER_ID);
            BRAZIER = new BrazierBlock(BlockBehaviour.Properties.of()
                    .setId(key)
                    .mapColor(MapColor.STONE)
                    .strength(3.0F, 8.0F)
                    .sound(SoundType.STONE)
                    .lightLevel(state -> state.getValue(BrazierBlock.LIT) ? 15 : 0)
                    .noOcclusion());
            helper.register(BRAZIER_ID, BRAZIER);
        });

        event.register(Registries.ENTITY_TYPE, helper -> {
            ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, GATE_ID);
            GATE = EntityType.Builder.<MegalithicGateEntity>of(MegalithicGateEntity::new, MobCategory.MISC)
                    .sized(3.0F, 3.0F)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .build(key);
            helper.register(GATE_ID, GATE);
        });

        event.register(Registries.SOUND_EVENT, helper -> {
            OPEN_SOUND = SoundEvent.createVariableRangeEvent(OPEN_SOUND_ID);
            helper.register(OPEN_SOUND_ID, OPEN_SOUND);
        });

        event.register(Registries.ITEM, helper -> {
            ResourceKey<Item> brazierKey = ResourceKey.create(Registries.ITEM, BRAZIER_ID);
            BRAZIER_ITEM = new BlockItem(BRAZIER, new Item.Properties()
                    .setId(brazierKey).useBlockDescriptionPrefix());
            helper.register(BRAZIER_ID, BRAZIER_ITEM);

            ResourceKey<Item> eggKey = ResourceKey.create(Registries.ITEM, EGG_ID);
            GATE_SPAWN_EGG = new MegalithicGateSpawnEggItem(new Item.Properties()
                    .spawnEgg(GATE).setId(eggKey));
            helper.register(EGG_ID, GATE_SPAWN_EGG);
        });
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(GATE, MegalithicGateEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) event.accept(BRAZIER_ITEM);
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) event.accept(GATE_SPAWN_EGG);
    }
}
