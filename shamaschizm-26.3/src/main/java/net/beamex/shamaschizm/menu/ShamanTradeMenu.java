package net.beamex.shamaschizm.menu;

import java.util.List;
import net.beamex.shamaschizm.SoulData;
import net.beamex.shamaschizm.effect.ModEffects;
import net.beamex.shamaschizm.event.ModItemBootstrap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;

/** Server-owned virtual currency. Display stacks never enter an inventory or become payment. */
public final class ShamanTradeMenu extends MerchantMenu {
    private final ServerPlayer player;
    private final Merchant merchant;
    private int selected;
    private boolean updating;
    private boolean closed;

    public ShamanTradeMenu(int id, Inventory inventory, Merchant merchant) {
        super(id, inventory, merchant);
        this.player = (ServerPlayer) inventory.player;
        this.merchant = merchant;
        refresh();
    }

    private static boolean soul(ItemStack stack) {
        return stack.is(ModItemBootstrap.SOUL_ICON) || stack.is(ModItemBootstrap.HIGH_SOUL_ICON);
    }
    private MerchantOffer offer() {
        var offers = getOffers();
        return selected >= 0 && selected < offers.size() ? offers.get(selected) : null;
    }
    private boolean virtual(MerchantOffer offer) {
        return soul(offer.getBaseCostA()) || soul(offer.getResult());
    }
    private boolean allowed(MerchantOffer offer) {
        if (offer.isOutOfStock()) return false;
        ItemStack a = offer.getBaseCostA();
        if (soul(a)) {
            if (a.is(ModItemBootstrap.HIGH_SOUL_ICON) && !player.hasEffect(ModEffects.TRIPPING)) return false;
            return SoulData.getSouls(player) >= a.getCount() && matchesSecond(offer);
        }
        return offer.satisfiedBy(getSlot(0).getItem(), getSlot(1).getItem())
                && SoulData.getSouls(player) < Integer.MAX_VALUE;
    }
    private boolean matchesSecond(MerchantOffer offer) {
        ItemStack b = offer.getCostB();
        ItemStack actual = getSlot(1).getItem();
        return b.isEmpty() ? actual.isEmpty()
                : ItemStack.isSameItemSameComponents(b, actual) && actual.getCount() >= b.getCount();
    }

    private void setResult(ItemStack stack) {
        // Slot.set calls MerchantContainer.setChanged, which would rebuild a vanilla result from the display currency.
        // Write the backing result directly; broadcastChanges synchronizes the authoritative value.
        getSlot(2).container.setItem(2, stack);
    }
    private void clearGhosts() {
        for (int i = 0; i <= 2; i++) {
            if (soul(getSlot(i).getItem())) getSlot(i).set(ItemStack.EMPTY);
        }
    }
    private void refresh() {
        if (updating || closed || player == null) return;
        updating = true;
        try {
            MerchantOffer offer = offer();
            if (offer == null) {
                clearGhosts();
                setResult(ItemStack.EMPTY);
                return;
            }
            if (soul(offer.getBaseCostA())) {
                var existing = getSlot(0).getItem();
                if (!existing.isEmpty() && !soul(existing)) {
                    // Preserve real input when changing to a currency trade.
                    getSlot(0).set(ItemStack.EMPTY);
                    returnToPlayer(existing);
                }
                ItemStack display = offer.getBaseCostA().copy();
                display.set(DataComponents.LORE, new ItemLore(List.of(Component.literal(
                        "You have " + SoulData.getSouls(player) + " soul(s)."))));
                getSlot(0).set(display);
                setResult(allowed(offer) ? offer.getResult().copy() : ItemStack.EMPTY);
            } else if (soul(offer.getResult())) {
                setResult(allowed(offer) ? offer.getResult().copy() : ItemStack.EMPTY);
            }
        } finally { updating = false; }
    }
    private void returnToPlayer(ItemStack stack) {
        player.getInventory().add(stack);
        if (!stack.isEmpty()) player.drop(stack, false, net.minecraft.util.Prediction.SERVER_ONLY);
    }

    @Override public boolean stillValid(Player who) { return !closed && merchant.stillValid(who); }
    @Override public void setSelectionHint(int index) {
        updating = true;
        try {
            clearGhosts();
            selected = index;
            super.setSelectionHint(index);
        } finally { updating = false; }
        refresh();
    }
    @Override public void slotsChanged(Container container) {
        if (updating || closed) return;
        super.slotsChanged(container);
        refresh();
    }
    @Override public void broadcastChanges() {
        refresh();
        super.broadcastChanges();
    }
    @Override public void tryMoveItems(int index) {
        setSelectionHint(index);
        MerchantOffer offer = offer();
        if (offer != null && !soul(offer.getBaseCostA())) super.tryMoveItems(index);
        refresh();
    }
    @Override public boolean canDragTo(Slot slot) {
        return slot.index > 2 && super.canDragTo(slot);
    }
    @Override public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.index > 2 && !soul(stack) && super.canTakeItemForPickAll(stack, slot);
    }
    @Override public ItemStack quickMoveStack(Player who, int index) {
        MerchantOffer offer = offer();
        if (index == 2 && offer != null && virtual(offer)) {
            transact(true);
            return ItemStack.EMPTY;
        }
        if (index >= 0 && index < slots.size() && soul(getSlot(index).getItem())) return ItemStack.EMPTY;
        return super.quickMoveStack(who, index);
    }
    @Override public void clicked(int index, int button, ContainerInput type, Player who) {
        if (!stillValid(who)) return;
        MerchantOffer offer = offer();
        if (index == 0 && offer != null && soul(offer.getBaseCostA())) return;
        if (index == 2 && offer != null && virtual(offer)) {
            if ((type == ContainerInput.PICKUP && (button == 0 || button == 1))
                    || type == ContainerInput.QUICK_MOVE) transact(type == ContainerInput.QUICK_MOVE);
            return;
        }
        super.clicked(index, button, type, who);
    }

    private void transact(boolean shift) {
        MerchantOffer offer = offer();
        if (offer == null || !virtual(offer) || !allowed(offer) || !stillValid(player)) return;
        ItemStack result = offer.getResult().copy();
        boolean currencyResult = soul(result);
        if (!currencyResult && !canReceive(result, shift)) return;
        updating = true;
        try {
            if (soul(offer.getBaseCostA())) {
                if (!SoulData.spend(player, offer.getBaseCostA().getCount())) return;
                if (!offer.getCostB().isEmpty()) getSlot(1).remove(offer.getCostB().getCount());
            } else {
                if (!SoulData.gain(player, result.getCount())) return;
                getSlot(0).remove(offer.getBaseCostA().getCount());
                if (!offer.getCostB().isEmpty()) getSlot(1).remove(offer.getCostB().getCount());
            }
            if (!currencyResult) {
                if (shift) player.getInventory().add(result);
                else if (getCarried().isEmpty()) setCarried(result);
                else getCarried().grow(result.getCount());
            }
            offer.increaseUses();
            merchant.notifyTrade(offer);
            player.playSound(merchant.getNotifyTradeSound(), 1.0F, 1.0F);
            setResult(ItemStack.EMPTY);
        } finally { updating = false; }
        broadcastChanges();
    }
    private boolean canReceive(ItemStack result, boolean shift) {
        if (!shift) {
            ItemStack carried = getCarried();
            return carried.isEmpty() || ItemStack.isSameItemSameComponents(carried, result)
                    && carried.getCount() + result.getCount() <= carried.getMaxStackSize();
        }
        int capacity = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) capacity += result.getMaxStackSize();
            else if (ItemStack.isSameItemSameComponents(stack, result)) capacity += Math.max(0, stack.getMaxStackSize() - stack.getCount());
            if (capacity >= result.getCount()) return true;
        }
        return false;
    }
    @Override public void removed(Player who) {
        closed = true;
        updating = true;
        clearGhosts();
        super.removed(who);
        merchant.setTradingPlayer(null);
    }
}
