package net.narutoxboruto.attachments.modes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Direction;
import net.narutoxboruto.util.RotationUtil;

/**
 * Data attachment for storing which surface the player is wall running on.
 * Mirrors the Gravity API approach but simplified for wall running only.
 */
public class WallRunning {
    
    public static final Codec<WallRunning> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("surface").forGetter(wr -> wr.surface.name())
            ).apply(instance, surfaceName -> new WallRunning(RotationUtil.Surface.valueOf(surfaceName)))
    );
    
    private RotationUtil.Surface surface;
    private int ticksOnSurface; // Track how long on this surface
    private boolean wasSprinting; // Track if player was sprinting when they started wall running
    
    public WallRunning() {
        this(RotationUtil.Surface.GROUND);
    }
    
    public WallRunning(RotationUtil.Surface surface) {
        this.surface = surface;
        this.ticksOnSurface = 0;
        this.wasSprinting = false;
    }
    
    public RotationUtil.Surface getSurface() {
        return surface;
    }
    
    public void setSurface(RotationUtil.Surface surface) {
        if (this.surface != surface) {
            this.surface = surface;
            this.ticksOnSurface = 0;
        }
    }
    
    public boolean isWallRunning() {
        return surface != RotationUtil.Surface.GROUND && surface != RotationUtil.Surface.CEILING;
    }

    /**
     * True when on ANY non-ground surface (wall or ceiling).
     * Used by onGround() override to suppress gravity and fall damage.
     */
    public boolean isOnSurface() {
        return surface != RotationUtil.Surface.GROUND;
    }
    
    public int getTicksOnSurface() {
        return ticksOnSurface;
    }
    
    public void incrementTicks() {
        ticksOnSurface++;
    }
    
    public void resetTicks() {
        ticksOnSurface = 0;
    }
    
    public boolean wasSprintingWhenStarted() {
        return wasSprinting;
    }
    
    public void setWasSprinting(boolean wasSprinting) {
        this.wasSprinting = wasSprinting;
    }
    
    public Direction getDirection() {
        return surface.getDirection();
    }
    
    /**
     * Reset to ground surface
     */
    public void reset(net.minecraft.server.level.ServerPlayer player) {
        this.surface = RotationUtil.Surface.GROUND;
        this.ticksOnSurface = 0;
        // Send packet to client
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
            player, 
            new net.narutoxboruto.networking.misc.SyncWallRunning(this.surface)
        );
    }
    
    /**
     * Set surface with player param (for compatibility)
     */
    public void setSurface(RotationUtil.Surface surface, net.minecraft.server.level.ServerPlayer player) {
        if (this.surface != surface) {
            setSurface(surface);
            // Send packet to client to sync surface state
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                player, 
                new net.narutoxboruto.networking.misc.SyncWallRunning(surface)
            );
        }
    }
    
    /**
     * Public alias for the RotationUtil.Surface enum
     */
    public static class Surface {
        public static final RotationUtil.Surface GROUND = RotationUtil.Surface.GROUND;
        public static final RotationUtil.Surface NORTH_WALL = RotationUtil.Surface.NORTH_WALL;
        public static final RotationUtil.Surface SOUTH_WALL = RotationUtil.Surface.SOUTH_WALL;
        public static final RotationUtil.Surface EAST_WALL = RotationUtil.Surface.EAST_WALL;
        public static final RotationUtil.Surface WEST_WALL = RotationUtil.Surface.WEST_WALL;
        public static final RotationUtil.Surface CEILING = RotationUtil.Surface.CEILING;
    }
    
    /**
     * Get surface from direction
     */
    public static RotationUtil.Surface surfaceFromDirection(Direction dir) {
        return switch (dir) {
            case NORTH -> RotationUtil.Surface.NORTH_WALL;
            case SOUTH -> RotationUtil.Surface.SOUTH_WALL;
            case EAST -> RotationUtil.Surface.EAST_WALL;
            case WEST -> RotationUtil.Surface.WEST_WALL;
            case UP -> RotationUtil.Surface.CEILING;
            default -> RotationUtil.Surface.GROUND;
        };
    }
}