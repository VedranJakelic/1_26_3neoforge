package net.beamex.shamaschizm.ascent;

import java.util.List;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class AscentBootstrap {
    public static final Identifier TEMPLATE_ID = id("ascent_armor_trim_smithing_template");
    public static final Identifier LIGHT_ID = id("ascent_worn_light");
    public static Item TEMPLATE;
    public static AscentLightBlock LIGHT;
    private AscentBootstrap() {}
    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath(Shamaschizm.MOD_ID, path); }

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(Registries.BLOCK, helper -> {
            LIGHT = new AscentLightBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, LIGHT_ID))
                    .replaceable().noCollision().noOcclusion().noLootTable()
                    .strength(0.0F).lightLevel(state -> AscentGlow.LIGHT_LEVEL)
                    .pushReaction(PushReaction.POPPED).isValidSpawn((s,l,p,e) -> false));
            helper.register(LIGHT_ID, LIGHT);
        });
        event.register(Registries.ITEM, helper -> {
            TEMPLATE = new SmithingTemplateItem(Component.literal("Armor"),
                    Component.literal("Trim materials or Glow Ink Sac"),
                    Component.literal("Add a piece of armor"),
                    Component.literal("Add a trim material or glow ink sac"),
                    List.of(Identifier.parse("minecraft:container/slot/chestplate")),
                    List.of(Identifier.parse("minecraft:container/slot/ingot")),
                    new Item.Properties().setId(ResourceKey.create(Registries.ITEM, TEMPLATE_ID))
                            .rarity(Rarity.UNCOMMON).component(DataComponents.ITEM_NAME,
                                    Component.translatableWithFallback("item.shamaschizm.ascent_armor_trim_smithing_template", "Ascent Armor Trim")));
            helper.register(TEMPLATE_ID, TEMPLATE);
        });
        event.register(Registries.RECIPE_SERIALIZER,
                helper -> helper.register(id("ascent_glow_trim"), AscentGlowRecipe.SERIALIZER));
    }

    @SubscribeEvent
    public static void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().identifier().equals(Identifier.parse("minecraft:ingredients"))) event.accept(TEMPLATE);
    }
}
