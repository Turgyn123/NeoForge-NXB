package net.narutoxboruto.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.narutoxboruto.attachments.MainAttachment;
import net.narutoxboruto.attachments.jutsus.JutsuStorage;
import net.narutoxboruto.items.jutsus.AbstractJutsuItem;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
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
 * 4. Only valid destination is jutsu storage menu
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
        
        // Only check every 20 ticks (1 second) for performance
        if (player.tickCount % 20 != 0) {
            return;
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
