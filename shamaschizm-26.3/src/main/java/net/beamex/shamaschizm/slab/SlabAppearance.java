package net.beamex.shamaschizm.slab;

import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/** One gameplay item; a saved model choice changes only its appearance. */
@EventBusSubscriber(modid=Shamaschizm.MOD_ID)
public final class SlabAppearance {
    public static final Identifier SECOND_MODEL=Shamaschizm.id("slab02");
    private static final String CHOSEN="shamaschizm_slab_appearance_chosen";
    private SlabAppearance(){}
    public static boolean isSecond(ItemStack stack){return SECOND_MODEL.equals(stack.get(DataComponents.ITEM_MODEL));}
    public static ItemStack choose(ItemStack stack,RandomSource random){
        if(!stack.is(SlabRegistration.SLAB))return stack;
        CustomData data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY);
        if(data.copyTag().getBooleanOr(CHOSEN,false))return stack;
        // Respect an explicitly supplied alternate appearance as well.
        if(!isSecond(stack))stack.set(DataComponents.ITEM_MODEL,random.nextBoolean()?SECOND_MODEL:SlabRegistration.SLAB_ID);
        CustomData.update(DataComponents.CUSTOM_DATA,stack,tag->tag.putBoolean(CHOSEN,true));
        return stack;
    }
    @SubscribeEvent public static void dropped(EntityJoinLevelEvent event){
        if(!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof ItemEntity item)
                || !item.getItem().is(SlabRegistration.SLAB))return;
        // Also covers command-created drops and slabs from other loot sources.
        // Reassign a copy so the item's synchronized stack is marked dirty.
        item.setItem(choose(item.getItem().copy(),level.getRandom()));
    }
}
