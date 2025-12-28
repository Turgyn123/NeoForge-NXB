package net.narutoxboruto.networking.jutsu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.narutoxboruto.attachments.MainAttachment;
import net.narutoxboruto.jutsu.JutsuWheel;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client -> Server packet to select a jutsu slot.
 */
public class SelectJutsuSlotPacket implements CustomPacketPayload {
    private final int slot;
    
    public static final Type<SelectJutsuSlotPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath("narutoxboruto", "select_jutsu_slot")
    );

    public static final StreamCodec<FriendlyByteBuf, SelectJutsuSlotPacket> STREAM_CODEC = 
        StreamCodec.ofMember(SelectJutsuSlotPacket::toBytes, SelectJutsuSlotPacket::new);

    public SelectJutsuSlotPacket(int slot) {
        this.slot = slot;
    }

    public SelectJutsuSlotPacket(FriendlyByteBuf buf) {
        this.slot = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(slot);
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                JutsuWheel wheel = serverPlayer.getData(MainAttachment.JUTSU_WHEEL);
                wheel.setSelectedSlot(slot);
                wheel.syncValue(serverPlayer);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
