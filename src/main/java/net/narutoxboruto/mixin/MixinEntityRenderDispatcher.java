package net.narutoxboruto.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.narutoxboruto.client.PlayerData;
import net.narutoxboruto.util.Orientation;
import net.narutoxboruto.util.RotationUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Entity render dispatcher mixin for wall running model rotation - EXACT AWCAPI pattern.
 * 
 * AWCAPI's ClientClimberHelper.preRenderClimber() does:
 * 1. translate(attachmentOffset - normal * verticalOffset)
 * 2. mulPose(Axis.YP.rotationDegrees(yaw))
 * 3. mulPose(Axis.XP.rotationDegrees(pitch))
 * 4. mulPose(Axis.YP.rotationDegrees(signum(0.5 - componentY - componentZ - componentX) * yaw))
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class MixinEntityRenderDispatcher {
    
    // Render offset to push player model out of wall (player hitbox is inside wall)
    // AWCAPI uses attachmentOffset + verticalOffset (0.075f) - we use a fixed offset
    private static final float WALL_RENDER_OFFSET = 0.075f;  // Match AWCAPI's default verticalOffset
    
    /**
     * Apply wall running rotation to entity model rendering.
     * Uses EXACT AWCAPI rendering pattern from ClientClimberHelper.preRenderClimber()
     */
    @Inject(
        method = "render",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", shift = At.Shift.BEFORE)
    )
    private <E extends Entity> void applyWallRunningModelRotation(E entity, double x, double y, double z, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int light, CallbackInfo ci) {
        if (!(entity instanceof Player)) return;
        
        RotationUtil.Surface surface = PlayerData.getWallRunningSurface();
        if (surface == RotationUtil.Surface.GROUND) return;
        
        // Always push pose to maintain stack balance
        poseStack.pushPose();
        
        // Get the orientation for this surface
        Direction direction = surface.getDirection();
        Orientation orientation = Orientation.fromWallDirection(direction);
        
        // === STEP 1: RENDER OFFSET === (AWCAPI pattern)
        // Push the model AWAY from the wall so it's not clipping
        // The direction is the wall normal (e.g., SOUTH wall has normal pointing +Z)
        if (direction != null) {
            float offsetX = -direction.getStepX() * WALL_RENDER_OFFSET;
            float offsetY = 0;  // No vertical offset
            float offsetZ = -direction.getStepZ() * WALL_RENDER_OFFSET;
            poseStack.translate(offsetX, offsetY, offsetZ);
        }
        
        // === STEP 2-4: APPLY ROTATIONS === (EXACT AWCAPI pattern)
        // AWCAPI does: mulPose(YP.yaw), mulPose(XP.pitch), mulPose(YP.signum*yaw)
        poseStack.mulPose(Axis.YP.rotationDegrees(orientation.yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(orientation.pitch));
        poseStack.mulPose(Axis.YP.rotationDegrees(
            (float) Math.signum(0.5f - orientation.componentY - orientation.componentZ - orientation.componentX) 
            * orientation.yaw
        ));
    }
    
    @Inject(
        method = "render",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", shift = At.Shift.AFTER)
    )
    private <E extends Entity> void popWallRunningRotation(E entity, double x, double y, double z, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int light, CallbackInfo ci) {
        if (!(entity instanceof Player)) return;
        
        RotationUtil.Surface surface = PlayerData.getWallRunningSurface();
        if (surface == RotationUtil.Surface.GROUND) return;
        
        // Always pop to match the push
        poseStack.popPose();
    }
}
