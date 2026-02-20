package net.narutoxboruto.networking.misc;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.narutoxboruto.items.throwables.FumaShurikenItem;
import net.narutoxboruto.items.throwables.ThrowableWeaponItem;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class SpecialThrowPacket implements CustomPacketPayload {

    public static final Type<SpecialThrowPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("narutoxboruto", "special_throw"));

    public static final StreamCodec<FriendlyByteBuf, SpecialThrowPacket> STREAM_CODEC = StreamCodec.ofMember(SpecialThrowPacket::toBytes, SpecialThrowPacket::new);

    public SpecialThrowPacket() {
        // Empty constructor for creation
    }

    // Proper deserialization constructor
    public SpecialThrowPacket(FriendlyByteBuf buf) {
        // No data to read
    }

    // Proper serialization method
    public void toBytes(FriendlyByteBuf buf) {
        // No data to write
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer serverPlayer = (ServerPlayer) context.player();
            ItemStack stack = serverPlayer.getItemInHand(InteractionHand.MAIN_HAND);
            
            // Only work with throwable items (excluding Fuma Shuriken)
            if (stack.getItem() instanceof ThrowableWeaponItem throwableItem && 
                !(stack.getItem() instanceof FumaShurikenItem)) {
                throwableItem.performSpecialThrow(serverPlayer, stack);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
