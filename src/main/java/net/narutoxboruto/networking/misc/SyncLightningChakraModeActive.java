package net.narutoxboruto.networking.misc;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.narutoxboruto.client.PlayerData;

/**
 * Network packet to sync Lightning Chakra Mode active state to client.
 */
public class SyncLightningChakraModeActive implements CustomPacketPayload {
    private final boolean active;

    public static final CustomPacketPayload.Type<SyncLightningChakraModeActive> TYPE = 
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("narutoxboruto", "sync_lightning_chakra_mode_active"));

    public static final StreamCodec<FriendlyByteBuf, SyncLightningChakraModeActive> STREAM_CODEC = 
            StreamCodec.ofMember(SyncLightningChakraModeActive::toBytes, SyncLightningChakraModeActive::new);

    public SyncLightningChakraModeActive(boolean active) {
        this.active = active;
    }

    public SyncLightningChakraModeActive(FriendlyByteBuf buf) {
        this.active = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(active);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            PlayerData.setLightningChakraModeActive(active);
        });
    }
}
