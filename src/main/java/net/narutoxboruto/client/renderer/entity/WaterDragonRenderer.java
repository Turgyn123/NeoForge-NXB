package net.narutoxboruto.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.narutoxboruto.client.model.WaterDragonModel;
import net.narutoxboruto.entities.jutsus.WaterDragonEntity;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * GeckoLib renderer for Water Dragon entity.
 * - Uses GeckoLib's built-in withScale for proper model scaling.
 * - Only applies yaw rotation. The Attack animation handles the visual
 *   transition from vertical to horizontal — applying global pitch here
 *   would distort the multi-bone serpentine body.
 * - Uses entityTranslucent render type so flat-plane cubes (fins, ridges)
 *   are visible from both sides (no backface culling) and translucency
 *   is supported for the water effect.
 */
public class WaterDragonRenderer extends GeoEntityRenderer<WaterDragonEntity> {
    
    private static final float DRAGON_SCALE = 3.0f;
    
    public WaterDragonRenderer(EntityRendererProvider.Context context) {
        super(context, new WaterDragonModel());
        this.withScale(DRAGON_SCALE);
    }
    
    /**
     * Use entityTranslucent render type to disable backface culling
     * so zero-width flat-plane cubes (fins, ridges, side panels) render
     * from both sides, and to support water translucency.
     */
    @Nullable
    @Override
    public RenderType getRenderType(WaterDragonEntity animatable, ResourceLocation texture,
                                    @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }
    
    /**
     * Override GeckoLib's rotation handling.
     * GeckoLib passes rotationYaw=0 for non-LivingEntity (Projectile),
     * so we apply the entity's actual yRot and xRot.
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
