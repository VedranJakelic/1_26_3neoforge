package net.beamex.shamaschizm.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.beamex.shamaschizm.registry.ModDataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** A real smithing recipe: copy the original equipment and add only the persistent upgrade flag. */
public record BlueprintSameItemRecipe(Ingredient template, Ingredient base, Ingredient addition) implements SmithingRecipe {
    public static final MapCodec<BlueprintSameItemRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC.fieldOf("template").forGetter(BlueprintSameItemRecipe::template),
            Ingredient.CODEC.fieldOf("base").forGetter(BlueprintSameItemRecipe::base),
            Ingredient.CODEC.fieldOf("addition").forGetter(BlueprintSameItemRecipe::addition)
    ).apply(i, BlueprintSameItemRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, BlueprintSameItemRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, BlueprintSameItemRecipe::template,
            Ingredient.CONTENTS_STREAM_CODEC, BlueprintSameItemRecipe::base,
            Ingredient.CONTENTS_STREAM_CODEC, BlueprintSameItemRecipe::addition,
            BlueprintSameItemRecipe::new);

    @Override public boolean matches(SmithingRecipeInput input, Level level) {
        return template.test(input.getItem(0)) && base.test(input.getItem(1)) && addition.test(input.getItem(2));
    }
    @Override public ItemStack assemble(SmithingRecipeInput input) {
        if (!base.test(input.getItem(1))) return ItemStack.EMPTY;
        ItemStack result = input.getItem(1).copyWithCount(1);
        result.set(ModDataComponents.RAISED_ENCHANT_CAP, true);
        if (isVanillaNetheriteArmor(result)) {
            result.set(ModDataComponents.ANCIENT_APPEARANCE, true);
        }
        return result;
    }

    private static boolean isVanillaNetheriteArmor(ItemStack stack) {
        return stack.is(Items.NETHERITE_HELMET)
                || stack.is(Items.NETHERITE_CHESTPLATE)
                || stack.is(Items.NETHERITE_LEGGINGS)
                || stack.is(Items.NETHERITE_BOOTS);
    }
    @Override public boolean showNotification() { return true; }
    @Override public String group() { return ""; }
    @Override public Optional<Ingredient> templateIngredient() { return Optional.of(template); }
    @Override public Ingredient baseIngredient() { return base; }
    @Override public Optional<Ingredient> additionIngredient() { return Optional.of(addition); }
    @Override public PlacementInfo placementInfo() { return PlacementInfo.create(java.util.List.of(template, base, addition)); }
    @Override public RecipeSerializer<BlueprintSameItemRecipe> getSerializer() { return ModRecipeSerializers.BLUEPRINT_SAME_ITEM; }
}
