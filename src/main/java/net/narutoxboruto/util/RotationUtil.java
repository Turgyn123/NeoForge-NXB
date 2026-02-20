package net.narutoxboruto.util;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Utility class for gravity/orientation rotation calculations.
 * Based on patterns from Gravity API - handles all coordinate transforms
 * between world space and player-relative space for different surfaces.
 * 
 * Key concepts:
 * - World Space: Standard Minecraft coordinates (Y = up)
 * - Player Space: Coordinates relative to current surface (player's Y = surface normal)
 * 
 * For wall running:
 * - Player's +Y axis = direction away from wall (surface normal)
 * - Player's movement is calculated as if walking on ground, then transformed
 */
public class RotationUtil {
    
    /**
     * Surface enum for wall running - defines which surface the player is on.
     * Kept in RotationUtil for future wall running reimplementation.
     */
    public enum Surface {
        GROUND(Direction.UP),
        NORTH_WALL(Direction.NORTH),
        SOUTH_WALL(Direction.SOUTH),
        EAST_WALL(Direction.EAST),
        WEST_WALL(Direction.WEST),
        CEILING(Direction.DOWN);
        
        private final Direction direction;
        
        Surface(Direction direction) {
            this.direction = direction;
        }
        
        public Direction getDirection() {
            return direction;
        }
    }
    
    // Pre-computed quaternions for each surface
    // WORLD_ROTATION: Rotates FROM player space TO world space
    // CAMERA_ROTATION: Rotates the camera view to match the new "up"
    private static final Quaternionf[] WORLD_ROTATION_QUATERNIONS = new Quaternionf[6];
    private static final Quaternionf[] CAMERA_ROTATION_QUATERNIONS = new Quaternionf[6];
    
    static {
        // GROUND - no rotation, player space = world space
        WORLD_ROTATION_QUATERNIONS[0] = new Quaternionf();
        CAMERA_ROTATION_QUATERNIONS[0] = new Quaternionf();
        
        // For wall running, we need:
        // - Player +Z (forward) → World +Y (climbing up)
        // - Player +Y (up, away from surface) → World direction away from wall
        // - Player +X (right) → horizontal along wall
        //
        // Step 1: Rotate -90° around X axis to make Z→Y
        //   After this: X→X, Y→-Z, Z→Y
        // Step 2: Rotate around Y axis to orient the wall direction
        //   SOUTH_WALL: no rotation needed (Y→-Z is correct, wall at +Z)
        //   NORTH_WALL: 180° around Y to flip -Z→+Z (wall at -Z, need Y→+Z)
        //   EAST_WALL: -90° around Y to make -Z→-X (wall at +X, need Y→-X)
        //   WEST_WALL: +90° around Y to make -Z→+X (wall at -X, need Y→+X)
        
        // NORTH_WALL - Wall at -Z, player's feet point toward -Z
        // Rotate 90° around X axis so forward becomes up and feet point at wall
        WORLD_ROTATION_QUATERNIONS[1] = new Quaternionf()
            .rotateX((float) Math.toRadians(90));
        CAMERA_ROTATION_QUATERNIONS[1] = new Quaternionf()
            .rotateX((float) Math.toRadians(-90));
        
        // SOUTH_WALL - Wall at +Z, player's feet point toward +Z
        // Rotate -90° around X axis so forward becomes up and feet point at wall
        WORLD_ROTATION_QUATERNIONS[2] = new Quaternionf()
            .rotateX((float) Math.toRadians(-90));
        CAMERA_ROTATION_QUATERNIONS[2] = new Quaternionf()
            .rotateX((float) Math.toRadians(90));
        
        // EAST_WALL - Wall at +X, player's feet point toward +X
        // Rotate 90° around Z axis so forward becomes up and feet point at wall
        WORLD_ROTATION_QUATERNIONS[3] = new Quaternionf()
            .rotateZ((float) Math.toRadians(90));
        CAMERA_ROTATION_QUATERNIONS[3] = new Quaternionf()
            .rotateZ((float) Math.toRadians(-90));
        
        // WEST_WALL - Wall at -X, player's feet point toward -X
        // Rotate -90° around Z axis so forward becomes up and feet point at wall
        WORLD_ROTATION_QUATERNIONS[4] = new Quaternionf()
            .rotateZ((float) Math.toRadians(-90));
        CAMERA_ROTATION_QUATERNIONS[4] = new Quaternionf()
            .rotateZ((float) Math.toRadians(90));
        
        // CEILING - upside down, player's +Y should point toward -Y (world down)
        // Player +Z (forward) still maps to world +Z
        // Rotate 180° around Z (or X - both work for ceiling)
        WORLD_ROTATION_QUATERNIONS[5] = new Quaternionf().rotationZ((float) Math.toRadians(180));
        CAMERA_ROTATION_QUATERNIONS[5] = new Quaternionf().rotationZ((float) Math.toRadians(180));
    }
    
    /**
     * Get the index for a surface in our quaternion arrays.
     */
    public static int getSurfaceIndex(Surface surface) {
        return switch (surface) {
            case GROUND -> 0;
            case NORTH_WALL -> 1;
            case SOUTH_WALL -> 2;
            case EAST_WALL -> 3;
            case WEST_WALL -> 4;
            case CEILING -> 5;
        };
    }
    
    /**
     * Get the quaternion to rotate FROM player space TO world space.
     * Use this when converting player-relative movement to world movement.
     */
    public static Quaternionf getWorldRotationQuaternion(Surface surface) {
        return new Quaternionf(WORLD_ROTATION_QUATERNIONS[getSurfaceIndex(surface)]);
    }
    
    /**
     * Get the quaternion to rotate the CAMERA for this surface.
     * This is applied to make the view oriented correctly for the current gravity.
     */
    public static Quaternionf getCameraRotationQuaternion(Surface surface) {
        return new Quaternionf(CAMERA_ROTATION_QUATERNIONS[getSurfaceIndex(surface)]);
    }
    
    /**
     * Get rotation between two directions (like Gravity API's getRotationBetween).
     * Returns quaternion that rotates from d1 to d2.
     */
    public static Quaternionf getRotationBetween(Direction d1, Direction d2) {
        if (d1 == d2) return new Quaternionf();
        if (d1.getOpposite() == d2) {
            // 180 degree rotation - use Z axis for consistency
            return new Quaternionf().fromAxisAngleDeg(new Vector3f(0, 0, 1), 180.0f);
        }
        
        Vec3 start = Vec3.atLowerCornerOf(d1.getNormal());
        Vec3 end = Vec3.atLowerCornerOf(d2.getNormal());
        return getRotationBetween(start, end);
    }
    
    /**
     * Get quaternion that rotates vector 'from' to vector 'to'.
     */
    public static Quaternionf getRotationBetween(Vec3 from, Vec3 to) {
        Vec3 cross = from.cross(to);
        double dot = from.dot(to);
        
        // Handle parallel vectors
        if (cross.lengthSqr() < 0.0001) {
            if (dot > 0) {
                return new Quaternionf(); // Same direction
            } else {
                // Opposite direction - rotate 180° around perpendicular axis
                return new Quaternionf().fromAxisAngleDeg(new Vector3f(0, 0, 1), 180.0f);
            }
        }
        
        // Quaternion from axis-angle
        Vec3 axis = cross.normalize();
        float angle = (float) Math.acos(Math.max(-1, Math.min(1, dot)));
        
        return new Quaternionf().fromAxisAngleRad(
            new Vector3f((float) axis.x, (float) axis.y, (float) axis.z), 
            angle
        );
    }
    
    /**
     * Transform a vector from WORLD space to PLAYER space.
     * In player space, +Y is always "up" relative to the current surface.
     */
    public static Vec3 vecWorldToPlayer(Vec3 vec, Surface surface) {
        if (surface == Surface.GROUND) return vec;
        
        Vector3f v = new Vector3f((float) vec.x, (float) vec.y, (float) vec.z);
        // To go FROM world TO player, we use inverse of world rotation
        Quaternionf inverseRotation = getWorldRotationQuaternion(surface).invert();
        v.rotate(inverseRotation);
        return new Vec3(v.x, v.y, v.z);
    }
    
    /**
     * Transform a vector from PLAYER space to WORLD space.
     * Takes a vector where +Y is "up" relative to player and converts to world coordinates.
     */
    public static Vec3 vecPlayerToWorld(Vec3 vec, Surface surface) {
        if (surface == Surface.GROUND) return vec;
        
        Vector3f v = new Vector3f((float) vec.x, (float) vec.y, (float) vec.z);
        v.rotate(getWorldRotationQuaternion(surface));
        return new Vec3(v.x, v.y, v.z);
    }
    
    /**
     * Transform player rotation (yaw, pitch) from player-relative to world-relative.
     * This is like Gravity API's rotPlayerToWorld.
     * 
     * @param yaw Player's yaw in degrees
     * @param pitch Player's pitch in degrees
     * @param surface Current surface
     * @return Vec2(worldYaw, worldPitch)
     */
    public static Vec2 rotPlayerToWorld(float yaw, float pitch, Surface surface) {
        // Convert rotation to look direction vector
        Vec3 lookDir = rotToVec(yaw, pitch);
        // Transform to world space
        Vec3 worldLook = vecPlayerToWorld(lookDir, surface);
        // Convert back to yaw/pitch
        return vecToRot(worldLook);
    }
    
    /**
     * Transform world rotation to player-relative rotation.
     * This is like Gravity API's rotWorldToPlayer.
     */
    public static Vec2 rotWorldToPlayer(float yaw, float pitch, Surface surface) {
        Vec3 lookDir = rotToVec(yaw, pitch);
        Vec3 playerLook = vecWorldToPlayer(lookDir, surface);
        return vecToRot(playerLook);
    }
    
    /**
     * Convert yaw and pitch to a look direction vector.
     */
    public static Vec3 rotToVec(float yaw, float pitch) {
        double radPitch = pitch * Math.PI / 180.0;
        double radYaw = -yaw * Math.PI / 180.0;
        double cosPitch = Math.cos(radPitch);
        double sinPitch = Math.sin(radPitch);
        double cosYaw = Math.cos(radYaw);
        double sinYaw = Math.sin(radYaw);
        return new Vec3(sinYaw * cosPitch, -sinPitch, cosYaw * cosPitch);
    }
    
    /**
     * Convert a direction vector to yaw and pitch.
     */
    public static Vec2 vecToRot(Vec3 vec) {
        double xz = Math.sqrt(vec.x * vec.x + vec.z * vec.z);
        float pitch = (float) -Math.toDegrees(Math.atan2(vec.y, xz));
        float yaw = (float) -Math.toDegrees(Math.atan2(vec.x, vec.z));
        return new Vec2(yaw, pitch);
    }
    
    // ========== OVERLOADED VECTOR TRANSFORMS (from Gravity API) ==========
    
    /**
     * Transform from world space to player space (overload with components).
     */
    public static Vec3 vecWorldToPlayer(double x, double y, double z, Surface surface) {
        return vecWorldToPlayer(new Vec3(x, y, z), surface);
    }
    
    /**
     * Transform from player space to world space (overload with components).
     */
    public static Vec3 vecPlayerToWorld(double x, double y, double z, Surface surface) {
        return vecPlayerToWorld(new Vec3(x, y, z), surface);
    }
    
    /**
     * Get the Direction mapped from player space to world space.
     * For example, if surface is NORTH_WALL, player's UP becomes world's +Z.
     */
    public static Direction dirPlayerToWorld(Direction dir, Surface surface) {
        if (surface == Surface.GROUND) return dir;
        
        Vec3 dirVec = Vec3.atLowerCornerOf(dir.getNormal());
        Vec3 worldVec = vecPlayerToWorld(dirVec, surface);
        
        // Find closest direction
        return Direction.getNearest(
            (float) worldVec.x, 
            (float) worldVec.y, 
            (float) worldVec.z
        );
    }
    
    /**
     * Get the Direction mapped from world space to player space.
     */
    public static Direction dirWorldToPlayer(Direction dir, Surface surface) {
        if (surface == Surface.GROUND) return dir;
        
        Vec3 dirVec = Vec3.atLowerCornerOf(dir.getNormal());
        Vec3 playerVec = vecWorldToPlayer(dirVec, surface);
        
        return Direction.getNearest(
            (float) playerVec.x, 
            (float) playerVec.y, 
            (float) playerVec.z
        );
    }
    
    /**
     * Create a mask vector transformed from player space to world space.
     * This is used for selectively applying components based on gravity.
     * For example, maskPlayerToWorld(0, 1, 0, NORTH_WALL) gives (0, 0, 1)
     * meaning "only the Z component matters for vertical".
     */
    public static Vec3 maskPlayerToWorld(double x, double y, double z, Surface surface) {
        Vec3 mask = vecPlayerToWorld(x, y, z, surface);
        return new Vec3(Math.abs(mask.x), Math.abs(mask.y), Math.abs(mask.z));
    }
    
    /**
     * Create a mask vector (overload for Vec3).
     */
    public static Vec3 maskPlayerToWorld(Vec3 vec, Surface surface) {
        return maskPlayerToWorld(vec.x, vec.y, vec.z, surface);
    }
    
    /**
     * Get the player's relative Y position (height along gravity axis).
     * For GROUND, this is world Y. For NORTH_WALL, this is world Z, etc.
     */
    public static double getPlayerY(Vec3 worldPos, Surface surface) {
        Vec3 playerPos = vecWorldToPlayer(worldPos, surface);
        return playerPos.y;
    }
    
    /**
     * Get the player's relative position in player space.
     */
    public static Vec3 getPlayerPos(double worldX, double worldY, double worldZ, Surface surface) {
        return vecWorldToPlayer(worldX, worldY, worldZ, surface);
    }
    
    /**
     * Hamilton product of two quaternions.
     * This is the proper way to combine quaternion rotations (from Gravity API's CompatMath).
     * 
     * @param a First quaternion
     * @param b Second quaternion
     * @return The product quaternion (a * b in Hamilton product)
     */
    public static Quaternionf hamiltonProduct(Quaternionf a, Quaternionf b) {
        float f = a.x();
        float g = a.y();
        float h = a.z();
        float i = a.w();
        float j = b.x();
        float k = b.y();
        float l = b.z();
        float m = b.w();
        float x = i * j + f * m + g * l - h * k;
        float y = i * k - f * l + g * m + h * j;
        float z = i * l + f * k - g * j + h * m;
        float w = i * m - f * j - g * k - h * l;
        return new Quaternionf(x, y, z, w);
    }
    
    /**
     * Get the surface normal (direction INTO the wall/floor) in world coordinates.
     * This is the direction the player's feet point when standing on that surface.
     */
    public static Vec3 getSurfaceNormal(Surface surface) {
        return switch (surface) {
            case GROUND -> new Vec3(0, -1, 0);      // Feet point down
            case NORTH_WALL -> new Vec3(0, 0, -1);   // Feet point toward -Z (into north wall)
            case SOUTH_WALL -> new Vec3(0, 0, 1);    // Feet point toward +Z (into south wall)
            case EAST_WALL -> new Vec3(1, 0, 0);     // Feet point toward +X (into east wall)
            case WEST_WALL -> new Vec3(-1, 0, 0);    // Feet point toward -X (into west wall)
            case CEILING -> new Vec3(0, 1, 0);       // Feet point up
        };
    }
    
    /**
     * Get the "up" direction for this surface (opposite of where feet point).
     * This is the direction perpendicular to the walking surface, pointing away from it.
     */
    public static Vec3 getUpDirection(Surface surface) {
        return getSurfaceNormal(surface).scale(-1);
    }
    
    /**
     * Get the direction of gravity pull for this surface (toward the wall/floor).
     */
    public static Vec3 getGravityDirection(Surface surface) {
        return getSurfaceNormal(surface);
    }
    
    // ========== AABB (Bounding Box) TRANSFORMS (from Gravity API) ==========
    
    /**
     * Transform an AABB from world space to player space.
     * Used for collision checks in rotated gravity.
     */
    public static AABB boxWorldToPlayer(AABB box, Surface surface) {
        if (surface == Surface.GROUND) return box;
        
        Vec3 min = vecWorldToPlayer(box.minX, box.minY, box.minZ, surface);
        Vec3 max = vecWorldToPlayer(box.maxX, box.maxY, box.maxZ, surface);
        
        return new AABB(
            Math.min(min.x, max.x), Math.min(min.y, max.y), Math.min(min.z, max.z),
            Math.max(min.x, max.x), Math.max(min.y, max.y), Math.max(min.z, max.z)
        );
    }
    
    /**
     * Transform an AABB from player space to world space.
     * Used for positioning bounding boxes for collision.
     */
    public static AABB boxPlayerToWorld(AABB box, Surface surface) {
        if (surface == Surface.GROUND) return box;
        
        Vec3 min = vecPlayerToWorld(box.minX, box.minY, box.minZ, surface);
        Vec3 max = vecPlayerToWorld(box.maxX, box.maxY, box.maxZ, surface);
        
        return new AABB(
            Math.min(min.x, max.x), Math.min(min.y, max.y), Math.min(min.z, max.z),
            Math.max(min.x, max.x), Math.max(min.y, max.y), Math.max(min.z, max.z)
        );
    }
    
    /**
     * Get eye offset in world coordinates for the given surface.
     * On ground: (0, 1.62, 0). On walls: offset away from wall surface.
     */
    public static Vec3 getEyeOffset(Surface surface, float eyeHeight) {
        return vecPlayerToWorld(new Vec3(0, eyeHeight, 0), surface);
    }
    
    /**
     * Calculate movement in player space from input, then convert to world space.
     * This properly handles WASD relative to the camera when on any surface.
     * 
     * @param forwardInput W/S input (-1 to 1)
     * @param strafeInput A/D input (-1 to 1) 
     * @param yawDegrees Player's yaw
     * @param speed Movement speed multiplier
     * @param surface Current surface
     * @return Movement vector in world space
     */
    public static Vec3 calculateMovementToWorld(float forwardInput, float strafeInput, 
            float yawDegrees, float speed, Surface surface) {
        
        if (forwardInput == 0 && strafeInput == 0) return Vec3.ZERO;
        
        // Calculate movement in player space (standard ground movement formula)
        float yaw = (float) Math.toRadians(yawDegrees);
        float sinYaw = (float) Math.sin(yaw);
        float cosYaw = (float) Math.cos(yaw);
        
        // In player space, forward is -Z, right is +X
        double moveX = strafeInput * cosYaw - forwardInput * sinYaw;
        double moveZ = forwardInput * cosYaw + strafeInput * sinYaw;
        
        // Normalize and apply speed
        Vec3 playerMove = new Vec3(moveX, 0, moveZ);
        double len = playerMove.length();
        if (len > 1.0) {
            playerMove = playerMove.scale(1.0 / len);
        }
        playerMove = playerMove.scale(speed);
        
        // Transform to world space
        return vecPlayerToWorld(playerMove, surface);
    }
    
    /**
     * Calculate camera-relative movement for wall running.
     * When looking up on a wall, W moves you upward. When looking down, W moves you downward.
     * This gives true 3D movement on surfaces like in Gravity Changer.
     * 
     * @param forwardInput W/S input (-1 to 1)
     * @param strafeInput A/D input (-1 to 1)
     * @param yaw Player's yaw in degrees
     * @param pitch Player's pitch in degrees  
     * @param surface Current surface
     * @param speed Movement speed multiplier
     * @return Movement vector in world space
     */
    public static Vec3 calculateWallCameraRelativeMovement(float forwardInput, float strafeInput,
            float yaw, float pitch, Surface surface, float speed) {
        
        if (forwardInput == 0 && strafeInput == 0) return Vec3.ZERO;
        
        // Get look direction in player space
        Vec3 lookDir = rotToVec(yaw, pitch);
        
        // Transform look direction to world space
        Vec3 worldLookDir = vecPlayerToWorld(lookDir, surface);
        
        // Get the surface up direction
        Vec3 surfaceUp = getUpDirection(surface);
        
        // Calculate forward direction (look projected onto surface plane)
        // Remove the component along the surface normal
        double upComponent = worldLookDir.dot(surfaceUp);
        Vec3 forwardDir = worldLookDir.subtract(surfaceUp.scale(upComponent));
        
        // Normalize - but if we're looking directly up/down, use default forward
        if (forwardDir.lengthSqr() < 0.001) {
            // Looking straight up or down relative to surface
            // Use a default forward direction based on yaw
            forwardDir = vecPlayerToWorld(rotToVec(yaw, 0), surface);
            upComponent = forwardDir.dot(surfaceUp);
            forwardDir = forwardDir.subtract(surfaceUp.scale(upComponent));
        }
        forwardDir = forwardDir.normalize();
        
        // Calculate right direction (perpendicular to forward and up)
        Vec3 rightDir = forwardDir.cross(surfaceUp).normalize();
        
        // Apply inputs
        Vec3 movement = forwardDir.scale(forwardInput).add(rightDir.scale(-strafeInput));
        
        // For wall running, also include vertical component based on pitch
        // When looking up and pressing W, move UP the wall
        if (surface != Surface.GROUND && surface != Surface.CEILING) {
            // Add vertical component from pitch when moving forward
            double verticalMult = -Math.sin(Math.toRadians(pitch)) * forwardInput;
            movement = movement.add(surfaceUp.scale(verticalMult * 0.5));
        }
        
        // Normalize and apply speed
        double len = movement.length();
        if (len > 0) {
            movement = movement.scale(speed / len);
        }
        
        return movement;
    }
}
