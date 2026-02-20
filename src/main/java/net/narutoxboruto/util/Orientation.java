package net.narutoxboruto.util;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;
import org.joml.Quaternionf;

/**
 * Represents the orientation of a wall climbing entity relative to the surface it's attached to.
 * Contains the normal vector and local coordinate system for transforming between global and local space.
 * 
 * This is an EXACT port of AWCAPI's Orientation and calculateOrientation() logic.
 */
public class Orientation {
    // The attachment normal (direction feet point away from surface)
    public final Vec3 normal;
    
    // Local coordinate system basis vectors
    public final Vec3 localZ;  // Forward (faces away from where player is looking)
    public final Vec3 localY;  // Up (relative to orientation - same as normal when on ground)
    public final Vec3 localX;  // Right
    
    // Components of normal projected onto standard basis
    public final float componentZ;
    public final float componentY;
    public final float componentX;
    
    // Euler angles
    public final float yaw;
    public final float pitch;
    
    public Orientation(Vec3 normal, Vec3 localZ, Vec3 localY, Vec3 localX, 
                       float componentZ, float componentY, float componentX, 
                       float yaw, float pitch) {
        this.normal = normal;
        this.localZ = localZ;
        this.localY = localY;
        this.localX = localX;
        this.componentZ = componentZ;
        this.componentY = componentY;
        this.componentX = componentX;
        this.yaw = yaw;
        this.pitch = pitch;
    }
    
    /**
     * Calculate orientation from an attachment normal vector.
     * This is an EXACT port of AWCAPI's ClimberComponent.calculateOrientation().
     */
    public static Orientation fromAttachmentNormal(Vec3 attachmentNormal) {
        Vec3 localZ = new Vec3(0, 0, 1);
        Vec3 localY = new Vec3(0, 1, 0);
        Vec3 localX = new Vec3(1, 0, 0);
        
        float componentZ = (float) localZ.dot(attachmentNormal);
        float componentY;
        float componentX = (float) localX.dot(attachmentNormal);
        
        float yaw = (float) Math.toDegrees(Mth.atan2(componentX, componentZ));
        
        localZ = new Vec3(Math.sin(Math.toRadians(yaw)), 0, Math.cos(Math.toRadians(yaw)));
        localY = new Vec3(0, 1, 0);
        localX = new Vec3(Math.sin(Math.toRadians(yaw - 90)), 0, Math.cos(Math.toRadians(yaw - 90)));
        
        componentZ = (float) localZ.dot(attachmentNormal);
        componentY = (float) localY.dot(attachmentNormal);
        componentX = (float) localX.dot(attachmentNormal);
        
        float pitch = (float) Math.toDegrees(Mth.atan2(
            Mth.sqrt(componentX * componentX + componentZ * componentZ), 
            componentY
        ));
        
        // Build rotation matrix using Matrix4f (EXACT copy of AWCAPI)
        Matrix4f m = new Matrix4f();
        m.multiply(new Matrix4f((float) Math.toRadians(yaw), 0, 1, 0));
        m.multiply(new Matrix4f((float) Math.toRadians(pitch), 1, 0, 0));
        m.multiply(new Matrix4f((float) Math.toRadians(Math.signum(0.5f - componentY - componentZ - componentX) * yaw), 0, 1, 0));
        
        // Transform basis vectors (note AWCAPI uses (0,0,-1) for localZ)
        localZ = m.multiply(new Vec3(0, 0, -1));
        localY = m.multiply(new Vec3(0, 1, 0));
        localX = m.multiply(new Vec3(1, 0, 0));
        
        return new Orientation(attachmentNormal, localZ, localY, localX, 
                               componentZ, componentY, componentX, yaw, pitch);
    }
    
    /**
     * Create orientation from a wall direction.
     * 
     * The wallDirection is the direction FROM the player TO the wall.
     * The attachmentNormal should point AWAY from the wall (where the player's "back" is).
     */
    public static Orientation fromWallDirection(Direction wallDirection) {
        // attachmentNormal points OPPOSITE to the wall direction
        // If wall is NORTH, normal points SOUTH (away from wall)
        Vec3 attachmentNormal = switch (wallDirection) {
            case NORTH -> new Vec3(0, 0, 1);   // Wall to north, normal points south
            case SOUTH -> new Vec3(0, 0, -1);  // Wall to south, normal points north
            case EAST -> new Vec3(-1, 0, 0);   // Wall to east, normal points west
            case WEST -> new Vec3(1, 0, 0);    // Wall to west, normal points east
            case UP -> new Vec3(0, -1, 0);     // Ceiling above, normal points down
            case DOWN -> new Vec3(0, 1, 0);    // Ground below, normal points up
        };
        return fromAttachmentNormal(attachmentNormal);
    }
    
    /**
     * Ground orientation (standard Minecraft orientation).
     */
    public static Orientation ground() {
        return fromAttachmentNormal(new Vec3(0, 1, 0));
    }
    
    /**
     * Ceiling orientation.
     */
    public static Orientation ceiling() {
        return fromAttachmentNormal(new Vec3(0, -1, 0));
    }
    
    /**
     * Transforms yaw and pitch angles to a global direction vector.
     * This is the core method for movement - matches AWCAPI exactly.
     * 
     * @param yaw The yaw angle in degrees
     * @param pitch The pitch angle in degrees
     * @return The global direction vector
     */
    public Vec3 getGlobal(float yaw, float pitch) {
        float cy = Mth.cos(yaw * 0.017453292F);
        float sy = Mth.sin(yaw * 0.017453292F);
        float cp = -Mth.cos(-pitch * 0.017453292F);
        float sp = Mth.sin(-pitch * 0.017453292F);
        return this.localX.scale(sy * cp).add(this.localY.scale(sp)).add(this.localZ.scale(cy * cp));
    }
    
    /**
     * Transforms a local coordinate to global space.
     */
    public Vec3 getGlobal(Vec3 local) {
        return this.localX.scale(local.x).add(this.localY.scale(local.y)).add(this.localZ.scale(local.z));
    }
    
    /**
     * Transforms a global coordinate to local space.
     */
    public Vec3 getLocal(Vec3 global) {
        return new Vec3(this.localX.dot(global), this.localY.dot(global), this.localZ.dot(global));
    }
    
    /**
     * Calculates the local yaw and pitch from a global direction vector.
     */
    public Pair<Float, Float> getLocalRotation(Vec3 global) {
        Vec3 local = this.getLocal(global);
        
        float localYaw = (float) Math.toDegrees(Mth.atan2(local.x, local.z)) + 180.0f;
        float localPitch = (float) -Math.toDegrees(Mth.atan2(local.y, Math.sqrt(local.x * local.x + local.z * local.z)));
        
        return Pair.of(localYaw, localPitch);
    }
    
    /**
     * Get the quaternion rotation for this orientation (for rendering).
     * Matches AWCAPI's preRenderClimber transformations.
     */
    public Quaternionf getQuaternion() {
        Quaternionf q = new Quaternionf();
        q.identity();
        q.rotateY((float) Math.toRadians(yaw));
        q.rotateX((float) Math.toRadians(pitch));
        q.rotateY((float) Math.toRadians(Math.signum(0.5f - componentY - componentZ - componentX) * yaw));
        return q;
    }
    
    /**
     * Interpolate between two orientations (for smooth transitions).
     */
    public Orientation lerp(Orientation target, float t) {
        Vec3 lerpedNormal = this.normal.lerp(target.normal, t).normalize();
        return fromAttachmentNormal(lerpedNormal);
    }
    
    // Helper methods
    public Vec3 getUp() { return localY; }
    public Vec3 getForward() { return localZ; }
    public Vec3 getRight() { return localX; }
}
