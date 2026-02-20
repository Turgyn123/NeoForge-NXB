package net.narutoxboruto.networking;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.narutoxboruto.networking.info.*;
import net.narutoxboruto.networking.jutsu.*;
import net.narutoxboruto.networking.jutsus.*;
import net.narutoxboruto.networking.misc.*;
import net.narutoxboruto.networking.stats.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModPacketHandler {

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1.0.0"); // Your network version
         //Clientbound packets
        registrar.playToClient(SyncChakra.TYPE, SyncChakra.STREAM_CODEC, (payload, context) -> payload.handle(context));
        registrar.playToClient(SyncMaxChakra.TYPE, SyncMaxChakra.STREAM_CODEC, (payload, context) -> payload.handle(context));
        registrar.playToClient(SyncAffiliation.TYPE, SyncAffiliation.STREAM_CODEC, (payload, context) -> payload.handle(context));
        registrar.playToClient(SyncClan.TYPE, SyncClan.STREAM_CODEC, (payload, context) -> payload.handle(context));
        registrar.playToClient(SyncRank.TYPE, SyncRank.STREAM_CODEC, (payload, context) -> payload.handle(context));
        registrar.playToClient(SyncReleaseList.TYPE, SyncReleaseList.STREAM_CODEC, (payload, context) -> payload.handle(context));
        registrar.playToClient(SyncShinobiPoints.TYPE, SyncShinobiPoints.STREAM_CODEC, (payload, context) -> payload.handle(context));

        registrar.playToClient(SyncEarthList.TYPE, SyncEarthList.STREAM_CODEC, (payload, context) -> payload.handle(context));
        registrar.playToClient(SyncFireList.TYPE, SyncFireList.STREAM_CODEC, (payload, context) -> payload.handle(context));
        registrar.playToClient(SyncLightningList.TYPE, SyncLightningList.STREAM_CODEC, (payload, context) -> payload.handle(context));
        registrar.playToClient(SyncWindList.TYPE, SyncWindList.STREAM_CODEC, (payload, context) -> payload.handle(context));
        registrar.playToClient(SyncWaterList.TYPE, SyncWaterList.STREAM_CODEC, (payload, context) -> payload.handle(context));
        registrar.playToClient(SyncYangList.TYPE, SyncYangList.STREAM_CODEC, (payload, context) -> payload.handle(context));
        registrar.playToClient(SyncYingList.TYPE, SyncYingList.STREAM_CODEC, (payload, context) -> payload.handle(context));

        registrar.playToClient(SyncGenjutsu.TYPE, SyncGenjutsu.STREAM_CODEC, (payload, context) -> payload.handle(context));
        registrar.playToClient(SyncKenjutsu.TYPE, SyncKenjutsu.STREAM_CODEC, (payload, context) -> payload.handle(context));
        registrar.playToClient(SyncKinjutsu.TYPE, SyncKinjutsu.STREAM_CODEC, (payload, context) -> payload.handle(context));
        registrar.playToClient(SyncMedical.TYPE, SyncMedical.STREAM_CODEC, (payload, context) -> payload.handle(context));
        registrar.playToClient(SyncNinjutsu.TYPE, SyncNinjutsu.STREAM_CODEC, (payload, context) -> payload.handle(context));
        registrar.playToClient(SyncSenjutsu.TYPE, SyncSenjutsu.STREAM_CODEC, (payload, context) -> payload.handle(context));
        registrar.playToClient(SyncShurikenjutsu.TYPE, SyncShurikenjutsu.STREAM_CODEC, (payload, context) -> payload.handle(context));
        registrar.playToClient(SyncSummoning.TYPE, SyncSummoning.STREAM_CODEC, (payload, context) -> payload.handle(context));
        registrar.playToClient(SyncTaijutsu.TYPE, SyncTaijutsu.STREAM_CODEC, (payload, context) -> payload.handle(context));
        registrar.playToClient(SyncSpeed.TYPE, SyncSpeed.STREAM_CODEC, (payload, context) -> payload.handle(context));
        registrar.playToClient(SyncChakraControl.TYPE, SyncChakraControl.STREAM_CODEC, (payload, context) -> payload.handle(context));
        registrar.playToClient(SyncKibaActive.TYPE, SyncKibaActive.STREAM_CODEC, (payload, context) -> payload.handle(context));
        registrar.playToClient(SyncLightningChakraModeActive.TYPE, SyncLightningChakraModeActive.STREAM_CODEC, (payload, context) -> payload.handle(context));
        // DISABLED - Wall running postponed to a future update
        // registrar.playToClient(SyncWallRunning.TYPE, SyncWallRunning.STREAM_CODEC, SyncWallRunning::handle);

        //Serverbound packets
        registrar.playToServer(ToggleSwordAbility.TYPE, ToggleSwordAbility.STREAM_CODEC, (payload, context) -> payload.handle(context));
        registrar.playToServer(RechargeChakra.TYPE, RechargeChakra.STREAM_CODEC, (payload, context) -> payload.handle(context));
        registrar.playToClient(SyncNarutoRun.TYPE, SyncNarutoRun.STREAM_CODEC, (payload, context) -> payload.handle(context));
        registrar.playToServer(ToggleChakraControl.TYPE, ToggleChakraControl.STREAM_CODEC, (payload, context) -> payload.handle(context));
        registrar.playToServer(SpecialThrowPacket.TYPE, SpecialThrowPacket.STREAM_CODEC, (payload, context) -> payload.handle(context));
        
        // Jutsu Storage packets
        registrar.playToClient(SyncJutsuStorage.TYPE, SyncJutsuStorage.STREAM_CODEC, (payload, context) -> payload.handle(context));
        registrar.playToServer(OpenJutsuStoragePacket.TYPE, OpenJutsuStoragePacket.STREAM_CODEC, OpenJutsuStoragePacket::handle);
    }

    public static void sendToPlayer(CustomPacketPayload packet, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    public static void sendToServer(CustomPacketPayload packet) {
        PacketDistributor.sendToServer(packet);
    }
}
