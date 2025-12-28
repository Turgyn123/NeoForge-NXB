package net.narutoxboruto.networking.jutsu;

import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.narutoxboruto.attachments.MainAttachment;
import net.narutoxboruto.jutsu.JutsuCaster;
import net.narutoxboruto.util.ModUtil;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client -> Server packet to cast a jutsu.
 */
public class CastJutsuPacket implements CustomPacketPayload {
    
    public static final Type<CastJutsuPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath("narutoxboruto", "cast_jutsu")
    );

    public static final StreamCodec<FriendlyByteBuf, CastJutsuPacket> STREAM_CODEC = 
        StreamCodec.ofMember(CastJutsuPacket::toBytes, CastJutsuPacket::new);

    public CastJutsuPacket() {
    }

    public CastJutsuPacket(FriendlyByteBuf buf) {
        // No data needed - server will use player's selected jutsu
    }

    public void toBytes(FriendlyByteBuf buf) {
        // No data needed
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                JutsuCaster.tryCastSelectedJutsu(serverPlayer);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
