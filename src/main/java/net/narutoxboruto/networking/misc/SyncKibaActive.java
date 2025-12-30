package net.narutoxboruto.networking.misc;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.narutoxboruto.client.PlayerData;

public class SyncKibaActive implements CustomPacketPayload {
    private final boolean active;

    public static final CustomPacketPayload.Type<SyncKibaActive> TYPE = 
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("narutoxboruto", "sync_kiba_active"));

    public static final StreamCodec<FriendlyByteBuf, SyncKibaActive> STREAM_CODEC = 
            StreamCodec.ofMember(SyncKibaActive::toBytes, SyncKibaActive::new);

    public SyncKibaActive(boolean active) {
        this.active = active;
    }

    public SyncKibaActive(FriendlyByteBuf buf) {
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
            PlayerData.setKibaActive(active);
        });
    }
}
