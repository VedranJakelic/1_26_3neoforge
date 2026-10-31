package net.beamex.shamaschizm.jewelfish;

import java.util.ArrayList;
import java.util.List;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;

public final class JewelfishFood {
    public static final TagKey<Item> FOOD=TagKey.create(Registries.ITEM,Shamaschizm.id("jewelfish_food"));
    public static final TagKey<Item> BLOCKS=TagKey.create(Registries.ITEM,Shamaschizm.id("jewelfish_food_blocks"));
    public static final TagKey<Item> JEWELS=TagKey.create(Registries.ITEM,Shamaschizm.id("jewelfish_jewels"));
    public static int value(ItemStack stack){
        if(stack.isEmpty()||stack.is(Items.REDSTONE)||stack.is(Items.REDSTONE_BLOCK))return 0;
        if(stack.is(BLOCKS))return 9;
        return stack.is(FOOD)?1:0;
    }
    public static boolean holding(Player player){return value(player.getMainHandItem())>0||value(player.getOffhandItem())>0;}
    public static List<Item> jewels(){
        List<Item> pool=new ArrayList<>();
        for(var holder:BuiltInRegistries.ITEM.getTagOrEmpty(JEWELS)){
            Item item=holder.value();
            if(item!=Items.AIR&&item!=Items.REDSTONE&&!(item instanceof BlockItem)&&!pool.contains(item))pool.add(item);
        }
        if(pool.isEmpty())pool.add(Items.EMERALD);
        return pool;
    }
}
