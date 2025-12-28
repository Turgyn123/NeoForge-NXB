package net.narutoxboruto.networking.jutsu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.narutoxboruto.attachments.MainAttachment;
import net.narutoxboruto.attachments.jutsus.JutsuStorage;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Syncs the jutsu storage from server to client.
 */
public class SyncJutsuStorage implements CustomPacketPayload {
    
    private final CompoundTag storageData;
    
    public static final Type<SyncJutsuStorage> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath("narutoxboruto", "sync_jutsu_storage")
    );
    
    public static final StreamCodec<FriendlyByteBuf, SyncJutsuStorage> STREAM_CODEC = StreamCodec.of(
        (buf, value) -> buf.writeNbt(value.storageData),
        buf -> new SyncJutsuStorage(buf.readNbt())
    );
    
    public SyncJutsuStorage(CompoundTag storageData) {
        this.storageData = storageData;
    }
    
    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                JutsuStorage storage = JutsuStorage.fromNbt(this.storageData);
                player.setData(MainAttachment.JUTSU_STORAGE, storage);
            }
        });
    }
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
