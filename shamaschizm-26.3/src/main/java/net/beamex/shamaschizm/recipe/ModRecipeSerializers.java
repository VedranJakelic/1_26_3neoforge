package net.beamex.shamaschizm.recipe;
import com.mojang.serialization.MapCodec;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.common.crafting.IngredientType;

@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class ModRecipeSerializers {
    public static RecipeSerializer<BlueprintSameItemRecipe> BLUEPRINT_SAME_ITEM;
    public static IngredientType<UpgradeableIngredient> UPGRADEABLE;
    @SubscribeEvent public static void register(RegisterEvent event) {
        event.register(Registries.RECIPE_SERIALIZER, helper -> {
            BLUEPRINT_SAME_ITEM = new RecipeSerializer<>(BlueprintSameItemRecipe.CODEC, BlueprintSameItemRecipe.STREAM_CODEC);
            helper.register(Shamaschizm.id("blueprint_same_item"), BLUEPRINT_SAME_ITEM);
        });
        event.register(NeoForgeRegistries.Keys.INGREDIENT_TYPES, helper -> {
            UPGRADEABLE = new IngredientType<>(MapCodec.unit(new UpgradeableIngredient()));
            helper.register(Shamaschizm.id("upgradeable"), UPGRADEABLE);
        });
    }
}
