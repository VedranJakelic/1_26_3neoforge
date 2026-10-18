package net.beamex.shamaschizm.item;
import net.minecraft.world.item.Item;
public final class SoulIconItem extends Item {
    public SoulIconItem(Properties properties) { super(properties); }
    @Override public void inventoryTick(net.minecraft.world.item.ItemStack stack,
            net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.Entity owner,
            net.minecraft.world.entity.@org.jspecify.annotations.Nullable EquipmentSlot slot) {
        stack.setCount(0);
    }
}

