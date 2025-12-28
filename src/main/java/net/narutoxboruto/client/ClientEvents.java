package net.narutoxboruto.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.narutoxboruto.client.gui.ShinobiStatsGui;
import net.narutoxboruto.items.swords.AbstractAbilitySword;
import net.narutoxboruto.main.Main;
import net.narutoxboruto.networking.ModPacketHandler;
import net.narutoxboruto.networking.jutsu.CastJutsuPacket;
import net.narutoxboruto.networking.jutsu.OpenJutsuStoragePacket;
import net.narutoxboruto.networking.misc.RechargeChakra;
import net.narutoxboruto.networking.misc.ToggleChakraControl;
import net.narutoxboruto.networking.misc.ToggleSwordAbility;
import net.narutoxboruto.util.ModKeyBinds;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Main.MOD_ID, value = Dist.CLIENT, bus =EventBusSubscriber.Bus.MOD)
public class ClientEvents {
    private static int keyPressTime = 0;
    private static boolean iskeyHeldDown = false;

    @SubscribeEvent
    public static void clientTicker(PlayerTickEvent.Post event) {
        if (iskeyHeldDown) {
            keyPressTime++;
        } else {
            keyPressTime = 0;
        }
    }

    @SubscribeEvent
    public static void shinobiStatsKeybind(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (ModKeyBinds.OPEN_GUI.consumeClick()) {
            minecraft.setScreen(new ShinobiStatsGui());
        }
    }
    
    @SubscribeEvent
    public static void jutsuStorageKeybind(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen == null && ModKeyBinds.JUTSU_WHEEL.consumeClick()) {
            // Send packet to server to open jutsu storage
            ModPacketHandler.sendToServer(new OpenJutsuStoragePacket());
        }
    }

    @SubscribeEvent
    public static void specialActionKeybind(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (player == null) return;

        // Use if instead of while to avoid consuming multiple clicks
        if (ModKeyBinds.SPECIAL_ACTION.consumeClick()) {
            ItemStack itemStack = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (itemStack.getItem() instanceof AbstractAbilitySword) {
                ModPacketHandler.sendToServer(new ToggleSwordAbility());
            }
        }
    }

    @SubscribeEvent
    public static void chakraKeybind(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player != null && ModKeyBinds.CHAKRA_CONTROL.consumeClick()) {
            ModPacketHandler.sendToServer(new ToggleChakraControl());
        }
    }

    @SubscribeEvent
    public static void chakraCharge(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player != null && ModKeyBinds.CHAKRA_RECHARGE.consumeClick()){
            // Shift + C to charge chakra
            if (player.isCrouching()){
                ModPacketHandler.sendToServer(new RechargeChakra());
            }
        }
    }
    
    /**
     * Client-side handler for RightClickEmpty - fires when player right-clicks with empty hand at air.
     * This event only fires on the client, so we need to send a packet to the server.
     */
    @EventBusSubscriber(modid = Main.MOD_ID, value = Dist.CLIENT)
    public static class ClientGameBusEvents {
        @SubscribeEvent
        public static void onRightClickEmpty(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickEmpty event) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) return;
            
            // Check if main hand is empty
            ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (!mainHand.isEmpty()) return;
            
            // Send packet to server to try casting jutsu
            // The server will validate chakra control, chakra amount, cooldowns, etc.
            ModPacketHandler.sendToServer(new CastJutsuPacket());
        }
    }

    @EventBusSubscriber(modid = Main.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static class ClientModBusEvents {

        @SubscribeEvent
        public static void onKeyRegister(RegisterKeyMappingsEvent event) {
            event.register(ModKeyBinds.SPECIAL_ACTION);
            event.register(ModKeyBinds.CHAKRA_RECHARGE);
            event.register(ModKeyBinds.OPEN_GUI);
            event.register(ModKeyBinds.CHAKRA_CONTROL);
            event.register(ModKeyBinds.JUTSU_WHEEL);
        }
    }

}

