package net.beamex.shamaschizm.event;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.world.block.PoisonTrapBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Self-contained block and item registration for the poison trap. */
@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class PoisonTrapBootstrap {
    public static final Identifier ID =
            Identifier.fromNamespaceAndPath(Shamaschizm.MOD_ID, "poison_trap");

    public static PoisonTrapBlock BLOCK;
    public static Item ITEM;
    public static SoundEvent ACTIVATION_SOUND;

    private PoisonTrapBootstrap() {}

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(Registries.SOUND_EVENT, helper -> {
            ACTIVATION_SOUND = SoundEvent.createVariableRangeEvent(ID);
            helper.register(ID, ACTIVATION_SOUND);
        });

        event.register(Registries.BLOCK, helper -> {
            BLOCK = new PoisonTrapBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, ID))
                    .mapColor(MapColor.METAL)
                    .strength(3.5F, 6.0F)
                    .sound(SoundType.METAL));
            helper.register(ID, BLOCK);
        });

        event.register(Registries.ITEM, helper -> {
            ITEM = new BlockItem(BLOCK, new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, ID))
                    .useBlockDescriptionPrefix());
            helper.register(ID, ITEM);
        });
    }

    @SubscribeEvent
    public static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
            event.accept(ITEM);
        }
    }
}
