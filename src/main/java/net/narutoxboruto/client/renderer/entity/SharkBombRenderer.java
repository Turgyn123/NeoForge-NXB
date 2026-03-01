package net.narutoxboruto.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.narutoxboruto.client.model.SharkBombModel;
import net.narutoxboruto.entities.jutsus.SharkBombEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * GeckoLib renderer for SharkBomb entity.
 * - Overrides applyRotations to fix GeckoLib ignoring yRot for non-LivingEntity
 * - No scaling override (shark renders at natural model size)
 */
public class SharkBombRenderer extends GeoEntityRenderer<SharkBombEntity> {
    
    public SharkBombRenderer(EntityRendererProvider.Context context) {
        super(context, new SharkBombModel());
    }
    
    /**
     * Override GeckoLib's rotation handling.
     * GeckoLib passes rotationYaw=0 for non-LivingEntity (Projectile),
     * so we must apply the entity's actual yRot and xRot ourselves.
     * Entity yaw uses MC convention (negated atan2), so standard 180-yaw works.
     */
    @Override
    protected void applyRotations(SharkBombEntity entity, PoseStack poseStack, 
                                   float ageInTicks, float rotationYaw, 
                                   float partialTick, float nativeScale) {
        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        
        poseStack.mulPose(Axis.YP.rotationDegrees(180f - yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
    }
}
