package net.narutoxboruto.networking.misc;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.narutoxboruto.attachments.MainAttachment;
import net.narutoxboruto.attachments.info.Chakra;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class RechargeChakra implements CustomPacketPayload {

    public static final Type<RechargeChakra> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("narutoxboruto", "recharge_chakra"));

    public static final StreamCodec<FriendlyByteBuf, RechargeChakra> STREAM_CODEC = StreamCodec.ofMember(RechargeChakra::toBytes, RechargeChakra::new);

    public RechargeChakra() {
        // Empty constructor for creation
    }

    // Proper deserialization constructor
    public RechargeChakra(FriendlyByteBuf buf) {
        // Add any data reading here if your packet needs to send data
    }

    // Proper serialization method
    public void toBytes(FriendlyByteBuf buf) {
        // Add any data writing here if your packet needs to send data
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer serverPlayer = (ServerPlayer) context.player();

            // Use the data attachment system directly
            Chakra chakra = serverPlayer.getData(MainAttachment.CHAKRA);
            chakra.addValue(1, serverPlayer);
            
            // Apply Slowness 2 while charging chakra (lasts 10 ticks = 0.5 seconds, refreshed each tick while holding)
            serverPlayer.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 1, false, true));
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
