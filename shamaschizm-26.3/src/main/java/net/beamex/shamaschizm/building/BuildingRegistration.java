package net.beamex.shamaschizm.building;

import java.util.LinkedHashMap;
import java.util.Map;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class BuildingRegistration {
    private static final Map<String, Block> BRICKS = new LinkedHashMap<>();
    private static final Map<String, Item> ITEMS = new LinkedHashMap<>();
    public static SpanningWebBlock WEB;
    public static Item WEB_ITEM;
    public static net.minecraft.world.level.block.entity.BlockEntityType<WebBlockEntity> WEB_BLOCK_ENTITY;
    public static net.minecraft.world.entity.EntityType<SpanningWebEntity> WEB_ENTITY;

    private static BlockBehaviour.Properties stone(String name) {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS)
                .setId(ResourceKey.create(Registries.BLOCK, Shamaschizm.id(name)));
    }

    @SubscribeEvent public static void register(RegisterEvent event) {
        event.register(Registries.ENTITY_TYPE, helper -> {
            var id = Shamaschizm.id("spanning_web");
            WEB_ENTITY = net.minecraft.world.entity.EntityType.Builder.<SpanningWebEntity>of(
                    SpanningWebEntity::new, net.minecraft.world.entity.MobCategory.MISC)
                    .sized(5, 5).clientTrackingRange(10).updateInterval(20).noLootTable()
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, id));
            helper.register(id, WEB_ENTITY);
        });
        event.register(Registries.BLOCK, helper -> {
            Block base = new Block(stone("dungeon_bricks"));
            BRICKS.put("dungeon_bricks", base);
            BRICKS.put("dungeon_brick_slab", new SlabBlock(stone("dungeon_brick_slab")));
            BRICKS.put("dungeon_brick_stairs", new BrickStairs(base.defaultBlockState(), stone("dungeon_brick_stairs")));
            BRICKS.put("dungeon_brick_wall", new WallBlock(stone("dungeon_brick_wall")));
            BRICKS.put("dungeon_brick_fence", new FenceBlock(stone("dungeon_brick_fence")));
            BRICKS.put("dungeon_brick_shelf", new ShelfBlock(stone("dungeon_brick_shelf").noOcclusion()));
            BRICKS.forEach((name, block) -> helper.register(Shamaschizm.id(name), block));
            WEB = new SpanningWebBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.COBWEB)
                    .setId(ResourceKey.create(Registries.BLOCK, Shamaschizm.id("spanning_web")))
                    .noCollision().noOcclusion().dynamicShape().randomTicks());
            helper.register(Shamaschizm.id("spanning_web"), WEB);
        });
        event.register(Registries.BLOCK_ENTITY_TYPE, helper -> {
            WEB_BLOCK_ENTITY = new net.minecraft.world.level.block.entity.BlockEntityType<>(WebBlockEntity::new, java.util.Set.of(WEB));
            helper.register(Shamaschizm.id("spanning_web"), WEB_BLOCK_ENTITY);
        });
        event.register(Registries.ITEM, helper -> {
            BRICKS.forEach((name, block) -> ITEMS.put(name, new BlockItem(block,
                    new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Shamaschizm.id(name))))));
            ITEMS.put("spanning_web", WEB_ITEM = new SpanningWebItem(new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, Shamaschizm.id("spanning_web")))));
            ITEMS.forEach((name, item) -> helper.register(Shamaschizm.id(name), item));
        });
    }

    @SubscribeEvent public static void shelves(BlockEntityTypeAddBlocksEvent event) {
        event.modify(BlockEntityTypes.SHELF, BRICKS.get("dungeon_brick_shelf"));
    }

    @SubscribeEvent public static void tabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().identifier().equals(net.minecraft.resources.Identifier.withDefaultNamespace("building_blocks")))
            ITEMS.forEach((name, item) -> { if (!name.equals("spanning_web") && !name.endsWith("shelf")) event.accept(item); });
        if (event.getTabKey().identifier().equals(net.minecraft.resources.Identifier.withDefaultNamespace("functional_blocks"))) {
            event.accept(ITEMS.get("dungeon_brick_shelf"));
            event.accept(ITEMS.get("spanning_web"));
        }
    }

    private static final class BrickStairs extends StairBlock {
        private BrickStairs(BlockState base, BlockBehaviour.Properties properties) { super(base, properties); }
    }
}
