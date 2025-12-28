package net.narutoxboruto.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.narutoxboruto.client.model.FireBallModel;
import net.narutoxboruto.entities.jutsus.FireBallEntity;
import net.narutoxboruto.main.Main;

/**
 * Renderer for the FireBall entity with continuous rotation during flight.
 * Renders a 3D model that rotates as it flies.
 */
public class FireBallRenderer extends EntityRenderer<FireBallEntity> {
    
    private static final ResourceLocation FIREBALL_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        Main.MOD_ID, "textures/entity/great_fireball.png"
    );
    
    private final FireBallModel model;

    public FireBallRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new FireBallModel(context.bakeLayer(FireBallModel.LAYER_LOCATION));
    }

    @Override
    public void render(FireBallEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, 
                       MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        
        // Scale the fireball (4x original size)
        float scale = 2.0F;
        poseStack.scale(scale, scale, scale);
        
        // Apply rotation for spinning effect on all axes
        float rotation = entity.getRotation(partialTicks);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        poseStack.mulPose(Axis.XP.rotationDegrees(rotation * 0.7F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotation * 0.3F));
        
        // Get the render type for the texture (emissive for glowing effect)
        RenderType renderType = RenderType.entityTranslucentEmissive(FIREBALL_TEXTURE);
        VertexConsumer vertexConsumer = buffer.getBuffer(renderType);
        
        // Full brightness for glowing effect
        int brightness = 15728880;
        
        // Render the 3D model
        model.renderToBuffer(poseStack, vertexConsumer, brightness, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        
        poseStack.popPose();
        
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(FireBallEntity entity) {
        return FIREBALL_TEXTURE;
    }
}
