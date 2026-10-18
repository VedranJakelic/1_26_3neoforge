package net.beamex.shamaschizm.stag;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
public final class AntlerRecipe extends CustomRecipe{
    private static final AntlerRecipe INSTANCE=new AntlerRecipe();
    public static final RecipeSerializer<AntlerRecipe> SERIALIZER=new RecipeSerializer<>(MapCodec.unit(INSTANCE),StreamCodec.<RegistryFriendlyByteBuf,AntlerRecipe>unit(INSTANCE));
    private static ItemStack base(CraftingInput input){
        if(input.ingredientCount()!=2)return ItemStack.EMPTY;
        ItemStack helmet=ItemStack.EMPTY;boolean antler=false;
        for(int i=0;i<input.size();i++){
            var stack=input.getItem(i);if(stack.isEmpty())continue;
            if(stack.is(StagRegistration.ANTLER)&&!antler)antler=true;
            else if(Antlers.helmet(stack)&&!Antlers.has(stack)&&helmet.isEmpty())helmet=stack;
            else return ItemStack.EMPTY;
        }
        return antler?helmet:ItemStack.EMPTY;
    }
    @Override public boolean matches(CraftingInput input,Level level){return !base(input).isEmpty();}
    @Override public ItemStack assemble(CraftingInput input){var base=base(input);if(base.isEmpty())return ItemStack.EMPTY;var result=base.copyWithCount(1);Antlers.set(result,true);return result;}
    @Override public RecipeSerializer<AntlerRecipe> getSerializer(){return SERIALIZER;}
}
