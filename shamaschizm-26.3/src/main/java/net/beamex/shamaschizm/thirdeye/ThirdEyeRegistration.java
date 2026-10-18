package net.beamex.shamaschizm.thirdeye;

import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Self-contained registration and left-click hook for the Third Eye. */
@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class ThirdEyeRegistration {
    public static final Identifier ID = Shamaschizm.id("third_eye_of_providence");
    public static ThirdEyeBlock BLOCK;
    public static BlockEntityType<ThirdEyeBlockEntity> BLOCK_ENTITY;
    public static Item ITEM;

    private ThirdEyeRegistration() {}

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(Registries.BLOCK, helper -> {
            ResourceKey<net.minecraft.world.level.block.Block> key =
                    ResourceKey.create(Registries.BLOCK, ID);
            BLOCK = new ThirdEyeBlock(BlockBehaviour.Properties.of()
                    .setId(key)
                    .mapColor(MapColor.STONE)
                    .strength(3.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(ThirdEyeBlock.STAGE) == 0 ? 0 : 12));
            helper.register(ID, BLOCK);
        });

        event.register(Registries.BLOCK_ENTITY_TYPE, helper -> {
            BLOCK_ENTITY = new BlockEntityType<>(ThirdEyeBlockEntity::new, BLOCK);
            helper.register(ID, BLOCK_ENTITY);
        });

        event.register(Registries.ITEM, helper -> {
            ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, ID);
            ITEM = new BlockItem(BLOCK, new Item.Properties()
                    .setId(key)
                    .useBlockDescriptionPrefix());
            helper.register(ID, ITEM);
        });
    }

    @SubscribeEvent
    public static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) event.accept(ITEM);
    }

    @SubscribeEvent
    public static void leftClick(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) return;
        var state = event.getLevel().getBlockState(event.getPos());
        if (state.is(BLOCK) && state.getValue(ThirdEyeBlock.STAGE) == 0) {
            BLOCK.activateForPlayer(state, event.getLevel(), event.getPos(), event.getEntity());
            // The first mining swing is the activation gesture, not block damage.
            event.setCanceled(true);
        }
    }
}
