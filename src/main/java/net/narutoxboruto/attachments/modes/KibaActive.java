package net.narutoxboruto.attachments.modes;

import com.mojang.serialization.Codec;
import net.minecraft.server.level.ServerPlayer;
import net.narutoxboruto.attachments.MainAttachment;
import net.narutoxboruto.networking.ModPacketHandler;
import net.narutoxboruto.networking.misc.SyncKibaActive;

public class KibaActive {
    private boolean active;

    public static final Codec<KibaActive> CODEC = Codec.BOOL.xmap(KibaActive::new, KibaActive::isActive);

    public KibaActive(boolean active) {
        this.active = active;
    }

    public KibaActive() {
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
        serverPlayer.setData(MainAttachment.KIBA_ACTIVE.get(), this);
        ModPacketHandler.sendToPlayer(new SyncKibaActive(active), serverPlayer);
    }

    // Client-side only method
    public void setActive(boolean active) {
        this.active = active;
    }
}
