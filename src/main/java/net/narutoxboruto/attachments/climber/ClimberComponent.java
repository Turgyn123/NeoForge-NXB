package net.narutoxboruto.attachments.climber;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.narutoxboruto.util.Orientation;

/**
 * Unified climbing component that holds all wall-running state.
 * Manages smooth orientation transitions, attachment offsets, and climbing flags.
 */
public class ClimberComponent {
    private Orientation currentOrientation = Orientation.ground();
    private Orientation targetOrientation = Orientation.ground();
    private float orientationLerpProgress = 1.0f;  // 1 = fully at target
    
    private Vec3 currentAttachmentOffset = Vec3.ZERO;
    private Vec3 targetAttachmentOffset = Vec3.ZERO;
    private float offsetLerpProgress = 1.0f;  // 1 = fully at target
    
    private boolean isClimbing = false;
    private Direction currentWallDirection = null;
    
    public ClimberComponent() {
    }
    
    /**
     * Called each tick to advance interpolation.
     */
    public void tick(float interpolationSpeed) {
        // Advance orientation interpolation
        if (orientationLerpProgress < 1.0f) {
            orientationLerpProgress = Math.min(1.0f, orientationLerpProgress + interpolationSpeed);
            if (orientationLerpProgress >= 1.0f) {
                currentOrientation = targetOrientation;
                orientationLerpProgress = 1.0f;
            }
        }
        
        // Advance offset interpolation
        if (offsetLerpProgress < 1.0f) {
            offsetLerpProgress = Math.min(1.0f, offsetLerpProgress + interpolationSpeed);
            if (offsetLerpProgress >= 1.0f) {
                currentAttachmentOffset = targetAttachmentOffset;
                offsetLerpProgress = 1.0f;
            }
        }
    }
    
    /**
     * Transition to a new wall surface with smooth interpolation.
     */
    public void transitionToWall(Direction wallDirection, Vec3 attachmentOffset) {
        if (this.currentWallDirection == wallDirection && isClimbing) {
            // Already on this wall, just update offset
            this.targetAttachmentOffset = attachmentOffset;
            this.offsetLerpProgress = 0.0f;  // Start interpolation from beginning
            return;
        }
        
        // Transition to new wall
        this.targetOrientation = Orientation.fromWallDirection(wallDirection);
        this.orientationLerpProgress = 0.0f;  // Start interpolation from beginning
        
        this.targetAttachmentOffset = attachmentOffset;
        this.offsetLerpProgress = 0.0f;
        
        this.currentWallDirection = wallDirection;
        this.isClimbing = true;
        
        // Immediately snap currentOrientation for movement purposes
        // This ensures movement works correctly even during visual transition
        this.currentOrientation = this.targetOrientation;
    }
    
    /**
     * Transition to ceiling.
     */
    public void transitionToCeiling(Vec3 attachmentOffset) {
        this.targetOrientation = Orientation.ceiling();
        this.orientationLerpProgress = 0.0f;  // Start interpolation from beginning
        
        this.targetAttachmentOffset = attachmentOffset;
        this.offsetLerpProgress = 0.0f;
        
        this.currentWallDirection = Direction.UP;
        this.isClimbing = true;
        
        // Immediately snap currentOrientation for movement purposes
        this.currentOrientation = this.targetOrientation;
    }
    
    /**
     * Transition back to ground (standing normally).
     */
    public void transitionToGround() {
        this.targetOrientation = Orientation.ground();
        this.orientationLerpProgress = 0.0f;  // Start interpolation from beginning
        
        this.targetAttachmentOffset = Vec3.ZERO;
        this.offsetLerpProgress = 0.0f;
        
        this.currentWallDirection = null;
        this.isClimbing = false;
        
        // Immediately snap for movement
        this.currentOrientation = this.targetOrientation;
    }
    
    /**
     * Get the current rendering orientation (interpolated).
     */
    public Orientation getRenderOrientation(float partialTicks) {
        float blend = orientationLerpProgress;
        // Add sub-tick interpolation
        if (orientationLerpProgress < 1.0f) {
            blend = Math.min(1.0f, orientationLerpProgress + partialTicks * 0.05f);
        }
        return currentOrientation.lerp(targetOrientation, blend);
    }
    
    /**
     * Get the movement orientation (for input transformation).
     */
    public Orientation getMovementOrientation() {
        return currentOrientation;
    }
    
    /**
     * Get attachment offset with interpolation.
     */
    public Vec3 getAttachmentOffset(float partialTicks) {
        if (offsetLerpProgress >= 1.0f) {
            return currentAttachmentOffset;
        }
        float blend = Math.min(1.0f, offsetLerpProgress + partialTicks * 0.1f);
        return currentAttachmentOffset.lerp(targetAttachmentOffset, blend);
    }
    
    public boolean isClimbing() {
        return isClimbing;
    }
    
    public Direction getCurrentWallDirection() {
        return currentWallDirection;
    }
    
    public Orientation getCurrentOrientation() {
        return currentOrientation;
    }
    
    public Orientation getTargetOrientation() {
        return targetOrientation;
    }
    
    /**
     * Serialize to NBT for saving.
     */
    public void save(CompoundTag tag) {
        tag.putBoolean("isClimbing", isClimbing);
        if (currentWallDirection != null) {
            tag.putString("wallDir", currentWallDirection.getName());
        }
    }
    
    /**
     * Deserialize from NBT.
     */
    public void load(CompoundTag tag) {
        this.isClimbing = tag.getBoolean("isClimbing");
        if (tag.contains("wallDir")) {
            this.currentWallDirection = Direction.byName(tag.getString("wallDir"));
            if (this.currentWallDirection == Direction.UP) {
                this.currentOrientation = Orientation.ceiling();
            } else {
                this.currentOrientation = Orientation.fromWallDirection(this.currentWallDirection);
            }
        }
    }
}
