package net.narutoxboruto.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.narutoxboruto.attachments.MainAttachment;
import net.narutoxboruto.attachments.jutsus.JutsuStorage;
import net.narutoxboruto.entities.throwables.ThrownFumaShuriken;
import net.narutoxboruto.items.ModItems;

import java.util.List;

/**
 * Helper class for managing clan-specific permanent items.
 * Currently handles Fuma Clan's permanent Fuma Shuriken.
 * 
 * Since Minecraft 1.21.1 changed the item data API, we use a simpler approach:
 * - Fuma clan members should always have exactly one Fuma Shuriken
 * - Check by item type, not by NBT tags
 * - Automatically restore if lost
 */
public class ClanItemHelper {
    
    /**
     * Check if an item stack is a Fuma clan item (Fuma Shuriken).
     */
    public static boolean isClanItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.getItem() == ModItems.FUMA_SHURIKEN.get();
    }
    
    /**
     * Check if player has a Fuma Shuriken in inventory or jutsu storage.
     */
    private static boolean hasFumaShuriken(ServerPlayer player) {
        Item fumaItem = ModItems.FUMA_SHURIKEN.get();
        
        // Check inventory
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && stack.getItem() == fumaItem) {
                return true;
            }
        }
        
        // Check jutsu storage
        JutsuStorage storage = player.getData(MainAttachment.JUTSU_STORAGE);
        for (ItemStack stack : storage.getItems()) {
            if (!stack.isEmpty() && stack.getItem() == fumaItem) {
                return true;
            }
        }
        
        // Check for thrown Fuma Shurikens in the world that belong to this player
        if (hasThrownFumaShuriken(player)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if there's a ThrownFumaShuriken in the world that belongs to this player.
     * This prevents duplicate shurikens from being given while one is still in flight/returning.
     */
    private static boolean hasThrownFumaShuriken(ServerPlayer player) {
        // Search in a large area around the player (256 blocks should cover max throw distance + return)
        double searchRadius = 256.0;
        AABB searchBox = player.getBoundingBox().inflate(searchRadius);
        
        List<ThrownFumaShuriken> thrownShurikens = player.level().getEntitiesOfClass(
                ThrownFumaShuriken.class, 
                searchBox,
                shuriken -> shuriken.getOwner() == player
        );
        
        return !thrownShurikens.isEmpty();
    }
    
    /**
     * Remove all Fuma Shurikens from player's inventory and jutsu storage.
     */
    public static void removeClanItems(ServerPlayer player) {
        Item fumaItem = ModItems.FUMA_SHURIKEN.get();
        
        // Remove from inventory
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && stack.getItem() == fumaItem) {
                inventory.setItem(i, ItemStack.EMPTY);
            }
        }
        
        // Remove from jutsu storage
        JutsuStorage storage = player.getData(MainAttachment.JUTSU_STORAGE);
        for (int i = 0; i < JutsuStorage.STORAGE_SIZE; i++) {
            ItemStack stack = storage.getItem(i);
            if (!stack.isEmpty() && stack.getItem() == fumaItem) {
                storage.setItem(i, ItemStack.EMPTY);
            }
        }
        storage.syncToClient(player);
    }
    
    /**
     * Give the Fuma Shuriken to a player when they become Fuma clan.
     */
    public static void giveFumaClanItem(ServerPlayer player) {
        // Only give if they don't already have one
        if (hasFumaShuriken(player)) {
            return;
        }
        
        ItemStack fumaShuriken = new ItemStack(ModItems.FUMA_SHURIKEN.get());
        
        // Try to add to inventory first
        if (!player.getInventory().add(fumaShuriken)) {
            // If inventory full, add to jutsu storage
            JutsuStorage storage = player.getData(MainAttachment.JUTSU_STORAGE);
            storage.addJutsu(fumaShuriken);
            storage.syncToClient(player);
        }
    }
    
    /**
     * Check if player should have Fuma clan item but doesn't, and restore it.
     * Called periodically to ensure the item is never permanently lost.
     */
    public static void ensureFumaClanItemPresent(ServerPlayer player) {
        String clan = player.getData(MainAttachment.CLAN).getValue();
        
        if ("fuma".equals(clan)) {
            if (!hasFumaShuriken(player)) {
                // Player lost their Fuma Shuriken - give it back
                giveFumaClanItem(player);
            }
        }
    }
}
