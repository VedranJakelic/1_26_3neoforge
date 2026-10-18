package net.beamex.shamaschizm.ascent;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SmithingRecipeDisplay;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimPattern;

/** Supplies a trim material locally to this recipe, without changing glow ink globally. */
public final class AscentGlowRecipe extends SimpleSmithingRecipe {
    public static final MapCodec<AscentGlowRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Recipe.CommonInfo.MAP_CODEC.forGetter((AscentGlowRecipe r) -> r.commonInfo),
            Ingredient.CODEC.fieldOf("template").forGetter(r -> r.template),
            Ingredient.CODEC.fieldOf("base").forGetter(r -> r.base),
            Ingredient.CODEC.fieldOf("addition").forGetter(r -> r.addition),
            TrimPattern.CODEC.fieldOf("pattern").forGetter(r -> r.pattern),
            TrimMaterial.CODEC.fieldOf("material").forGetter(r -> r.material)
    ).apply(i, AscentGlowRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, AscentGlowRecipe> STREAM_CODEC =
            StreamCodec.composite(
                    Recipe.CommonInfo.STREAM_CODEC, r -> r.commonInfo,
                    Ingredient.CONTENTS_STREAM_CODEC, r -> r.template,
                    Ingredient.CONTENTS_STREAM_CODEC, r -> r.base,
                    Ingredient.CONTENTS_STREAM_CODEC, r -> r.addition,
                    TrimPattern.STREAM_CODEC, r -> r.pattern,
                    TrimMaterial.STREAM_CODEC, r -> r.material, AscentGlowRecipe::new);
    public static final RecipeSerializer<AscentGlowRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);

    private final Ingredient template;
    private final Ingredient base;
    private final Ingredient addition;
    private final Holder<TrimPattern> pattern;
    private final Holder<TrimMaterial> material;

    public AscentGlowRecipe(Recipe.CommonInfo commonInfo, Ingredient template, Ingredient base,
                            Ingredient addition, Holder<TrimPattern> pattern, Holder<TrimMaterial> material) {
        super(commonInfo);
        this.template = template;
        this.base = base;
        this.addition = addition;
        this.pattern = pattern;
        this.material = material;
    }

    @Override
    public ItemStack assemble(SmithingRecipeInput input) {
        if (!template.test(input.template()) || !base.test(input.base()) || !addition.test(input.addition())
                || !pattern.is(AscentGlow.PATTERN) || !material.is(AscentGlow.MATERIAL)) return ItemStack.EMPTY;
        ArmorTrim trim = new ArmorTrim(material, pattern);
        if (Objects.equals(input.base().get(DataComponents.TRIM), trim)) return ItemStack.EMPTY;
        ItemStack result = input.base().copyWithCount(1);
        result.set(DataComponents.TRIM, trim);
        return result;
    }

    @Override public Optional<Ingredient> templateIngredient() { return Optional.of(template); }
    @Override public Ingredient baseIngredient() { return base; }
    @Override public Optional<Ingredient> additionIngredient() { return Optional.of(addition); }
    @Override public RecipeSerializer<AscentGlowRecipe> getSerializer() { return SERIALIZER; }
    @Override protected PlacementInfo createPlacementInfo() { return PlacementInfo.create(List.of(template, base, addition)); }

    @Override
    public List<RecipeDisplay> display() {
        ItemStack example = new ItemStack(Items.DIAMOND_CHESTPLATE);
        example.set(DataComponents.TRIM, new ArmorTrim(material, pattern));
        return List.of(new SmithingRecipeDisplay(template.display(), base.display(), addition.display(),
                new SlotDisplay.ItemStackSlotDisplay(ItemStackTemplate.fromNonEmptyStack(example)),
                new SlotDisplay.ItemSlotDisplay(Items.SMITHING_TABLE)));
    }
}
