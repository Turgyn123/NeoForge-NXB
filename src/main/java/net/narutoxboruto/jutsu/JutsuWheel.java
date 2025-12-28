package net.narutoxboruto.jutsu;

import com.mojang.serialization.Codec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.narutoxboruto.networking.ModPacketHandler;
import net.narutoxboruto.networking.jutsu.SyncJutsuWheel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Player attachment that stores the jutsu wheel configuration.
 * The wheel has 8 slots (like a radial menu) that can be assigned jutsus.
 * Also tracks which slot is currently selected for casting.
 */
public class JutsuWheel {
    public static final int WHEEL_SIZE = 8;
    
    // Codec for serialization - stores as comma-separated jutsu IDs
    public static final Codec<JutsuWheel> CODEC = Codec.STRING.xmap(
        JutsuWheel::fromString,
        JutsuWheel::toString
    );
    
    // Array of jutsu IDs assigned to each slot (empty string = no jutsu)
    private final String[] slots = new String[WHEEL_SIZE];
    
    // Currently selected slot index (0-7)
    private int selectedSlot = 0;
    
    public JutsuWheel() {
        Arrays.fill(slots, "");
    }
    
    public JutsuWheel(String serialized) {
        Arrays.fill(slots, "");
        if (serialized != null && !serialized.isEmpty()) {
            String[] parts = serialized.split(";");
            if (parts.length > 0) {
                // First part is selected slot
                try {
                    selectedSlot = Integer.parseInt(parts[0]);
                } catch (NumberFormatException e) {
                    selectedSlot = 0;
                }
            }
            if (parts.length > 1) {
                // Second part is comma-separated slot assignments
                String[] slotData = parts[1].split(",", -1);
                for (int i = 0; i < Math.min(slotData.length, WHEEL_SIZE); i++) {
                    slots[i] = slotData[i];
                }
            }
        }
    }
    
    public static JutsuWheel fromString(String s) {
        return new JutsuWheel(s);
    }
    
    @Override
    public String toString() {
        return selectedSlot + ";" + String.join(",", slots);
    }
    
    public String getJutsuInSlot(int slot) {
        if (slot < 0 || slot >= WHEEL_SIZE) return "";
        return slots[slot];
    }
    
    public void setJutsuInSlot(int slot, String jutsuId) {
        if (slot >= 0 && slot < WHEEL_SIZE) {
            slots[slot] = jutsuId != null ? jutsuId : "";
        }
    }
    
    public int getSelectedSlot() {
        return selectedSlot;
    }
    
    public void setSelectedSlot(int slot) {
        if (slot >= 0 && slot < WHEEL_SIZE) {
            this.selectedSlot = slot;
        }
    }
    
    public String getSelectedJutsu() {
        return getJutsuInSlot(selectedSlot);
    }
    
    /**
     * Get the first empty slot, or -1 if wheel is full.
     */
    public int getFirstEmptySlot() {
        for (int i = 0; i < WHEEL_SIZE; i++) {
            if (slots[i] == null || slots[i].isEmpty()) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Check if a jutsu is already in the wheel.
     */
    public boolean containsJutsu(String jutsuId) {
        for (String slot : slots) {
            if (slot != null && slot.equals(jutsuId)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Try to add a jutsu to the first empty slot.
     * Returns true if added, false if wheel is full or jutsu already exists.
     */
    public boolean tryAddJutsu(String jutsuId) {
        if (containsJutsu(jutsuId)) return false;
        int emptySlot = getFirstEmptySlot();
        if (emptySlot == -1) return false;
        slots[emptySlot] = jutsuId;
        return true;
    }
    
    /**
     * Get list of all assigned jutsus (non-empty slots).
     */
    public List<String> getAssignedJutsus() {
        List<String> result = new ArrayList<>();
        for (String slot : slots) {
            if (slot != null && !slot.isEmpty()) {
                result.add(slot);
            }
        }
        return result;
    }
    
    /**
     * Clear a slot.
     */
    public void clearSlot(int slot) {
        setJutsuInSlot(slot, "");
    }
    
    /**
     * Copy from another wheel.
     */
    public void copyFrom(JutsuWheel other) {
        this.selectedSlot = other.selectedSlot;
        System.arraycopy(other.slots, 0, this.slots, 0, WHEEL_SIZE);
    }
    
    /**
     * Sync to client.
     */
    public void syncValue(ServerPlayer serverPlayer) {
        ModPacketHandler.sendToPlayer(new SyncJutsuWheel(this.toString()), serverPlayer);
    }
    
    /**
     * Auto-populate wheel with available jutsus based on player's releases.
     */
    public void autoPopulate(String releaseList) {
        List<JutsuData> available = JutsuRegistry.getAvailableJutsus(releaseList);
        for (JutsuData jutsu : available) {
            if (!containsJutsu(jutsu.getId())) {
                int emptySlot = getFirstEmptySlot();
                if (emptySlot == -1) break; // Wheel is full
                slots[emptySlot] = jutsu.getId();
            }
        }
    }
}
