package net.narutoxboruto.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.narutoxboruto.attachments.MainAttachment;
import net.narutoxboruto.attachments.modes.ChakraControl;
import net.narutoxboruto.jutsu.JutsuCaster;
import net.narutoxboruto.jutsu.JutsuWheel;
import net.narutoxboruto.main.Main;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Handles empty-hand right-click to cast jutsus when channeling chakra.
 */
@EventBusSubscriber(modid = Main.MOD_ID)
public class JutsuCastingEvents {
    
    @SubscribeEvent
    public static void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
        // This only fires on client, we need to use RightClickItem with empty check
    }
    
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        
        // Check if main hand is empty
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!mainHand.isEmpty()) return;
        
        // Check if channeling chakra
        ChakraControl chakraControl = serverPlayer.getData(MainAttachment.CHAKRA_CONTROL);
        if (!chakraControl.getValue()) return;
        
        // Try to cast the selected jutsu
        if (JutsuCaster.tryCastSelectedJutsu(serverPlayer)) {
            event.setCanceled(true);
        }
    }
    
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        
        // Check if main hand is empty (this event fires even with empty hand in some cases)
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!mainHand.isEmpty()) return;
        
        // Check if channeling chakra
        ChakraControl chakraControl = serverPlayer.getData(MainAttachment.CHAKRA_CONTROL);
        if (!chakraControl.getValue()) return;
        
        // Try to cast the selected jutsu
        if (JutsuCaster.tryCastSelectedJutsu(serverPlayer)) {
            event.setCanceled(true);
        }
    }
}
