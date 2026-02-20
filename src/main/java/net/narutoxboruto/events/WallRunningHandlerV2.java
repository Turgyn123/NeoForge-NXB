package net.narutoxboruto.events;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.narutoxboruto.main.Main;
import net.narutoxboruto.attachments.MainAttachment;
import net.narutoxboruto.attachments.climber.ClimberComponent;
import net.narutoxboruto.attachments.modes.ChakraControl;
import net.narutoxboruto.attachments.modes.WallRunning;
import net.narutoxboruto.util.RotationUtil;

/**
 * Server-side wall running detection and state management.
 *
 * Detection flow each tick:
 *   1. Check chakra control is active and player has chakra
 *   2. Based on current surface, detect transitions:
 *      - GROUND:  walk into a wall → attach
 *      - WALL:    wall still present → stay; top of wall + ceiling → ceiling;
 *                 wall gone → corner / ceiling / ground fallback
 *      - CEILING: ceiling present → stay; edge + wall → wall; no ceiling → ground
 *   3. Update WallRunning attachment (auto-syncs packet to client)
 *   4. Update ClimberComponent for movement orientation
 *
 * DISABLED - Wall running postponed to a future update.
 */
//@EventBusSubscriber(modid = Main.MOD_ID)
public class WallRunningHandlerV2 {

    /** Max distance (player center → wall face) for initial attachment. */
    private static final double WALL_ATTACH_RANGE = 0.55;

    /** Max distance to remain attached (more lenient to prevent jitter). */
    private static final double WALL_STAY_RANGE = 0.9;

    /** Minimum velocity component toward a wall to trigger attachment. */
    private static final double MIN_APPROACH_SPEED = 0.01;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ChakraControl chakraControl = player.getData(MainAttachment.CHAKRA_CONTROL);
        WallRunning wallRunning = player.getData(MainAttachment.WALL_RUNNING);
        ClimberComponent climber = player.getData(MainAttachment.CLIMBER);
        int chakra = player.getData(MainAttachment.CHAKRA).getValue();
        Level level = player.level();

        RotationUtil.Surface prevSurface = wallRunning.getSurface();

        // Advance climber interpolation
        climber.tick(0.05f);

        // Guard: chakra control off or no chakra → reset to ground
        if (!chakraControl.isActive() || chakra <= 0) {
            if (prevSurface != RotationUtil.Surface.GROUND) {
                applySurface(player, wallRunning, climber, RotationUtil.Surface.GROUND);
            }
            return;
        }

        // Detect which surface the player should be on
        RotationUtil.Surface newSurface = detectSurface(player, level, prevSurface);

        if (newSurface != prevSurface) {
            applySurface(player, wallRunning, climber, newSurface);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  D E T E C T I O N
    // ═══════════════════════════════════════════════════════════════════

    private static RotationUtil.Surface detectSurface(
            ServerPlayer player, Level level, RotationUtil.Surface current) {
        return switch (current) {
            case GROUND  -> detectFromGround(player, level);
            case CEILING -> detectFromCeiling(player, level);
            default      -> detectFromWall(player, level, current);
        };
    }

    // ─── From GROUND ────────────────────────────────────────────────

    private static RotationUtil.Surface detectFromGround(ServerPlayer player, Level level) {
        Vec3 pos = player.position();
        BlockPos bp = player.blockPosition();
        Vec3 vel = player.getDeltaMovement();

        Direction bestWall = null;
        double bestDist = Double.MAX_VALUE;

        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (!hasWall(level, bp, dir)) continue;

            double dist = distToFace(pos, bp, dir);
            if (dist > WALL_ATTACH_RANGE) continue;

            // Must be moving toward the wall
            if (velocityToward(vel, dir) < MIN_APPROACH_SPEED) continue;

            if (dist < bestDist) {
                bestDist = dist;
                bestWall = dir;
            }
        }

        return bestWall != null ? dirToSurface(bestWall) : RotationUtil.Surface.GROUND;
    }

    // ─── From WALL ──────────────────────────────────────────────────

    private static RotationUtil.Surface detectFromWall(
            ServerPlayer player, Level level, RotationUtil.Surface current) {

        Direction wallDir = surfaceToDir(current);
        if (wallDir == null) return RotationUtil.Surface.GROUND;

        BlockPos bp = player.blockPosition();
        Vec3 pos = player.position();
        Vec3 vel = player.getDeltaMovement();

        // 1. Is the wall still present and in range?
        boolean wallHere = hasWall(level, bp, wallDir)
                && distToFace(pos, bp, wallDir) <= WALL_STAY_RANGE;

        if (wallHere) {
            // Wall present → check for ceiling transition at the top edge
            if (vel.y > 0.03) {
                BlockPos twoAbove = bp.above(2);
                boolean ceilingExists = level.getBlockState(twoAbove).isSolid();
                boolean wallEndsAbove = !hasWall(level, bp.above(2), wallDir);
                if (ceilingExists && wallEndsAbove) {
                    return RotationUtil.Surface.CEILING;
                }
            }
            return current;  // stay on this wall
        }

        // 2. Wall gone – try transitions
        //    Ceiling
        if (vel.y > 0.01) {
            BlockPos ceilCheck = bp.above(2);
            if (level.getBlockState(ceilCheck).isSolid()) {
                return RotationUtil.Surface.CEILING;
            }
        }
        //    Corner (perpendicular wall)
        for (Direction perp : Direction.Plane.HORIZONTAL) {
            if (perp == wallDir || perp == wallDir.getOpposite()) continue;
            if (hasWall(level, bp, perp) && distToFace(pos, bp, perp) < WALL_STAY_RANGE) {
                return dirToSurface(perp);
            }
        }
        //    Ground
        if (level.getBlockState(bp.below()).isSolid()) {
            double yFrac = pos.y - Math.floor(pos.y);
            if (yFrac < 0.25) return RotationUtil.Surface.GROUND;
        }

        // Default: lost wall → detach
        return RotationUtil.Surface.GROUND;
    }

    // ─── From CEILING ───────────────────────────────────────────────

    private static RotationUtil.Surface detectFromCeiling(ServerPlayer player, Level level) {
        BlockPos bp = player.blockPosition();
        Vec3 pos = player.position();
        Vec3 vel = player.getDeltaMovement();

        BlockPos ceilBlock = bp.above(2);
        if (!level.getBlockState(ceilBlock).isSolid()) {
            // No ceiling – try walls, then ground
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                if (hasWall(level, bp, dir) && distToFace(pos, bp, dir) < WALL_STAY_RANGE) {
                    return dirToSurface(dir);
                }
            }
            return RotationUtil.Surface.GROUND;
        }

        // Ceiling still solid – check for edge → wall transition
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (!hasWall(level, bp, dir)) continue;
            if (velocityToward(vel, dir) < MIN_APPROACH_SPEED) continue;
            if (distToFace(pos, bp, dir) < WALL_ATTACH_RANGE) {
                return dirToSurface(dir);
            }
        }

        return RotationUtil.Surface.CEILING;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  H E L P E R S
    // ═══════════════════════════════════════════════════════════════════

    /** Solid blocks adjacent at feet or head level in the given direction. */
    private static boolean hasWall(Level level, BlockPos playerBlock, Direction dir) {
        BlockPos feetWall = playerBlock.relative(dir);
        return level.getBlockState(feetWall).isSolid()
                || level.getBlockState(feetWall.above()).isSolid();
    }

    /**
     * Distance from the player center to the block-boundary face in the given
     * direction.  0 = touching the boundary, approaches 1.0 at the far side.
     */
    private static double distToFace(Vec3 pos, BlockPos bp, Direction dir) {
        return switch (dir) {
            case NORTH -> pos.z - bp.getZ();
            case SOUTH -> (bp.getZ() + 1.0) - pos.z;
            case WEST  -> pos.x - bp.getX();
            case EAST  -> (bp.getX() + 1.0) - pos.x;
            default    -> Double.MAX_VALUE;
        };
    }

    /** Positive value = moving toward the given direction. */
    private static double velocityToward(Vec3 velocity, Direction dir) {
        return switch (dir) {
            case NORTH -> -velocity.z;
            case SOUTH ->  velocity.z;
            case WEST  -> -velocity.x;
            case EAST  ->  velocity.x;
            default    -> 0;
        };
    }

    // ═══════════════════════════════════════════════════════════════════
    //  S T A T E   U P D A T E
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Apply a surface change.  Updates the WallRunning attachment (which
     * auto-sends the sync packet) and the ClimberComponent.
     */
    private static void applySurface(
            ServerPlayer player, WallRunning wallRunning,
            ClimberComponent climber, RotationUtil.Surface surface) {

        // setSurface(surface, player) updates the attachment AND sends SyncWallRunning
        wallRunning.setSurface(surface, player);

        if (surface == RotationUtil.Surface.GROUND) {
            climber.transitionToGround();
        } else if (surface == RotationUtil.Surface.CEILING) {
            climber.transitionToCeiling(Vec3.ZERO);
        } else {
            Direction dir = surfaceToDir(surface);
            if (dir != null) climber.transitionToWall(dir, Vec3.ZERO);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  C O N V E R S I O N
    // ═══════════════════════════════════════════════════════════════════

    private static RotationUtil.Surface dirToSurface(Direction dir) {
        return switch (dir) {
            case NORTH -> RotationUtil.Surface.NORTH_WALL;
            case SOUTH -> RotationUtil.Surface.SOUTH_WALL;
            case EAST  -> RotationUtil.Surface.EAST_WALL;
            case WEST  -> RotationUtil.Surface.WEST_WALL;
            default    -> RotationUtil.Surface.GROUND;
        };
    }

    private static Direction surfaceToDir(RotationUtil.Surface surface) {
        return switch (surface) {
            case NORTH_WALL -> Direction.NORTH;
            case SOUTH_WALL -> Direction.SOUTH;
            case EAST_WALL  -> Direction.EAST;
            case WEST_WALL  -> Direction.WEST;
            case CEILING    -> Direction.UP;
            default         -> null;
        };
    }
}
