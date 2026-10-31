package net.beamex.shamaschizm.enchantment;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.world.Schizm;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/** Roll once per eligible death-drop stack, after ordinary loot creation. */
@EventBusSubscriber(modid=Shamaschizm.MOD_ID)
public final class EaNasirDrops {
    public static final ResourceKey<Enchantment> CURSE=ResourceKey.create(Registries.ENCHANTMENT,Shamaschizm.id("touch_of_ea_nasir"));

    private static boolean eligible(ItemStack stack){
        return !stack.isEmpty()&&stack.isDamageableItem()&&!stack.isEnchanted()
                &&stack.is(ItemTags.DURABILITY_ENCHANTABLE);
    }

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void drops(LivingDropsEvent event){
        if(!(event.getEntity() instanceof Mob mob)||!(mob.level() instanceof ServerLevel level)
                ||!level.dimension().equals(Schizm.KEY))return;
        var curse=level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(CURSE);
        for(var drop:event.getDrops()){
            ItemStack stack=drop.getItem();
            if(eligible(stack)&&level.getRandom().nextInt(4)==0){
                ItemStack enchanted=stack.copy();
                enchanted.enchant(curse,1);
                drop.setItem(enchanted);
            }
        }
    }
}
