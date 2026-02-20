package net.narutoxboruto.mixin;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.narutoxboruto.client.PlayerData;
import net.narutoxboruto.util.RotationUtil;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Camera mixin for wall running view rotation.
 *
 * Applies a pre-computed quaternion from {@link RotationUtil} so the wall
 * surface appears as "ground" to the player.  Runs client-side only.
 */
@Mixin(Camera.class)
public abstract class MixinCamera {

    @Shadow
    private Quaternionf rotation;

    @Inject(method = "setup",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/client/Camera;setRotation(FF)V",
                     shift = At.Shift.AFTER))
    private void applyWallRunningRotation(net.minecraft.world.level.BlockGetter level,
                                          Entity renderViewEntity, boolean thirdPerson,
                                          boolean inverseView, float partialTick,
                                          CallbackInfo ci) {
        if (!(renderViewEntity instanceof Player)) return;

        RotationUtil.Surface surface = PlayerData.getWallRunningSurface();
        if (surface == RotationUtil.Surface.GROUND) return;

        Quaternionf surfaceRotation = RotationUtil.getCameraRotationQuaternion(surface);
        this.rotation.mul(surfaceRotation);
    }
}
