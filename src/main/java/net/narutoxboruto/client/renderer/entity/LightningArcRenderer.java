package net.narutoxboruto.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.narutoxboruto.entities.effects.LightningArcEntity;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Renderer for LightningArcEntity - draws branching lightning bolts.
 * 
 * Uses line rendering with random offsets to create the jagged lightning effect.
 */
public class LightningArcRenderer extends EntityRenderer<LightningArcEntity> {
    
    // Lightning render type doesn't use textures, but we need something for getTextureLocation
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    private static final Random RANDOM = new Random();
    
    public LightningArcRenderer(EntityRendererProvider.Context context) {
        super(context);
    }
    
    @Override
    public void render(LightningArcEntity entity, float entityYaw, float partialTick, 
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        
        Vec3 startPos = new Vec3(0, 0, 0); // Relative to entity position
        Vec3 endPos = entity.getEndVec().subtract(entity.position());
        
        int color = entity.getColor();
        float alpha = ((color >> 24) & 0xFF) / 255.0f;
        float red = ((color >> 16) & 0xFF) / 255.0f;
        float green = ((color >> 8) & 0xFF) / 255.0f;
        float blue = (color & 0xFF) / 255.0f;
        
        float thickness = entity.getThickness();
        int maxSegments = entity.getMaxSegments();
        
        // Use entity ID as random seed for consistent jitter per-entity
        RANDOM.setSeed(entity.getId() * 31L + entity.tickCount);
        
        poseStack.pushPose();
        
        // Get the render type for lightning (additive blending)
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.lightning());
        
        // Generate and render the main bolt
        List<Vec3> mainBoltPoints = generateBoltPoints(startPos, endPos, maxSegments, 0.15);
        renderBolt(poseStack, buffer, mainBoltPoints, red, green, blue, alpha, thickness);
        
        // Generate branch bolts
        int branchCount = RANDOM.nextInt(3);
        for (int i = 0; i < branchCount; i++) {
            if (mainBoltPoints.size() > 2) {
                // Pick a random point along the main bolt to branch from
                int branchIndex = 1 + RANDOM.nextInt(mainBoltPoints.size() - 2);
                Vec3 branchStart = mainBoltPoints.get(branchIndex);
                
                // Random direction for the branch
                Vec3 direction = endPos.subtract(startPos).normalize();
                Vec3 perpendicular = new Vec3(
                    direction.z * 0.5 + (RANDOM.nextDouble() - 0.5),
                    (RANDOM.nextDouble() - 0.3) * 0.5,
                    -direction.x * 0.5 + (RANDOM.nextDouble() - 0.5)
                );
                
                double branchLength = startPos.distanceTo(endPos) * (0.2 + RANDOM.nextDouble() * 0.3);
                Vec3 branchEnd = branchStart.add(perpendicular.normalize().scale(branchLength));
                
                List<Vec3> branchPoints = generateBoltPoints(branchStart, branchEnd, maxSegments / 2 + 1, 0.1);
                renderBolt(poseStack, buffer, branchPoints, red, green, blue, alpha * 0.7f, thickness * 0.7f);
            }
        }
        
        // Render glow core (brighter, thinner line down the center)
        renderBolt(poseStack, buffer, mainBoltPoints, 1.0f, 1.0f, 1.0f, alpha * 0.8f, thickness * 0.3f);
        
        poseStack.popPose();
    }
    
    /**
     * Generate points for a lightning bolt with random offsets.
     */
    private List<Vec3> generateBoltPoints(Vec3 start, Vec3 end, int segments, double jitterScale) {
        List<Vec3> points = new ArrayList<>();
        points.add(start);
        
        Vec3 direction = end.subtract(start);
        double length = direction.length();
        
        for (int i = 1; i < segments; i++) {
            double t = (double) i / segments;
            Vec3 basePoint = start.add(direction.scale(t));
            
            // Add random jitter perpendicular to the direction
            double jitter = jitterScale * length * (1.0 - Math.abs(t - 0.5) * 2.0); // More jitter in middle
            Vec3 offset = new Vec3(
                (RANDOM.nextDouble() - 0.5) * jitter,
                (RANDOM.nextDouble() - 0.5) * jitter,
                (RANDOM.nextDouble() - 0.5) * jitter
            );
            
            points.add(basePoint.add(offset));
        }
        
        points.add(end);
        return points;
    }
    
    /**
     * Render a lightning bolt as a series of connected quads.
     */
    private void renderBolt(PoseStack poseStack, VertexConsumer buffer, List<Vec3> points,
                           float red, float green, float blue, float alpha, float thickness) {
        if (points.size() < 2) return;
        
        Matrix4f matrix = poseStack.last().pose();
        
        for (int i = 0; i < points.size() - 1; i++) {
            Vec3 p1 = points.get(i);
            Vec3 p2 = points.get(i + 1);
            
            // Calculate perpendicular vector for thickness
            Vec3 direction = p2.subtract(p1).normalize();
            Vec3 perpX = new Vec3(-direction.z, 0, direction.x).normalize().scale(thickness);
            Vec3 perpY = new Vec3(0, 1, 0).scale(thickness);
            
            // Render as a quad facing the camera (billboard style)
            // We'll render two crossed quads for visibility from all angles
            
            // Quad 1 (horizontal)
            renderQuad(matrix, poseStack, buffer, p1, p2, perpX, red, green, blue, alpha);
            
            // Quad 2 (vertical)
            renderQuad(matrix, poseStack, buffer, p1, p2, perpY, red, green, blue, alpha);
        }
    }
    
    private void renderQuad(Matrix4f matrix, PoseStack poseStack, VertexConsumer buffer,
                           Vec3 p1, Vec3 p2, Vec3 perp,
                           float red, float green, float blue, float alpha) {
        // Four corners of the quad
        Vec3 v1 = p1.add(perp);
        Vec3 v2 = p1.subtract(perp);
        Vec3 v3 = p2.subtract(perp);
        Vec3 v4 = p2.add(perp);
        
        // For RenderType.lightning(), we use a simplified vertex format
        // It expects: position, color (RGBA) only - no UV, overlay, light, or normals
        
        // Render the quad (four vertices for the quad strip)
        buffer.addVertex(matrix, (float)v1.x, (float)v1.y, (float)v1.z)
            .setColor(red, green, blue, alpha);
            
        buffer.addVertex(matrix, (float)v2.x, (float)v2.y, (float)v2.z)
            .setColor(red, green, blue, alpha);
            
        buffer.addVertex(matrix, (float)v3.x, (float)v3.y, (float)v3.z)
            .setColor(red, green, blue, alpha);
            
        buffer.addVertex(matrix, (float)v4.x, (float)v4.y, (float)v4.z)
            .setColor(red, green, blue, alpha);
    }
    
    @Override
    public ResourceLocation getTextureLocation(LightningArcEntity entity) {
        return TEXTURE;
    }
}
