package net.narutoxboruto.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.narutoxboruto.client.model.WaterDragonModel;
import net.narutoxboruto.entities.jutsus.WaterDragonEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * GeckoLib renderer for Water Dragon entity.
 * - 5x scale for massive visual impact
 * - Overrides applyRotations to fix GeckoLib ignoring yRot/xRot for non-LivingEntity
 */
public class WaterDragonRenderer extends GeoEntityRenderer<WaterDragonEntity> {
    
    private static final float DRAGON_SCALE = 5.0f;
    
    public WaterDragonRenderer(EntityRendererProvider.Context context) {
        super(context, new WaterDragonModel());
    }
    
    @Override
    public void render(WaterDragonEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.scale(DRAGON_SCALE, DRAGON_SCALE, DRAGON_SCALE);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }
    
    /**
     * Override GeckoLib's rotation handling.
     * GeckoLib passes rotationYaw=0 for non-LivingEntity (Projectile),
     * so we must apply the entity's actual yRot and xRot ourselves.
     */
    @Override
    protected void applyRotations(WaterDragonEntity entity, PoseStack poseStack, 
                                   float ageInTicks, float rotationYaw, 
                                   float partialTick, float nativeScale) {
        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        
        poseStack.mulPose(Axis.YP.rotationDegrees(180f - yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
    }
}
