package net.narutoxboruto.networking.jutsu;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.narutoxboruto.client.PlayerData;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server -> Client packet to sync the jutsu wheel state.
 */
public class SyncJutsuWheel implements CustomPacketPayload {
    private final String wheelData;

    public static final Type<SyncJutsuWheel> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath("narutoxboruto", "sync_jutsu_wheel")
    );

    public static final StreamCodec<FriendlyByteBuf, SyncJutsuWheel> STREAM_CODEC = 
        StreamCodec.ofMember(SyncJutsuWheel::toBytes, SyncJutsuWheel::new);

    public SyncJutsuWheel(String wheelData) {
        this.wheelData = wheelData;
    }

    public SyncJutsuWheel(FriendlyByteBuf buf) {
        this.wheelData = buf.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(wheelData);
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            // Update client-side player data
            PlayerData.setJutsuWheelData(wheelData);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
