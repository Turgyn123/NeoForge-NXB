package net.narutoxboruto.networking.misc;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.narutoxboruto.attachments.MainAttachment;
import net.narutoxboruto.attachments.info.Chakra;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

public class SyncChakraControl implements CustomPacketPayload {
    private final boolean value;

    public static final Type<SyncChakraControl> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("narutoxboruto", "sync_chakra_control"));

    public static final StreamCodec<FriendlyByteBuf, SyncChakraControl> STREAM_CODEC = StreamCodec.ofMember(SyncChakraControl::toBytes, SyncChakraControl::new);

    public SyncChakraControl(boolean value) {
        this.value = value;
    }

    public SyncChakraControl(FriendlyByteBuf buf) {
        this.value = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(value);
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            context.player().getData(MainAttachment.CHAKRA_CONTROL);
            // Update client-side cache for jutsu casting
            net.narutoxboruto.client.PlayerData.setChakraControlActive(value);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
