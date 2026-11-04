package net.beamex.shamaschizm.banner;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Native banner behavior (placement, dye colours, data components, loom). */
@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class TatteredBannerRegistration {
    private static final Map<DyeColor, BannerBlock> STANDING = new LinkedHashMap<>();
    private static final Map<DyeColor, WallBannerBlock> WALL = new LinkedHashMap<>();
    private static final Map<DyeColor, Item> ITEMS = new LinkedHashMap<>();

    private TatteredBannerRegistration() {}

    /** 26.3-safe banner settings without depending on per-color Blocks constants. */
    private static BlockBehaviour.Properties bannerProperties(DyeColor color, Identifier id) {
        return BlockBehaviour.Properties.of()
                .setId(ResourceKey.create(Registries.BLOCK, id))
                .mapColor(color.getMapColor())
                .noCollision()
                .noOcclusion()
                .instabreak()
                .sound(SoundType.WOOL)
                .noLootTable();
    }

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(Registries.BLOCK, helper -> {
            for (DyeColor color : DyeColor.values()) {
                String base = "tattered_" + color.getSerializedName() + "_banner";
                Identifier standingId = Shamaschizm.id(base);
                Identifier wallId = Shamaschizm.id("tattered_" + color.getSerializedName() + "_wall_banner");
                BannerBlock standing = new TatteredStanding(color, bannerProperties(color, standingId));
                WallBannerBlock wall = new TatteredWall(color, standing, bannerProperties(color, wallId));
                STANDING.put(color, standing);
                WALL.put(color, wall);
                helper.register(standingId, standing);
                helper.register(wallId, wall);
            }
        });
        event.register(Registries.ITEM, helper -> {
            for (DyeColor color : DyeColor.values()) {
                Identifier id = Shamaschizm.id("tattered_" + color.getSerializedName() + "_banner");
                Item item = new BannerItem(STANDING.get(color), WALL.get(color), new Item.Properties()
                        .setId(ResourceKey.create(Registries.ITEM, id))
                        .useBlockDescriptionPrefix()
                        .stacksTo(16));
                ITEMS.put(color, item);
                helper.register(id, item);
            }
        });
    }

    @SubscribeEvent
    public static void extendVanillaBannerEntity(BlockEntityTypeAddBlocksEvent event) {
        List<Block> blocks = new ArrayList<>();
        blocks.addAll(STANDING.values());
        blocks.addAll(WALL.values());
        event.modify(BlockEntityTypes.BANNER, blocks.toArray(Block[]::new));
    }

    @SubscribeEvent
    public static void creative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().identifier().equals(Identifier.withDefaultNamespace("functional_blocks")))
            for (Item item : ITEMS.values()) event.accept(item);
    }

    // Vanilla banner block entities contain the pattern data, but vanilla loot tables
    // don't know these new IDs. Preserve every pattern and custom name on pickup.
    private static ItemStack bannerStack(Item item, BlockEntity be) {
        ItemStack stack = new ItemStack(item);
        if (be instanceof BannerBlockEntity banner) {
            stack.set(DataComponents.BANNER_PATTERNS, banner.getPatterns());
            if (banner.getCustomName() != null)
                stack.set(DataComponents.CUSTOM_NAME, banner.getCustomName());
        }
        return stack;
    }

    private static List<ItemStack> drops(Item item, LootParams.Builder params) {
        return List.of(bannerStack(item, params.getOptionalParameter(LootContextParams.BLOCK_ENTITY)));
    }

    private static class TatteredStanding extends BannerBlock implements TatteredBannerMarker {
        TatteredStanding(DyeColor color, Properties properties) { super(color, properties); }
        @Override protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
            return drops(this.asItem(), params);
        }
        @Override protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos,
                BlockState state, boolean includeData) {
            return bannerStack(asItem(), level.getBlockEntity(pos));
        }
    }

    private static class TatteredWall extends WallBannerBlock implements TatteredBannerMarker {
        private final Block standing;
        TatteredWall(DyeColor color, Block standing, Properties properties) {
            super(color, properties);
            this.standing = standing;
        }
        @Override protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
            return drops(standing.asItem(), params);
        }
        @Override protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos,
                BlockState state, boolean includeData) {
            return bannerStack(standing.asItem(), level.getBlockEntity(pos));
        }
    }
}
