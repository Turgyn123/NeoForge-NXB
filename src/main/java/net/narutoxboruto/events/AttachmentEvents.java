package net.narutoxboruto.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.narutoxboruto.attachments.MainAttachment;
import net.narutoxboruto.attachments.info.*;
import net.narutoxboruto.attachments.jutsus.JutsuStorage;
import net.narutoxboruto.items.jutsus.AbstractJutsuItem;
import net.narutoxboruto.main.Main;
import net.narutoxboruto.util.JutsuGrantHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.List;


public class AttachmentEvents {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            //INFO
            serverPlayer.getData(MainAttachment.AFFILIATION).syncValue(serverPlayer);
            serverPlayer.getData(MainAttachment.CLAN).syncValue(serverPlayer);
            serverPlayer.getData(MainAttachment.CHAKRA).syncValue(serverPlayer);
            serverPlayer.getData(MainAttachment.MAX_CHAKRA).syncValue(serverPlayer);
            serverPlayer.getData(MainAttachment.SHINOBI_POINTS).syncValue(serverPlayer);
            serverPlayer.getData(MainAttachment.RANK).syncValue(serverPlayer);
            serverPlayer.getData(MainAttachment.RELEASE_LIST).syncValue(serverPlayer);

            //STATS
            serverPlayer.getData(MainAttachment.GENJUTSU).syncValue(serverPlayer);
            serverPlayer.getData(MainAttachment.KENJUTSU).syncValue(serverPlayer);
            serverPlayer.getData(MainAttachment.KINJUTSU).syncValue(serverPlayer);
            serverPlayer.getData(MainAttachment.MEDICAL).syncValue(serverPlayer);
            serverPlayer.getData(MainAttachment.NINJUTSU).syncValue(serverPlayer);
            serverPlayer.getData(MainAttachment.SENJUTSU).syncValue(serverPlayer);
            serverPlayer.getData(MainAttachment.SHURIKENJUTSU).syncValue(serverPlayer);
            serverPlayer.getData(MainAttachment.SPEED).syncValue(serverPlayer);
            serverPlayer.getData(MainAttachment.SUMMONING).syncValue(serverPlayer);
            serverPlayer.getData(MainAttachment.TAIJUTSU).syncValue(serverPlayer);
            
            // Clean up any duplicate jutsus and sync jutsu storage
            JutsuGrantHelper.cleanupDuplicateJutsus(serverPlayer);
            serverPlayer.getData(MainAttachment.JUTSU_STORAGE).syncToClient(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onJoinWorldSyncCap(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof ServerPlayer serverPlayer) {

            //INFO
            serverPlayer.getData(MainAttachment.AFFILIATION).syncValue(serverPlayer);
            serverPlayer.getData(MainAttachment.CLAN).syncValue(serverPlayer);
            serverPlayer.getData(MainAttachment.CHAKRA).syncValue(serverPlayer);
            serverPlayer.getData(MainAttachment.MAX_CHAKRA).syncValue(serverPlayer);

            serverPlayer.getData(MainAttachment.SHINOBI_POINTS).syncValue(serverPlayer);
            serverPlayer.getData(MainAttachment.RANK).syncValue(serverPlayer);
            serverPlayer.getData(MainAttachment.RELEASE_LIST).syncValue(serverPlayer);

            //STATS
            serverPlayer.getData(MainAttachment.GENJUTSU).syncValue(serverPlayer);
            serverPlayer.getData(MainAttachment.KENJUTSU).syncValue(serverPlayer);
            serverPlayer.getData(MainAttachment.KINJUTSU).syncValue(serverPlayer);
            serverPlayer.getData(MainAttachment.MEDICAL).syncValue(serverPlayer);
            serverPlayer.getData(MainAttachment.NINJUTSU).syncValue(serverPlayer);
            serverPlayer.getData(MainAttachment.SENJUTSU).syncValue(serverPlayer);
            serverPlayer.getData(MainAttachment.SHURIKENJUTSU).syncValue(serverPlayer);
            serverPlayer.getData(MainAttachment.SPEED).syncValue(serverPlayer);
            serverPlayer.getData(MainAttachment.SUMMONING).syncValue(serverPlayer);
            serverPlayer.getData(MainAttachment.TAIJUTSU).syncValue(serverPlayer);

            //JUTSU_LIST (commented out)
            // serverPlayer.getData(MainAttachment.FIRELIST).syncValue(serverPlayer);
            // serverPlayer.getData(MainAttachment.EARTHLIST).syncValue(serverPlayer);
            // serverPlayer.getData(MainAttachment.WATERLIST).syncValue(serverPlayer);
            // serverPlayer.getData(MainAttachment.WINDLIST).syncValue(serverPlayer);
            // serverPlayer.getData(MainAttachment.LIGHTINGLIST).syncValue(serverPlayer);
            // serverPlayer.getData(MainAttachment.YANGLIST).syncValue(serverPlayer);
            // serverPlayer.getData(MainAttachment.YINGLIST).syncValue(serverPlayer);
            
            // Sync jutsu storage
            serverPlayer.getData(MainAttachment.JUTSU_STORAGE).syncToClient(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerCloned(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayer newPlayer
                && event.getOriginal() instanceof ServerPlayer original) {
            // No need to reviveCaps() with attachments

            //INFO
            newPlayer.setData(MainAttachment.AFFILIATION, original.getData(MainAttachment.AFFILIATION));
            newPlayer.setData(MainAttachment.CLAN, original.getData(MainAttachment.CLAN));
            newPlayer.setData(MainAttachment.MAX_CHAKRA, original.getData(MainAttachment.MAX_CHAKRA));
            newPlayer.setData(MainAttachment.RELEASE_LIST, original.getData(MainAttachment.RELEASE_LIST));
            newPlayer.setData(MainAttachment.SHINOBI_POINTS, original.getData(MainAttachment.SHINOBI_POINTS));

            newPlayer.setData(MainAttachment.CHAKRA, original.getData(MainAttachment.CHAKRA));
            newPlayer.getData(MainAttachment.CHAKRA).reset(newPlayer);

            //STATS
            newPlayer.setData(MainAttachment.RANK, original.getData(MainAttachment.RANK));
            newPlayer.setData(MainAttachment.TAIJUTSU, original.getData(MainAttachment.TAIJUTSU));
            newPlayer.setData(MainAttachment.SUMMONING, original.getData(MainAttachment.SUMMONING));
            newPlayer.setData(MainAttachment.GENJUTSU, original.getData(MainAttachment.GENJUTSU));
            newPlayer.setData(MainAttachment.KENJUTSU, original.getData(MainAttachment.KENJUTSU));
            newPlayer.setData(MainAttachment.KINJUTSU, original.getData(MainAttachment.KINJUTSU));
            newPlayer.setData(MainAttachment.MEDICAL, original.getData(MainAttachment.MEDICAL));
            newPlayer.setData(MainAttachment.SPEED, original.getData(MainAttachment.SPEED));
            newPlayer.setData(MainAttachment.SHURIKENJUTSU, original.getData(MainAttachment.SHURIKENJUTSU));
            newPlayer.setData(MainAttachment.NINJUTSU, original.getData(MainAttachment.NINJUTSU));
            newPlayer.setData(MainAttachment.SENJUTSU, original.getData(MainAttachment.SENJUTSU));
            
            // Preserve Jutsu Storage across death
            newPlayer.setData(MainAttachment.JUTSU_STORAGE, original.getData(MainAttachment.JUTSU_STORAGE));
            
            // Preserve jutsu items from inventory - they should NEVER drop on death
            if (event.isWasDeath()) {
                preserveJutsuItemsOnDeath(original, newPlayer);
            }
        }
    }
    
    /**
     * Preserve jutsu items from the original player's inventory to the new player.
     * Jutsu items are permanently bound and should never be lost.
     */
    private static void preserveJutsuItemsOnDeath(ServerPlayer original, ServerPlayer newPlayer) {
        Inventory originalInv = original.getInventory();
        List<ItemStack> jutsuItems = new ArrayList<>();
        
        // Collect all jutsu items from original inventory
        for (int i = 0; i < originalInv.getContainerSize(); i++) {
            ItemStack stack = originalInv.getItem(i);
            if (stack.getItem() instanceof AbstractJutsuItem) {
                jutsuItems.add(stack.copy());
            }
        }
        
        // Add them to new player's inventory
        for (ItemStack jutsu : jutsuItems) {
            if (!newPlayer.getInventory().add(jutsu)) {
                // If inventory is full, put back in jutsu storage
                JutsuStorage storage = newPlayer.getData(MainAttachment.JUTSU_STORAGE);
                storage.addJutsu(jutsu);
            }
        }
    }

    // Add this method to sync all stats to the client
    public static void syncAllStatsToClient(ServerPlayer player) {
        // Sync each individual stat attachment
        player.getData(MainAttachment.NINJUTSU).syncValue(player);
        player.getData(MainAttachment.SHURIKENJUTSU).syncValue(player);
        player.getData(MainAttachment.KINJUTSU).syncValue(player);
        player.getData(MainAttachment.SUMMONING).syncValue(player);
        player.getData(MainAttachment.MEDICAL).syncValue(player);
        player.getData(MainAttachment.KENJUTSU).syncValue(player);
        player.getData(MainAttachment.TAIJUTSU).syncValue(player);
        player.getData(MainAttachment.SENJUTSU).syncValue(player);
        player.getData(MainAttachment.SPEED).syncValue(player);
        player.getData(MainAttachment.GENJUTSU).syncValue(player);

        // Sync clan as well
        player.getData(MainAttachment.CLAN).syncValue(player);
        player.getData(MainAttachment.SHINOBI_POINTS).syncValue(player);
    }

    @SubscribeEvent
    public static void onReplenishChakra(PlayerTickEvent.Pre event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer && serverPlayer.isSleepingLongEnough()) {
            Chakra chakra = serverPlayer.getData(MainAttachment.CHAKRA.get());
            chakra.replenish(serverPlayer);
        }
    }
}
