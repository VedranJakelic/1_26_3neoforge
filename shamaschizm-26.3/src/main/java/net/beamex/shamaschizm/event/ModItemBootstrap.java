// src/main/java/net/beamex/shamaschizm/event/ModItemBootstrap.java
package net.beamex.shamaschizm.event;

import net.beamex.shamaschizm.Shamaschizm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.List;
import net.minecraft.world.level.storage.loot.LootParams;




@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class ModItemBootstrap {

    private ModItemBootstrap() {}

    // ---- IDs ----
    public static final Identifier MAGIC_MUSHROOM_ID =
            Identifier.fromNamespaceAndPath(Shamaschizm.MOD_ID, "magic_mushroom");
    public static final Identifier SOUL_ICON_ID =
            Identifier.fromNamespaceAndPath(Shamaschizm.MOD_ID, "soul_icon");
    public static final Identifier HIGH_SOUL_ICON_ID =
            Identifier.fromNamespaceAndPath(Shamaschizm.MOD_ID, "high_soul_icon");
    public static final Identifier SCHIZM_PORTAL_ID =
            Identifier.fromNamespaceAndPath(Shamaschizm.MOD_ID, "schizm_portal");
    public static final Identifier SCHIZM_BLOCK_ID =
            Identifier.fromNamespaceAndPath(Shamaschizm.MOD_ID, "schizm_block");

    // Ancient blueprint only (armor intentionally NOT registered here)
    public static final Identifier ANCIENT_BLUEPRINT_ID =
            Identifier.fromNamespaceAndPath(Shamaschizm.MOD_ID, "ancient_blueprint");
    // Ancient armor ids
    public static final Identifier ANCIENT_HELMET_ID =
            Identifier.fromNamespaceAndPath(Shamaschizm.MOD_ID, "ancient_helmet");
    public static final Identifier ANCIENT_CHESTPLATE_ID =
            Identifier.fromNamespaceAndPath(Shamaschizm.MOD_ID, "ancient_chestplate");
    public static final Identifier ANCIENT_LEGGINGS_ID =
            Identifier.fromNamespaceAndPath(Shamaschizm.MOD_ID, "ancient_leggings");
    public static final Identifier ANCIENT_BOOTS_ID =
            Identifier.fromNamespaceAndPath(Shamaschizm.MOD_ID, "ancient_boots");

    public static final ResourceKey<Item> ANCIENT_HELMET_KEY =
            ResourceKey.create(Registries.ITEM, ANCIENT_HELMET_ID);
    public static final ResourceKey<Item> ANCIENT_CHESTPLATE_KEY =
            ResourceKey.create(Registries.ITEM, ANCIENT_CHESTPLATE_ID);
    public static final ResourceKey<Item> ANCIENT_LEGGINGS_KEY =
            ResourceKey.create(Registries.ITEM, ANCIENT_LEGGINGS_ID);
    public static final ResourceKey<Item> ANCIENT_BOOTS_KEY =
            ResourceKey.create(Registries.ITEM, ANCIENT_BOOTS_ID);


    // ---- Instances ----
    public static Block SCHIZM_BLOCK;
    public static Block MAGIC_MUSHROOM_BLOCK;
    public static Block SCHIZM_PORTAL_BLOCK;

    public static Item MAGIC_MUSHROOM;
    public static Item SOUL_ICON;
    public static Item HIGH_SOUL_ICON;

    public static Item ANCIENT_BLUEPRINT;
    // Ancient armor items (assigned during item bootstrap)
// Ancient armor items (assigned during item bootstrap)
    public static Item ANCIENT_HELMET;
    public static Item ANCIENT_CHESTPLATE;
    public static Item ANCIENT_LEGGINGS;
    public static Item ANCIENT_BOOTS;



    // Simple plant block used by the mushroom item/block
    public static final class MagicMushroomBlock extends BushBlock {
        private static final VoxelShape SHAPE = Block.box(5.0, 0.0, 5.0, 11.0, 7.0, 11.0);
        public MagicMushroomBlock(BlockBehaviour.Properties props) { super(props); }
        @Override public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }
        @Override
        protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
            if (state.is(BlockTags.OVERRIDES_MUSHROOM_LIGHT_REQUIREMENT)) return true;
            if (level instanceof LevelReader reader && Block.canSupportCenter(reader, pos, Direction.UP)) {
                return reader.getRawBrightness(pos.above(), 0) < 15;
            }
            return false;
        }
        @Override
        protected List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
            return List.of(new ItemStack(MAGIC_MUSHROOM.asItem()));
        }
    }

    // Minimal shiny block-item wrapper for the mushroom
    public static final class MagicMushroomBlockItem extends BlockItem {
        public MagicMushroomBlockItem(Block block, Properties props) { super(block, props); }
        @Override public boolean isFoil(ItemStack stack) { return true; } // why: visual cue
    }

    @SubscribeEvent
    public static void onRegister(final RegisterEvent event) {
        event.register(Registries.BLOCK, helper -> {
            // === schizm_block ===
            BlockBehaviour.Properties schizmProps = BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(3.0f, 9.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.NETHER_GOLD_ORE)
                    .setId(ResourceKey.create(Registries.BLOCK, SCHIZM_BLOCK_ID));
            SCHIZM_BLOCK = new Block(schizmProps);
            helper.register(SCHIZM_BLOCK_ID, SCHIZM_BLOCK);

            // === magic_mushroom block ===
            BlockBehaviour.Properties mushProps = BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollision()
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .setId(ResourceKey.create(Registries.BLOCK, MAGIC_MUSHROOM_ID));
            MAGIC_MUSHROOM_BLOCK = new MagicMushroomBlock(mushProps);
            helper.register(MAGIC_MUSHROOM_ID, MAGIC_MUSHROOM_BLOCK);

            // === schizm_portal block ===
            final Identifier SCHIZM_PORTAL_ID =
                    Identifier.fromNamespaceAndPath(Shamaschizm.MOD_ID, "schizm_portal");
            final ResourceKey<Block> SCHIZM_PORTAL_KEY =
                    ResourceKey.create(Registries.BLOCK, SCHIZM_PORTAL_ID);

            Block.Properties portalProps = Block.Properties.of()
                    .noOcclusion()
                    .strength(-1.0F, 3_600_000.0F)
                    .setId(SCHIZM_PORTAL_KEY); // <- REQUIRED

            SCHIZM_PORTAL_BLOCK = new net.beamex.shamaschizm.world.block.SchizmPortalBlock(portalProps);
            helper.register(SCHIZM_PORTAL_ID, SCHIZM_PORTAL_BLOCK);
            net.beamex.shamaschizm.registry.ModBlocks.SCHIZM_PORTAL = SCHIZM_PORTAL_BLOCK;

        });

        // File: src/main/java/net/beamex/shamaschizm/event/ModItemBootstrap.java
// REPLACE the whole ITEMS registration lambda body with this version.
// (Keep your imports and class header as-is.)

        event.register(Registries.ITEM, helper -> {
            // === magic_mushroom (EDIBLE ITEM, not BlockItem) ===
            Item.Properties mmProps = new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, MAGIC_MUSHROOM_ID))
                    .rarity(net.minecraft.world.item.Rarity.UNCOMMON); // restore non-common
            MAGIC_MUSHROOM = new net.beamex.shamaschizm.item.custom.MagicMushroomItem(
                    MAGIC_MUSHROOM_BLOCK,
                    mmProps
            );
            helper.register(MAGIC_MUSHROOM_ID, MAGIC_MUSHROOM);

            // === soul_icon ===
            Item.Properties soulProps = new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, SOUL_ICON_ID));
            SOUL_ICON = new net.beamex.shamaschizm.item.SoulIconItem(soulProps);
            helper.register(SOUL_ICON_ID, SOUL_ICON);

            // === high_soul_icon (UNCOMMON) ===
            Item.Properties highSoulProps = new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, HIGH_SOUL_ICON_ID))
                    .rarity(net.minecraft.world.item.Rarity.UNCOMMON);
            HIGH_SOUL_ICON = new net.beamex.shamaschizm.item.HighSoulIconItem(highSoulProps);
            helper.register(HIGH_SOUL_ICON_ID, HIGH_SOUL_ICON);

            // === ancient_blueprint ===
            // AFTER (fix: assign the static field used by the recipe)
            final Item.Properties blueprintProps = new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, ANCIENT_BLUEPRINT_ID))
                    .rarity(Rarity.RARE);

            ModItemBootstrap.ANCIENT_BLUEPRINT =
                    new net.beamex.shamaschizm.ancient.AncientBlueprintItem(blueprintProps);

            helper.register(ANCIENT_BLUEPRINT_ID, ModItemBootstrap.ANCIENT_BLUEPRINT);


// === Ancient armor (wearable) ===
            var HELM_ID  = net.minecraft.resources.Identifier.fromNamespaceAndPath(Shamaschizm.MOD_ID, "ancient_helmet");
            var CHEST_ID = net.minecraft.resources.Identifier.fromNamespaceAndPath(Shamaschizm.MOD_ID, "ancient_chestplate");
            var LEGS_ID  = net.minecraft.resources.Identifier.fromNamespaceAndPath(Shamaschizm.MOD_ID, "ancient_leggings");
            var BOOTS_ID = net.minecraft.resources.Identifier.fromNamespaceAndPath(Shamaschizm.MOD_ID, "ancient_boots");

            var HELM_KEY  = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ITEM, HELM_ID);
            var CHEST_KEY = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ITEM, CHEST_ID);
            var LEGS_KEY  = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ITEM, LEGS_ID);
            var BOOTS_KEY = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ITEM, BOOTS_ID);

            // Plain items for now; you’ll swap to real armor later.
            var helm  = new net.minecraft.world.item.Item(new net.minecraft.world.item.Item.Properties().stacksTo(1).setId(HELM_KEY));
            var chest = new net.minecraft.world.item.Item(new net.minecraft.world.item.Item.Properties().stacksTo(1).setId(CHEST_KEY));
            var legs  = new net.minecraft.world.item.Item(new net.minecraft.world.item.Item.Properties().stacksTo(1).setId(LEGS_KEY));
            var boots = new net.minecraft.world.item.Item(new net.minecraft.world.item.Item.Properties().stacksTo(1).setId(BOOTS_KEY));

            helper.register(HELM_KEY,  helm);
            helper.register(CHEST_KEY, chest);
            helper.register(LEGS_KEY,  legs);
            helper.register(BOOTS_KEY, boots);

            // === block items ===
            BlockItem schizmBlockItem = new BlockItem(SCHIZM_BLOCK,
                    new Item.Properties().setId(ResourceKey.create(Registries.ITEM, SCHIZM_BLOCK_ID)));
            helper.register(SCHIZM_BLOCK_ID, schizmBlockItem);

            BlockItem schizmPortalItem = new BlockItem(SCHIZM_PORTAL_BLOCK,
                    new Item.Properties().setId(ResourceKey.create(Registries.ITEM, SCHIZM_PORTAL_ID)));
            helper.register(SCHIZM_PORTAL_ID, schizmPortalItem);
        });



// OPTIONAL CLEANUP inside the same file:
// 1) Remove the inner classes MagicMushroomBlock and MagicMushroomBlockItem.
// 2) Remove the "=== magic_mushroom block ===" section in the BLOCKS lambda, unless you need a plant block.
//    If you do want a world-placed plant as well, register it under a DIFFERENT id (e.g. "magic_mushroom_block")
//    so the edible item id "magic_mushroom" remains a plain Item, not a BlockItem.

    }


}
