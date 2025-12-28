package net.narutoxboruto.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.narutoxboruto.attachments.MainAttachment;
import net.narutoxboruto.attachments.jutsus.JutsuStorage;
import net.narutoxboruto.items.jutsus.AbstractJutsuItem;
import net.narutoxboruto.menu.JutsuStorageMenu;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Iterator;

/**
 * Event handlers to ensure jutsu items are permanently bound to the player.
 * 
 * Rules:
 * 1. Cannot be dropped/tossed from inventory (Q key)
 * 2. Cannot leave inventory on death
 * 3. If somehow found as an entity in the world, return to jutsu storage
 * 4. Only valid destination is jutsu storage menu or player inventory
 * 5. Cannot be placed in chests or other containers
 */
public class JutsuItemEvents {

    /**
     * Prevent players from dropping jutsu items with Q key.
     * Cancel the event and return the item to their inventory.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onItemToss(ItemTossEvent event) {
        ItemStack stack = event.getEntity().getItem();
        Player player = event.getPlayer();
        
        if (stack.getItem() instanceof AbstractJutsuItem) {
            // Cancel the drop
            event.setCanceled(true);
            
            // Give the item back to the player
            if (!player.getInventory().add(stack)) {
                // If inventory is somehow full, put it back in jutsu storage
                if (player instanceof ServerPlayer serverPlayer) {
                    returnJutsuToStorage(serverPlayer, stack);
                }
            }
        }
    }
    
    /**
     * When a container is closed, check if any jutsu items ended up in non-player slots.
     * Return them to the player's inventory or jutsu storage.
     */
    @SubscribeEvent
    public static void onContainerClose(PlayerContainerEvent.Close event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        
        AbstractContainerMenu container = event.getContainer();
        
        // Skip if it's the jutsu storage menu - that's allowed
        if (container instanceof JutsuStorageMenu) {
            return;
        }
        
        // Check all slots in the container
        for (int i = 0; i < container.slots.size(); i++) {
            Slot slot = container.slots.get(i);
            ItemStack stack = slot.getItem();
            
            if (stack.getItem() instanceof AbstractJutsuItem) {
                // Check if this slot belongs to player's inventory
                if (!(slot.container instanceof Inventory)) {
                    // This jutsu item is in a non-player container slot - remove it
                    slot.set(ItemStack.EMPTY);
                    
                    // Return to player inventory or storage
                    if (!serverPlayer.getInventory().add(stack)) {
                        returnJutsuToStorage(serverPlayer, stack);
                    }
                }
            }
        }
    }
    
    /**
     * When a player dies, prevent jutsu items from dropping as entities.
     * They should stay in inventory (handled by PlayerEvent.Clone) or go to storage.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        
        // Remove jutsu items from drops and save them for respawn
        Iterator<ItemEntity> iterator = event.getDrops().iterator();
        while (iterator.hasNext()) {
            ItemEntity itemEntity = iterator.next();
            ItemStack stack = itemEntity.getItem();
            
            if (stack.getItem() instanceof AbstractJutsuItem) {
                // Remove from drops - they'll be preserved via Clone event
                iterator.remove();
                
                // Store in jutsu storage as backup
                JutsuStorage storage = player.getData(MainAttachment.JUTSU_STORAGE);
                storage.addJutsu(stack);
            }
        }
    }
    
    /**
     * Periodic check to ensure any jutsu items that somehow escaped
     * are returned to jutsu storage.
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        
        // If player has a container open (other than jutsu storage), check every tick
        // Otherwise check every 20 ticks for performance
        AbstractContainerMenu container = player.containerMenu;
        boolean hasContainerOpen = container != null && !(container instanceof JutsuStorageMenu) 
                && container != player.inventoryMenu;
        
        if (!hasContainerOpen && player.tickCount % 20 != 0) {
            return;
        }
        
        // If a container is open, check for jutsu items in container slots and remove them
        if (hasContainerOpen) {
            for (int i = 0; i < container.slots.size(); i++) {
                Slot slot = container.slots.get(i);
                ItemStack stack = slot.getItem();
                
                if (stack.getItem() instanceof AbstractJutsuItem) {
                    // Check if this slot belongs to player's inventory
                    if (!(slot.container instanceof Inventory)) {
                        // Jutsu item in a non-player container slot - remove immediately
                        slot.set(ItemStack.EMPTY);
                        
                        // Return to player inventory or storage
                        if (!player.getInventory().add(stack)) {
                            returnJutsuToStorage(player, stack);
                        }
                    }
                }
            }
        }
        
        // Check if player has any jutsu items that need to be in storage
        // This handles edge cases where items might escape via bugs or exploits
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.getItem() instanceof AbstractJutsuItem) {
                // Verify this jutsu is legitimate (player should have the release for it)
                AbstractJutsuItem jutsu = (AbstractJutsuItem) stack.getItem();
                String requiredRelease = jutsu.getRequiredRelease();
                
                if (requiredRelease != null && !requiredRelease.isEmpty()) {
                    String playerReleases = player.getData(MainAttachment.RELEASE_LIST).getValue();
                    if (!playerReleases.toLowerCase().contains(requiredRelease.toLowerCase())) {
                        // Player doesn't have the release for this jutsu - remove it
                        inventory.setItem(i, ItemStack.EMPTY);
                        // Return to their storage in case they had it legitimately before
                        returnJutsuToStorage(player, stack);
                    }
                }
            }
        }
    }
    
    /**
     * Helper to return a jutsu item to the player's jutsu storage.
     */
    private static void returnJutsuToStorage(ServerPlayer player, ItemStack jutsuStack) {
        JutsuStorage storage = player.getData(MainAttachment.JUTSU_STORAGE);
        
        // Check if they already have this jutsu in storage
        if (!storage.hasJutsu(jutsuStack.getItem().getClass())) {
            storage.addJutsu(jutsuStack);
        }
        
        // Sync to client
        storage.syncToClient(player);
    }
}
