package net.narutoxboruto.networking.misc;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.narutoxboruto.attachments.MainAttachment;
import net.narutoxboruto.attachments.climber.ClimberComponent;
import net.narutoxboruto.client.PlayerData;
import net.narutoxboruto.main.Main;
import net.narutoxboruto.util.RotationUtil;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Network packet to synchronize wall running state from server to client.
 */
public record SyncWallRunning(RotationUtil.Surface surface) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<SyncWallRunning> TYPE = 
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Main.MOD_ID, "sync_wall_running"));
    
    public static final StreamCodec<ByteBuf, SyncWallRunning> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            packet -> packet.surface.ordinal(),
            ordinal -> new SyncWallRunning(RotationUtil.Surface.values()[ordinal])
    );
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    public static void handle(SyncWallRunning packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = Minecraft.getInstance().player;
            if (player == null) return;

            // Update client-side PlayerData cache for rendering
            PlayerData.setWallRunningSurface(packet.surface);

            // Update WallRunning attachment so onGround() override works on client
            player.getData(MainAttachment.WALL_RUNNING).setSurface(packet.surface);

            // Update ClimberComponent for movement handling
            ClimberComponent climber = player.getData(MainAttachment.CLIMBER);
            if (packet.surface == RotationUtil.Surface.GROUND) {
                climber.transitionToGround();
            } else if (packet.surface == RotationUtil.Surface.CEILING) {
                climber.transitionToCeiling(Vec3.ZERO);
            } else {
                Direction dir = switch (packet.surface) {
                    case NORTH_WALL -> Direction.NORTH;
                    case SOUTH_WALL -> Direction.SOUTH;
                    case EAST_WALL  -> Direction.EAST;
                    case WEST_WALL  -> Direction.WEST;
                    default -> null;
                };
                if (dir != null) {
                    climber.transitionToWall(dir, Vec3.ZERO);
                }
            }
        });
    }
}