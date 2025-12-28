package net.narutoxboruto.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.narutoxboruto.client.PlayerData;
import net.narutoxboruto.jutsu.JutsuData;
import net.narutoxboruto.jutsu.JutsuRegistry;
import net.narutoxboruto.jutsu.JutsuWheel;
import net.narutoxboruto.main.Main;
import net.narutoxboruto.networking.ModPacketHandler;
import net.narutoxboruto.networking.jutsu.SelectJutsuSlotPacket;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Radial menu for selecting jutsus from the wheel.
 * Left-click on a slot = select that jutsu
 * Right-click on a slot = open assignment menu (future feature)
 */
@OnlyIn(Dist.CLIENT)
public class JutsuWheelScreen extends Screen {
    
    private static final int WHEEL_RADIUS = 80;
    private static final int SLOT_RADIUS = 24;
    private static final int CENTER_RADIUS = 30;
    
    // Textures for jutsu icons
    private static final ResourceLocation FIREBALL_ICON = ResourceLocation.fromNamespaceAndPath(
        Main.MOD_ID, "textures/entity/great_fireball.png"
    );
    
    private int hoveredSlot = -1;
    private final JutsuWheel wheel;
    
    public JutsuWheelScreen() {
        super(Component.translatable("gui.narutoxboruto.jutsu_wheel"));
        this.wheel = PlayerData.getJutsuWheel();
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
    
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Override to do nothing - prevents the blur effect from 1.21
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // No background overlay - just draw the wheel directly over the game
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        
        // Calculate which slot is hovered based on mouse position
        hoveredSlot = getSlotFromMousePosition(mouseX, mouseY, centerX, centerY);
        
        // Use pose stack for proper rendering
        guiGraphics.pose().pushPose();
        
        // Draw the wheel slots
        for (int i = 0; i < JutsuWheel.WHEEL_SIZE; i++) {
            drawSlot(guiGraphics, centerX, centerY, i);
        }
        
        // Draw center indicator showing selected jutsu
        drawCenterIndicator(guiGraphics, centerX, centerY);
        
        guiGraphics.pose().popPose();
        
        // Draw tooltip for hovered slot - show name and cost
        if (hoveredSlot >= 0) {
            String jutsuId = wheel.getJutsuInSlot(hoveredSlot);
            if (jutsuId != null && !jutsuId.isEmpty()) {
                JutsuData jutsu = JutsuRegistry.getJutsuById(jutsuId);
                if (jutsu != null) {
                    guiGraphics.renderTooltip(
                        this.font,
                        Component.literal(jutsu.getDisplayName())
                            .append(" - ")
                            .append(Component.literal(jutsu.getChakraCost() + " CP")),
                        mouseX, mouseY
                    );
                }
            } else {
                guiGraphics.renderTooltip(
                    this.font,
                    Component.literal("Empty Slot"),
                    mouseX, mouseY
                );
            }
        }
        
        // Instructions
        guiGraphics.drawCenteredString(
            this.font,
            Component.translatable("gui.jutsu_wheel.instructions"),
            centerX,
            centerY + WHEEL_RADIUS + 50,
            0xFFFFFF
        );
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    private void drawSlot(GuiGraphics guiGraphics, int centerX, int centerY, int slotIndex) {
        // Calculate position around the circle
        double angle = (slotIndex * 2 * Math.PI / JutsuWheel.WHEEL_SIZE) - Math.PI / 2;
        int slotX = centerX + (int)(Math.cos(angle) * WHEEL_RADIUS);
        int slotY = centerY + (int)(Math.sin(angle) * WHEEL_RADIUS);
        
        // Determine colors
        boolean isSelected = slotIndex == wheel.getSelectedSlot();
        boolean isHovered = slotIndex == hoveredSlot;
        
        int bgColor;
        if (isHovered) {
            bgColor = 0xDD3366BB; // Blue highlight
        } else if (isSelected) {
            bgColor = 0xDDCC8800; // Orange for selected
        } else {
            bgColor = 0xCC333333; // Dark gray
        }
        
        int halfSize = SLOT_RADIUS;
        int left = slotX - halfSize;
        int top = slotY - halfSize;
        int right = slotX + halfSize;
        int bottom = slotY + halfSize;
        
        // Draw slot background
        guiGraphics.fill(left, top, right, bottom, bgColor);
        
        // Draw crisp border using filled rectangles instead of renderOutline
        int borderColor = isSelected ? 0xFFFFAA00 : (isHovered ? 0xFF6699FF : 0xFF555555);
        int borderWidth = 2;
        // Top
        guiGraphics.fill(left, top, right, top + borderWidth, borderColor);
        // Bottom
        guiGraphics.fill(left, bottom - borderWidth, right, bottom, borderColor);
        // Left
        guiGraphics.fill(left, top, left + borderWidth, bottom, borderColor);
        // Right
        guiGraphics.fill(right - borderWidth, top, right, bottom, borderColor);
        
        // Draw slot number
        guiGraphics.drawCenteredString(this.font, String.valueOf(slotIndex + 1), slotX, top - 12, 0xAAAAAA);
        
        // Draw jutsu icon if assigned
        String jutsuId = wheel.getJutsuInSlot(slotIndex);
        if (jutsuId != null && !jutsuId.isEmpty()) {
            JutsuData jutsu = JutsuRegistry.getJutsuById(jutsuId);
            if (jutsu != null) {
                // Draw icon based on jutsu type
                int iconSize = 16;
                int iconX = slotX - iconSize / 2;
                int iconY = slotY - iconSize / 2 - 4;
                
                if (jutsuId.equals("fire_ball")) {
                    // Use custom fireball texture
                    guiGraphics.blit(FIREBALL_ICON, iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);
                } else if (jutsuId.equals("earth_wall")) {
                    // Use dirt block as icon
                    guiGraphics.renderItem(new ItemStack(Items.DIRT), iconX, iconY);
                } else {
                    // Default: show abbreviated name
                    String shortName = getShortName(jutsu.getDisplayName());
                    guiGraphics.drawCenteredString(this.font, shortName, slotX, slotY - 8, 0xFFFFFF);
                }
                
                // Draw chakra cost below icon
                guiGraphics.drawCenteredString(this.font, jutsu.getChakraCost() + " CP", slotX, slotY + 8, 0x88AAFF);
            }
        } else {
            guiGraphics.drawCenteredString(this.font, "-", slotX, slotY - 4, 0x666666);
        }
    }
    
    private void drawCenterIndicator(GuiGraphics guiGraphics, int centerX, int centerY) {
        int left = centerX - CENTER_RADIUS;
        int top = centerY - CENTER_RADIUS;
        int right = centerX + CENTER_RADIUS;
        int bottom = centerY + CENTER_RADIUS;
        
        // Draw center background
        guiGraphics.fill(left, top, right, bottom, 0xEE111111);
        
        // Draw crisp border using filled rectangles
        int borderColor = 0xFFCC8800;
        int borderWidth = 2;
        // Top
        guiGraphics.fill(left, top, right, top + borderWidth, borderColor);
        // Bottom
        guiGraphics.fill(left, bottom - borderWidth, right, bottom, borderColor);
        // Left
        guiGraphics.fill(left, top, left + borderWidth, bottom, borderColor);
        // Right
        guiGraphics.fill(right - borderWidth, top, right, bottom, borderColor);
        
        // Show currently selected jutsu with icon
        String selectedId = wheel.getSelectedJutsu();
        if (selectedId != null && !selectedId.isEmpty()) {
            JutsuData jutsu = JutsuRegistry.getJutsuById(selectedId);
            if (jutsu != null) {
                // Draw larger icon in center
                int iconSize = 24;
                int iconX = centerX - iconSize / 2;
                int iconY = centerY - iconSize / 2 - 6;
                
                if (selectedId.equals("fire_ball")) {
                    guiGraphics.blit(FIREBALL_ICON, iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);
                } else if (selectedId.equals("earth_wall")) {
                    // Render larger dirt icon
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(centerX - 12, centerY - 16, 0);
                    guiGraphics.pose().scale(1.5F, 1.5F, 1.0F);
                    guiGraphics.renderItem(new ItemStack(Items.DIRT), 0, 0);
                    guiGraphics.pose().popPose();
                }
                
                // Draw chakra cost below icon
                guiGraphics.drawCenteredString(this.font,
                    jutsu.getChakraCost() + " CP",
                    centerX, centerY + 14, 0x88AAFF);
            }
        } else {
            guiGraphics.drawCenteredString(this.font, "No Jutsu", centerX, centerY - 4, 0x666666);
        }
    }
    
    private int getSlotFromMousePosition(int mouseX, int mouseY, int centerX, int centerY) {
        int dx = mouseX - centerX;
        int dy = mouseY - centerY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        // Must be within the wheel area but not in the center
        if (distance < CENTER_RADIUS || distance > WHEEL_RADIUS + SLOT_RADIUS + 10) {
            return -1;
        }
        
        // Calculate angle and determine slot
        double angle = Math.atan2(dy, dx) + Math.PI / 2; // Offset so slot 0 is at top
        if (angle < 0) angle += 2 * Math.PI;
        
        int slot = (int)(angle / (2 * Math.PI) * JutsuWheel.WHEEL_SIZE);
        return Mth.clamp(slot, 0, JutsuWheel.WHEEL_SIZE - 1);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (hoveredSlot >= 0) {
            if (button == 0) { // Left click - select this slot
                // Update locally
                wheel.setSelectedSlot(hoveredSlot);
                // Send to server
                ModPacketHandler.sendToServer(new SelectJutsuSlotPacket(hoveredSlot));
                // Play click sound
                Minecraft.getInstance().player.playSound(
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 0.5F, 1.0F
                );
                // Close the screen
                this.onClose();
                return true;
            } else if (button == 1) { // Right click - open assignment menu
                // TODO: Open jutsu list for assignment
                // For now, just close
                this.onClose();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Number keys 1-8 for quick slot selection
        if (keyCode >= 49 && keyCode <= 56) { // Keys 1-8
            int slot = keyCode - 49;
            wheel.setSelectedSlot(slot);
            ModPacketHandler.sendToServer(new SelectJutsuSlotPacket(slot));
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    private String getShortName(String fullName) {
        // Abbreviate long names
        if (fullName.length() <= 8) return fullName;
        String[] words = fullName.split(" ");
        if (words.length >= 2) {
            return words[0].substring(0, Math.min(4, words[0].length())) + ".";
        }
        return fullName.substring(0, 7) + ".";
    }
}
