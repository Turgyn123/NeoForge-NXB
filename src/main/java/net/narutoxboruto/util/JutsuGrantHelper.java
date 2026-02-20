package net.narutoxboruto.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.narutoxboruto.attachments.MainAttachment;
import net.narutoxboruto.attachments.jutsus.JutsuStorage;
import net.narutoxboruto.items.ModItems;

/**
 * Utility class for granting jutsus to players when they learn releases.
 * Centralized logic ensures all methods of learning releases grant the appropriate jutsus.
 */
public class JutsuGrantHelper {

    /**
     * Grants jutsus for all releases in a comma-separated list.
     * Use this when a player learns multiple releases at once (e.g., ChakraPaper).
     * 
     * @param serverPlayer The player to grant jutsus to
     * @param releaseList Comma-separated list of releases (e.g., "fire, earth, water")
     */
    public static void grantJutsusForReleases(ServerPlayer serverPlayer, String releaseList) {
        if (releaseList == null || releaseList.isEmpty()) return;
        
        String[] releases = releaseList.split(",");
        for (String release : releases) {
            grantJutsuForRelease(serverPlayer, release.trim());
        }
    }

    /**
     * Grants the corresponding jutsu item to the player's jutsu storage when they learn a release.
     * 
     * @param serverPlayer The player to grant the jutsu to
     * @param releaseType The type of release learned (fire, earth, water, etc.)
     */
    public static void grantJutsuForRelease(ServerPlayer serverPlayer, String releaseType) {
        JutsuStorage storage = serverPlayer.getData(MainAttachment.JUTSU_STORAGE);
        ItemStack jutsuToGrant = null;
        String jutsuName = null;
        
        switch (releaseType.toLowerCase()) {
            case "fire":
                jutsuToGrant = new ItemStack(ModItems.FIRE_BALL_JUTSU.get());
                jutsuName = "Fire Ball";
                break;
            case "earth":
                grantMultipleJutsus(serverPlayer, storage, 
                    new ItemStack(ModItems.EARTH_WALL_JUTSU.get()), "Earth Wall",
                    new ItemStack(ModItems.EARTH_WAVE_JUTSU.get()), "Earth Wave");
                return; // Already handled
            case "water":
                grantMultipleJutsus(serverPlayer, storage,
                    new ItemStack(ModItems.WATER_PRISON_JUTSU.get()), "Water Prison",
                    new ItemStack(ModItems.SHARK_BOMB_JUTSU.get()), "Shark Bomb",
                    new ItemStack(ModItems.WATER_DRAGON_JUTSU.get()), "Water Dragon");
                return; // Already handled
            case "lightning":
                jutsuToGrant = new ItemStack(ModItems.LIGHTNING_CHAKRA_MODE.get());
                jutsuName = "Lightning Chakra Mode";
                break;
            // case "wind": 
            //     jutsuToGrant = new ItemStack(ModItems.WIND_JUTSU.get()); 
            //     jutsuName = "Wind Jutsu";
            //     break;
        }
        
        if (jutsuToGrant != null && storage.addJutsuIfNotOwned(jutsuToGrant, serverPlayer)) {
            serverPlayer.setData(MainAttachment.JUTSU_STORAGE, storage);
            storage.syncToClient(serverPlayer);
            
            // Send mastery message: "You mastered _____, (Press Z)"
            serverPlayer.sendSystemMessage(
                Component.literal("You mastered ")
                    .withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(jutsuName).withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(", (Press ").withStyle(ChatFormatting.GREEN))
                    .append(Component.literal("Z").withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(")").withStyle(ChatFormatting.GREEN))
            );
        }
    }
    
    /**
     * Helper method to grant multiple jutsus for a single release (2 jutsus).
     */
    private static void grantMultipleJutsus(ServerPlayer serverPlayer, JutsuStorage storage, 
            ItemStack jutsu1, String name1, ItemStack jutsu2, String name2) {
        boolean granted1 = storage.addJutsuIfNotOwned(jutsu1, serverPlayer);
        boolean granted2 = storage.addJutsuIfNotOwned(jutsu2, serverPlayer);
        
        if (granted1 || granted2) {
            serverPlayer.setData(MainAttachment.JUTSU_STORAGE, storage);
            storage.syncToClient(serverPlayer);
        }
        
        sendMasteryMessage(serverPlayer, name1, granted1);
        sendMasteryMessage(serverPlayer, name2, granted2);
    }
    
    /**
     * Helper method to grant multiple jutsus for a single release (3 jutsus).
     */
    private static void grantMultipleJutsus(ServerPlayer serverPlayer, JutsuStorage storage, 
            ItemStack jutsu1, String name1, ItemStack jutsu2, String name2, ItemStack jutsu3, String name3) {
        boolean granted1 = storage.addJutsuIfNotOwned(jutsu1, serverPlayer);
        boolean granted2 = storage.addJutsuIfNotOwned(jutsu2, serverPlayer);
        boolean granted3 = storage.addJutsuIfNotOwned(jutsu3, serverPlayer);
        
        if (granted1 || granted2 || granted3) {
            serverPlayer.setData(MainAttachment.JUTSU_STORAGE, storage);
            storage.syncToClient(serverPlayer);
        }
        
        sendMasteryMessage(serverPlayer, name1, granted1);
        sendMasteryMessage(serverPlayer, name2, granted2);
        sendMasteryMessage(serverPlayer, name3, granted3);
    }
    
    /**
     * Send the mastery message for a jutsu if it was granted.
     */
    private static void sendMasteryMessage(ServerPlayer serverPlayer, String jutsuName, boolean granted) {
        if (granted) {
            serverPlayer.sendSystemMessage(
                Component.literal("You mastered ")
                    .withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(jutsuName).withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(", (Press ").withStyle(ChatFormatting.GREEN))
                    .append(Component.literal("Z").withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(")").withStyle(ChatFormatting.GREEN))
            );
        }
    }
    
    /**
     * Clean up duplicate jutsus in a player's storage.
     * Call this on player login or when opening jutsu storage to fix any existing duplicates.
     */
    public static void cleanupDuplicateJutsus(ServerPlayer serverPlayer) {
        JutsuStorage storage = serverPlayer.getData(MainAttachment.JUTSU_STORAGE);
        int removed = storage.removeDuplicates(serverPlayer);
        
        if (removed > 0) {
            serverPlayer.setData(MainAttachment.JUTSU_STORAGE, storage);
            storage.syncToClient(serverPlayer);
        }
    }
    
    /**
     * Verifies that a player has all jutsus they should have based on their releases.
     * If a jutsu is missing from both inventory and jutsu storage, it will be restored.
     * Call this periodically or on player login.
     * 
     * @param serverPlayer The player to verify jutsus for
     * @param checkInventory If true, also check player's inventory for the jutsu before restoring
     */
    public static void verifyAndRestoreMissingJutsus(ServerPlayer serverPlayer, boolean checkInventory) {
        String playerReleases = serverPlayer.getData(MainAttachment.RELEASE_LIST).getValue();
        if (playerReleases == null || playerReleases.isEmpty()) return;
        
        JutsuStorage storage = serverPlayer.getData(MainAttachment.JUTSU_STORAGE);
        boolean modified = false;
        
        // Check each release and ensure player has the corresponding jutsu(s)
        if (playerReleases.toLowerCase().contains("fire")) {
            if (!hasJutsuAnywhere(serverPlayer, storage, ModItems.FIRE_BALL_JUTSU.get(), checkInventory)) {
                storage.addJutsu(new ItemStack(ModItems.FIRE_BALL_JUTSU.get()));
                modified = true;
            }
        }
        
        if (playerReleases.toLowerCase().contains("earth")) {
            if (!hasJutsuAnywhere(serverPlayer, storage, ModItems.EARTH_WALL_JUTSU.get(), checkInventory)) {
                storage.addJutsu(new ItemStack(ModItems.EARTH_WALL_JUTSU.get()));
                modified = true;
            }
            if (!hasJutsuAnywhere(serverPlayer, storage, ModItems.EARTH_WAVE_JUTSU.get(), checkInventory)) {
                storage.addJutsu(new ItemStack(ModItems.EARTH_WAVE_JUTSU.get()));
                modified = true;
            }
        }
        
        if (playerReleases.toLowerCase().contains("water")) {
            if (!hasJutsuAnywhere(serverPlayer, storage, ModItems.WATER_PRISON_JUTSU.get(), checkInventory)) {
                storage.addJutsu(new ItemStack(ModItems.WATER_PRISON_JUTSU.get()));
                modified = true;
            }
        }
        
        if (playerReleases.toLowerCase().contains("lightning")) {
            if (!hasJutsuAnywhere(serverPlayer, storage, ModItems.LIGHTNING_CHAKRA_MODE.get(), checkInventory)) {
                storage.addJutsu(new ItemStack(ModItems.LIGHTNING_CHAKRA_MODE.get()));
                modified = true;
            }
        }
        
        if (modified) {
            serverPlayer.setData(MainAttachment.JUTSU_STORAGE, storage);
            storage.syncToClient(serverPlayer);
        }
    }
    
    /**
     * Check if a player has a specific jutsu item in storage or inventory.
     */
    private static boolean hasJutsuAnywhere(ServerPlayer player, JutsuStorage storage, net.minecraft.world.item.Item jutsuItem, boolean checkInventory) {
        // Check jutsu storage
        if (storage.hasJutsu(jutsuItem.getClass())) {
            return true;
        }
        
        // Check player inventory if requested
        if (checkInventory) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.getItem() == jutsuItem) {
                    return true;
                }
            }
        }
        
        return false;
    }
}
