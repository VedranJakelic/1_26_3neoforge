package net.beamex.shamaschizm.stag;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
public final class Antlers {
    private static final String KEY="shamaschizm_antlers";
    public static boolean has(ItemStack stack){return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getBooleanOr(KEY,false);}
    public static boolean helmet(ItemStack stack){
        var equippable=stack.get(DataComponents.EQUIPPABLE);
        return equippable!=null&&equippable.slot()==EquipmentSlot.HEAD&&stack.has(DataComponents.MAX_DAMAGE);
    }
    public static void set(ItemStack stack,boolean value){CustomData.update(DataComponents.CUSTOM_DATA,stack,t->{if(value)t.putBoolean(KEY,true);else t.remove(KEY);});}
}
