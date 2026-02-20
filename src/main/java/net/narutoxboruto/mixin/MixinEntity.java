package net.narutoxboruto.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.narutoxboruto.attachments.MainAttachment;
import net.narutoxboruto.util.RotationUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Core entity mixin for wall-running coordinate transforms.
 *
 * Transforms:
 * - onGround → true when on any non-GROUND surface (prevents gravity/fall damage)
 * - getViewVector → player-space look direction → world space
 * - getEyePosition → offset from entity pos accounts for surface orientation
 */
@Mixin(Entity.class)
public abstract class MixinEntity {

    @Shadow public abstract Vec3 position();
    @Shadow public abstract float getEyeHeight();

    // ── Helper: resolve the current surface on either side ──────────

    private RotationUtil.Surface nxb$getSurface(Player player) {
        if (player.level().isClientSide) {
            return net.narutoxboruto.client.PlayerData.getWallRunningSurface();
        }
        return player.getData(MainAttachment.WALL_RUNNING).getSurface();
    }

    // ── onGround override ──────────────────────────────────────────

    /**
     * Return true when on any non-GROUND surface so the engine treats
     * the player as "standing" (no gravity, no fall damage, sprint stays).
     */
    @Inject(method = "onGround", at = @At("RETURN"), cancellable = true)
    private void nxb$onGround(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof Player player)) return;

        RotationUtil.Surface surface = nxb$getSurface(player);
        if (surface != RotationUtil.Surface.GROUND) {
            cir.setReturnValue(true);
        }
    }

    // ── View-vector transform ──────────────────────────────────────

    @Inject(method = "getViewVector", at = @At("RETURN"), cancellable = true)
    private void nxb$transformViewVector(float partialTicks, CallbackInfoReturnable<Vec3> cir) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof Player player)) return;

        RotationUtil.Surface surface = nxb$getSurface(player);
        if (surface == RotationUtil.Surface.GROUND) return;

        Vec3 playerSpace = cir.getReturnValue();
        cir.setReturnValue(RotationUtil.vecPlayerToWorld(playerSpace, surface));
    }

    // ── Eye-position transform ─────────────────────────────────────

    @Inject(method = "getEyePosition()Lnet/minecraft/world/phys/Vec3;", at = @At("RETURN"), cancellable = true)
    private void nxb$transformEyePosition(CallbackInfoReturnable<Vec3> cir) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof Player player)) return;

        RotationUtil.Surface surface = nxb$getSurface(player);
        if (surface == RotationUtil.Surface.GROUND) return;

        Vec3 pos = position();
        Vec3 worldOffset = RotationUtil.vecPlayerToWorld(new Vec3(0, getEyeHeight(), 0), surface);
        cir.setReturnValue(pos.add(worldOffset));
    }
}
