package net.narutoxboruto.attachments.modes;

import com.mojang.serialization.Codec;
import net.minecraft.server.level.ServerPlayer;
import net.narutoxboruto.attachments.MainAttachment;
import net.narutoxboruto.networking.ModPacketHandler;
import net.narutoxboruto.networking.misc.SyncLightningChakraModeActive;

/**
 * Attachment to track whether Lightning Chakra Mode is active on a player.
 * This mode gives Strength 2, Speed 3, and lightning particle effects.
 */
public class LightningChakraModeActive {
    private boolean active;

    public static final Codec<LightningChakraModeActive> CODEC = Codec.BOOL.xmap(LightningChakraModeActive::new, LightningChakraModeActive::isActive);

    public LightningChakraModeActive(boolean active) {
        this.active = active;
    }

    public LightningChakraModeActive() {
        this.active = false;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active, ServerPlayer serverPlayer) {
        this.active = active;
        syncValue(serverPlayer);
    }

    public void toggle(ServerPlayer serverPlayer) {
        this.active = !this.active;
        syncValue(serverPlayer);
    }

    public void syncValue(ServerPlayer serverPlayer) {
        serverPlayer.setData(MainAttachment.LIGHTNING_CHAKRA_MODE_ACTIVE.get(), this);
        ModPacketHandler.sendToPlayer(new SyncLightningChakraModeActive(active), serverPlayer);
    }

    // Client-side only method
    public void setActive(boolean active) {
        this.active = active;
    }
}
