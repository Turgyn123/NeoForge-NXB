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
 * Client -> Server packet to assign a jutsu to a wheel slot.
 */
public class AssignJutsuSlotPacket implements CustomPacketPayload {
    private final int slot;
    private final String jutsuId;
    
    public static final Type<AssignJutsuSlotPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath("narutoxboruto", "assign_jutsu_slot")
    );

    public static final StreamCodec<FriendlyByteBuf, AssignJutsuSlotPacket> STREAM_CODEC = 
        StreamCodec.ofMember(AssignJutsuSlotPacket::toBytes, AssignJutsuSlotPacket::new);

    public AssignJutsuSlotPacket(int slot, String jutsuId) {
        this.slot = slot;
        this.jutsuId = jutsuId;
    }

    public AssignJutsuSlotPacket(FriendlyByteBuf buf) {
        this.slot = buf.readInt();
        this.jutsuId = buf.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(slot);
        buf.writeUtf(jutsuId);
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                JutsuWheel wheel = serverPlayer.getData(MainAttachment.JUTSU_WHEEL);
                wheel.setJutsuInSlot(slot, jutsuId);
                wheel.syncValue(serverPlayer);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
