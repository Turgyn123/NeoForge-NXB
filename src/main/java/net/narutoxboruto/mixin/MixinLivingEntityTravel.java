package net.narutoxboruto.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.narutoxboruto.attachments.MainAttachment;
import net.narutoxboruto.attachments.climber.ClimberComponent;
import net.narutoxboruto.attachments.climber.ClimberMoveController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Travel override mixin for wall-running players using the ClimberComponent architecture.
 * Intercepts travel() to apply custom climbing movement when needed.
 */
@Mixin(LivingEntity.class)
public abstract class MixinLivingEntityTravel {
    
    /**
     * Intercept travel() for players using custom climbing.
     * If climbing, handle movement via ClimberMoveController and skip vanilla travel.
     */
    @Inject(
        method = "travel",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onTravel(Vec3 travelVector, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Player player)) {
            return;  // Not a player, use vanilla travel
        }
        
        ClimberComponent climber = player.getData(MainAttachment.CLIMBER);
        
        // Use custom climbing travel if climbing
        if (ClimberMoveController.handleTravel(player, travelVector, climber)) {
            ci.cancel();  // Skip vanilla travel
        }
    }
}
