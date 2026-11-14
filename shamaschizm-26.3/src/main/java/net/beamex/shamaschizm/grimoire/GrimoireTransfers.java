package net.beamex.shamaschizm.grimoire;

import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.GrindstoneEvent;

/** Preview is pure; the book changes only when the server commits the output pickup. */
@EventBusSubscriber(modid=Shamaschizm.MOD_ID)
public final class GrimoireTransfers {
    public static final TagKey<Item> EQUIPMENT=TagKey.create(Registries.ITEM,Shamaschizm.id("grimoire_equipment"));
    public record Transfer(ItemStack output,ItemStack book,boolean bookOnTop) {}
    public static Transfer plan(ItemStack top,ItemStack bottom) {
        boolean first=GrimoireRegistration.isBook(top),second=GrimoireRegistration.isBook(bottom);
        if(first==second) return null;
        ItemStack book=first?top:bottom,tool=first?bottom:top;
        if(book.getCount()!=1 || tool.getCount()!=1 || !tool.is(EQUIPMENT)) return null;
        ItemEnchantments enchants=tool.getOrDefault(DataComponents.ENCHANTMENTS,ItemEnchantments.EMPTY);
        ItemEnchantments stored=book.getOrDefault(DataComponents.STORED_ENCHANTMENTS,ItemEnchantments.EMPTY);
        if(enchants.isEmpty() && stored.isEmpty()) return null;
        ItemStack result=tool.copy(),retained=book.copy();
        if(!enchants.isEmpty()) {
            ItemEnchantments.Mutable combined=new ItemEnchantments.Mutable(stored);
            for(var entry:enchants.entrySet()) combined.upgrade(entry.getKey(),entry.getIntValue());
            retained.set(DataComponents.STORED_ENCHANTMENTS,combined.toImmutable());
            result.set(DataComponents.ENCHANTMENTS,ItemEnchantments.EMPTY);
        } else {
            // Deliberately transfers every stored enchantment, including curses and incompatible types.
            result.set(DataComponents.ENCHANTMENTS,stored);
            retained.set(DataComponents.STORED_ENCHANTMENTS,ItemEnchantments.EMPTY);
        }
        return new Transfer(result,retained,first);
    }
    @SubscribeEvent public static void preview(GrindstoneEvent.OnPlaceItem event) {
        if(!GrimoireRegistration.isBook(event.getTopItem())&&!GrimoireRegistration.isBook(event.getBottomItem())) return;
        Transfer transfer=plan(event.getTopItem(),event.getBottomItem());
        if(transfer==null) { event.setCanceled(true);return; }
        event.setOutput(transfer.output());event.setXp(0);
    }
    @SubscribeEvent public static void take(GrindstoneEvent.OnTakeItem event) {
        if(!GrimoireRegistration.isBook(event.getTopItem())&&!GrimoireRegistration.isBook(event.getBottomItem())) return;
        Transfer transfer=plan(event.getTopItem(),event.getBottomItem());
        if(transfer==null) { event.setCanceled(true);return; }
        event.setXp(0);
        event.setNewTopItem(transfer.bookOnTop()?transfer.book():ItemStack.EMPTY);
        event.setNewBottomItem(transfer.bookOnTop()?ItemStack.EMPTY:transfer.book());
    }
}
