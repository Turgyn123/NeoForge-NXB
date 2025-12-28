package net.narutoxboruto.networking.jutsu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.narutoxboruto.attachments.MainAttachment;
import net.narutoxboruto.attachments.jutsus.JutsuStorage;
import net.narutoxboruto.main.Main;
import net.narutoxboruto.menu.JutsuStorageMenu;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Packet sent from client when player wants to open their Jutsu Storage.
 * Server responds by opening the JutsuStorageMenu.
 */
public record OpenJutsuStoragePacket() implements CustomPacketPayload {

    public static final Type<OpenJutsuStoragePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Main.MOD_ID, "open_jutsu_storage")
    );

    public static final StreamCodec<FriendlyByteBuf, OpenJutsuStoragePacket> STREAM_CODEC = StreamCodec.unit(new OpenJutsuStoragePacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenJutsuStoragePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                JutsuStorage storage = serverPlayer.getData(MainAttachment.JUTSU_STORAGE);
                
                serverPlayer.openMenu(new SimpleMenuProvider(
                        (containerId, playerInventory, player) -> 
                                new JutsuStorageMenu(containerId, playerInventory, storage.toItemStackHandler()),
                        Component.translatable("container.narutoxboruto.jutsu_storage")
                ));
            }
        });
    }
}
