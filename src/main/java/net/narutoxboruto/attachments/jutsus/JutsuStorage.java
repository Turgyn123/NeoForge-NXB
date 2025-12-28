package net.narutoxboruto.attachments.jutsus;

import com.mojang.serialization.Codec;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.narutoxboruto.networking.ModPacketHandler;
import net.narutoxboruto.networking.jutsu.SyncJutsuStorage;

/**
 * Player attachment that stores the player's unlocked jutsus.
 * Acts like an Ender Chest but specifically for jutsu items.
 * Players can move jutsus from here to their inventory for combat.
 */
public class JutsuStorage {
    public static final int STORAGE_SIZE = 54; // 6 rows of 9, like a double chest
    
    // Codec for serialization - stores as NBT string
    public static final Codec<JutsuStorage> CODEC = CompoundTag.CODEC.xmap(
        JutsuStorage::fromNbt,
        JutsuStorage::toNbt
    );
    
    private final NonNullList<ItemStack> items;
    
    public JutsuStorage() {
        this.items = NonNullList.withSize(STORAGE_SIZE, ItemStack.EMPTY);
    }
    
    public JutsuStorage(NonNullList<ItemStack> items) {
        this.items = items;
    }
    
    public static JutsuStorage fromNbt(CompoundTag tag) {
        JutsuStorage storage = new JutsuStorage();
        if (tag.contains("Items", Tag.TAG_LIST)) {
            ListTag listTag = tag.getList("Items", Tag.TAG_COMPOUND);
            for (int i = 0; i < listTag.size(); i++) {
                CompoundTag itemTag = listTag.getCompound(i);
                int slot = itemTag.getByte("Slot") & 255;
                if (slot < STORAGE_SIZE) {
                    // Use ResourceLocation-based serialization for jutsu items
                    String itemId = itemTag.getString("ItemId");
                    int count = itemTag.contains("Count") ? itemTag.getInt("Count") : 1;
                    if (!itemId.isEmpty()) {
                        ResourceLocation loc = ResourceLocation.tryParse(itemId);
                        if (loc != null) {
                            Item item = BuiltInRegistries.ITEM.get(loc);
                            if (item != null) {
                                storage.items.set(slot, new ItemStack(item, count));
                            }
                        }
                    }
                }
            }
        }
        return storage;
    }
    
    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        ListTag listTag = new ListTag();
        for (int i = 0; i < STORAGE_SIZE; i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putByte("Slot", (byte) i);
                // Store item as ResourceLocation string - simpler and doesn't need HolderLookup
                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                itemTag.putString("ItemId", itemId.toString());
                itemTag.putInt("Count", stack.getCount());
                listTag.add(itemTag);
            }
        }
        tag.put("Items", listTag);
        return tag;
    }
    
    public NonNullList<ItemStack> getItems() {
        return items;
    }
    
    public ItemStack getItem(int slot) {
        if (slot < 0 || slot >= STORAGE_SIZE) return ItemStack.EMPTY;
        return items.get(slot);
    }
    
    public void setItem(int slot, ItemStack stack) {
        if (slot >= 0 && slot < STORAGE_SIZE) {
            items.set(slot, stack);
        }
    }
    
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        return result;
    }
    
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = items.get(slot);
        items.set(slot, ItemStack.EMPTY);
        return stack;
    }
    
    /**
     * Add a jutsu to the first available slot.
     * @return true if added successfully, false if storage is full
     */
    public boolean addJutsu(ItemStack stack) {
        for (int i = 0; i < STORAGE_SIZE; i++) {
            if (items.get(i).isEmpty()) {
                items.set(i, stack.copy());
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if the player has a specific jutsu unlocked.
     * Checks by item type - each jutsu item class represents a unique jutsu.
     */
    public boolean hasJutsu(Class<?> jutsuItemClass) {
        for (ItemStack stack : items) {
            if (!stack.isEmpty() && jutsuItemClass.isInstance(stack.getItem())) {
                return true;
            }
        }
        return false;
    }
    
    public void syncToClient(ServerPlayer player) {
        ModPacketHandler.sendToPlayer(new SyncJutsuStorage(this.toNbt()), player);
    }
    
    public void copyFrom(JutsuStorage source) {
        for (int i = 0; i < STORAGE_SIZE; i++) {
            this.items.set(i, source.items.get(i).copy());
        }
    }
    
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }
    
    /**
     * Convert this storage to an ItemStackHandler for use in menus.
     */
    public net.neoforged.neoforge.items.ItemStackHandler toItemStackHandler() {
        net.neoforged.neoforge.items.ItemStackHandler handler = new net.neoforged.neoforge.items.ItemStackHandler(STORAGE_SIZE);
        for (int i = 0; i < STORAGE_SIZE; i++) {
            handler.setStackInSlot(i, items.get(i).copy());
        }
        return handler;
    }
}
