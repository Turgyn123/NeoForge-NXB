package net.narutoxboruto.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.narutoxboruto.client.gui.ShinobiStatsGui;
import net.narutoxboruto.items.swords.AbstractAbilitySword;
import net.narutoxboruto.items.throwables.FumaShurikenItem;
import net.narutoxboruto.items.throwables.ThrowableWeaponItem;
import net.narutoxboruto.main.Main;
import net.narutoxboruto.networking.ModPacketHandler;
import net.narutoxboruto.networking.jutsu.OpenJutsuStoragePacket;
import net.narutoxboruto.networking.misc.RechargeChakra;
import net.narutoxboruto.networking.misc.SpecialThrowPacket;
import net.narutoxboruto.networking.misc.ToggleChakraControl;
import net.narutoxboruto.networking.misc.ToggleSwordAbility;
import net.narutoxboruto.particles.LightningSparksParticle;
import net.narutoxboruto.particles.ModParticles;
import net.narutoxboruto.util.ModKeyBinds;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Main.MOD_ID, value = Dist.CLIENT, bus =EventBusSubscriber.Bus.MOD)
public class ClientEvents {
    private static int keyPressTime = 0;
    private static boolean iskeyHeldDown = false;
    private static boolean cKeyWasPressed = false; // Track C key state to prevent repeated toggles

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
        if (minecraft.screen == null && ModKeyBinds.JUTSU_STORAGE.consumeClick()) {
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
            // Check for ability swords first
            if (itemStack.getItem() instanceof AbstractAbilitySword) {
                ModPacketHandler.sendToServer(new ToggleSwordAbility());
            }
            // Check for throwable items (except Fuma Shuriken)
            else if (itemStack.getItem() instanceof ThrowableWeaponItem && 
                     !(itemStack.getItem() instanceof FumaShurikenItem)) {
                ModPacketHandler.sendToServer(new SpecialThrowPacket());
            }
        }
    }

    @SubscribeEvent
    public static void chakraKeybind(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        
        if (player == null) return;
        
        // Check if C key is currently pressed (don't consume the click yet)
        boolean cKeyPressed = ModKeyBinds.CHAKRA_CONTROL.isDown();
        
        // Only toggle when:
        // 1. C key is pressed AND wasn't pressed last frame (rising edge)
        // 2. Player is NOT sneaking (sneaking + C should only charge)
        if (cKeyPressed && !cKeyWasPressed && !player.isCrouching()) {
            ModPacketHandler.sendToServer(new ToggleChakraControl());
        }
        
        // Update state for next frame
        cKeyWasPressed = cKeyPressed;
    }

    @SubscribeEvent
    public static void chakraCharge(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        // Only consume the key if player is crouching (Shift + C to charge chakra)
        if (player != null && player.isCrouching() && ModKeyBinds.CHAKRA_RECHARGE.consumeClick()){
            ModPacketHandler.sendToServer(new RechargeChakra());
        }
    }
    
    // Camera control is handled by vanilla during normal movement
    // No special camera modifications needed
}

/**
 * MOD bus events (for registration, etc.)
 */
@EventBusSubscriber(modid = Main.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
class ClientModBusEvents {

    @SubscribeEvent
    public static void onKeyRegister(RegisterKeyMappingsEvent event) {
        event.register(ModKeyBinds.SPECIAL_ACTION);
        event.register(ModKeyBinds.CHAKRA_RECHARGE);
        event.register(ModKeyBinds.OPEN_GUI);
        event.register(ModKeyBinds.CHAKRA_CONTROL);
        event.register(ModKeyBinds.JUTSU_STORAGE);
    }
    
    @SubscribeEvent
    public static void onRegisterParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.LIGHTNING_SPARKS.get(), LightningSparksParticle.Provider::new);
    }
}

