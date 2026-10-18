package net.beamex.shamaschizm.slab;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jspecify.annotations.Nullable;

/**
 * A MapItem subclass solely so vanilla gives it the normal one/two-handed map
 * pose. SlabItemInHandMixin replaces the actual map sheet with the slab model.
 */
public final class SlabItem extends MapItem {
    public SlabItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    protected @Nullable MapItemSavedData getCustomMapData(ItemStack stack, Level level) {
        return null;
    }
}
