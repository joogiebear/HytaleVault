package com.joogiebear.hytalevault.gui;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.function.consumer.ShortObjectConsumer;
import com.hypixel.hytale.protocol.ItemWithAllMetadata;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.filter.FilterActionType;
import com.hypixel.hytale.server.core.inventory.container.filter.FilterType;
import com.hypixel.hytale.server.core.inventory.container.filter.SlotFilter;
import com.hypixel.hytale.server.core.inventory.transaction.ClearTransaction;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Custom ItemContainer for vault storage.
 * All slots are usable storage slots.
 */
public class VaultContainer extends ItemContainer {

    public static final VaultContainer INSTANCE = new VaultContainer();
    public static final BuilderCodec<VaultContainer> CODEC =
            BuilderCodec.builder(VaultContainer.class, () -> INSTANCE).build();

    private short _capacity;
    private ItemStack[] _slots;

    public VaultContainer(short capacity) {
        this._capacity = capacity;
        this._slots = new ItemStack[capacity];
    }

    protected VaultContainer() {
        // For CODEC/singleton
    }

    @Override
    public short getCapacity() {
        return _capacity;
    }

    @Nonnull
    @Override
    public ClearTransaction clear() {
        return ClearTransaction.EMPTY;
    }

    @Override
    public void forEach(@NonNullDecl ShortObjectConsumer<ItemStack> action) {
        for (short i = 0; i < _capacity; i++) {
            action.accept(i, _slots[i]);
        }
    }

    @Override
    protected <V> V readAction(@Nonnull Supplier<V> action) {
        return action.get();
    }

    @Override
    protected <X, V> V readAction(@Nonnull Function<X, V> action, X x) {
        return action.apply(x);
    }

    @Override
    protected <V> V writeAction(@Nonnull Supplier<V> action) {
        return action.get();
    }

    @Override
    protected <X, V> V writeAction(@Nonnull Function<X, V> action, X x) {
        return action.apply(x);
    }

    @Nonnull
    @Override
    protected ClearTransaction internal_clear() {
        return ClearTransaction.EMPTY;
    }

    @Override
    protected ItemStack internal_getSlot(short slot) {
        validateSlotIndex(slot, _capacity);
        return _slots[slot];
    }

    @Override
    protected ItemStack internal_setSlot(short slot, ItemStack itemStack) {
        validateSlotIndex(slot, _capacity);
        ItemStack prev = _slots[slot];
        _slots[slot] = itemStack;
        return prev;
    }

    @Override
    protected ItemStack internal_removeSlot(short slot) {
        validateSlotIndex(slot, _capacity);
        ItemStack prev = _slots[slot];
        _slots[slot] = null;
        return prev;
    }

    @Override
    protected boolean cantAddToSlot(short slot, ItemStack itemStack, ItemStack slotItemStack) {
        return false;
    }

    @Override
    protected boolean cantRemoveFromSlot(short slot) {
        return false;
    }

    @Override
    protected boolean cantDropFromSlot(short slot) {
        return false;
    }

    @Override
    protected boolean cantMoveToSlot(ItemContainer fromContainer, short slotFrom) {
        return false;
    }

    @Override
    public void setGlobalFilter(FilterType globalFilter) { }

    @Override
    public void setSlotFilter(FilterActionType actionType, short slot, SlotFilter filter) {
        validateSlotIndex(slot, _capacity);
    }

    @Nonnull
    @Override
    public List<ItemStack> removeAllItemStacks() {
        return Collections.emptyList();
    }

    @Nonnull
    public Map<Integer, ItemWithAllMetadata> toProtocolMap() {
        var map = new java.util.HashMap<Integer, ItemWithAllMetadata>();

        for (int i = 0; i < _capacity; i++) {
            ItemStack s = _slots[i];
            if (s == null) continue;

            ItemWithAllMetadata p = new ItemWithAllMetadata();
            p.itemId = s.getItemId();
            p.quantity = s.getQuantity();
            p.durability = 0;
            p.maxDurability = 0;
            p.overrideDroppedItemAnimation = false;
            p.metadata = null;

            map.put(i, p);
        }

        return map;
    }

    @Override
    public VaultContainer clone() {
        VaultContainer c = new VaultContainer();
        c._capacity = this._capacity;

        if (this._slots != null) {
            c._slots = this._slots.clone();
        }

        return c;
    }
}
