package net.narutoxboruto.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.narutoxboruto.client.PlayerData;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Custom renderer for Kiba sword that adds lightning effects directly in model-space.
 * This ensures lightning follows the sword during swings and animations.
 */
public class KibaSwordRenderer extends BlockEntityWithoutLevelRenderer {
    
    public static final KibaSwordRenderer INSTANCE = new KibaSwordRenderer();
    
    private static final Random random = new Random();
    
    // Bright electric blue color (RGB format)
    private static final int LIGHTNING_COLOR = 0x40B0FF;
    
    // Arc storage
    private static final List<LightningArc> arcs = new ArrayList<>();
    private static final int MAX_ARCS = 18;
    private static long lastArcGenTime = 0;
    private static final long ARC_INTERVAL_MS = 45;
    
    private record LightningArc(Vector3f start, Vector3f end, long expireTime) {}
    
    public KibaSwordRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), 
              Minecraft.getInstance().getEntityModels());
    }
    
    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                             MultiBufferSource buffer, int packedLight, int packedOverlay) {
        
        Minecraft mc = Minecraft.getInstance();
        ItemRenderer itemRenderer = mc.getItemRenderer();
        
        // Get the baked model for the sword
        BakedModel model = itemRenderer.getModel(stack, mc.level, mc.player, 0);
        
        // Render the actual sword model first
        poseStack.pushPose();
        // BEWLR models need slight adjustment
        poseStack.translate(0.5f, 0.5f, 0.5f);
        itemRenderer.render(stack, displayContext, false, poseStack, buffer, packedLight, packedOverlay, model);
        poseStack.popPose();
        
        // Only render lightning if Kiba ability is active
        if (!PlayerData.isKibaActive()) {
            return;
        }
        
        // Render lightning effects in the same model-space
        poseStack.pushPose();
        
        // Adjust position to align with blade
        // The model is centered at 0.5, 0.5, 0.5 - blade extends in specific direction
        // For different display contexts, adjust accordingly
        switch (displayContext) {
            case FIRST_PERSON_RIGHT_HAND:
            case FIRST_PERSON_LEFT_HAND:
                // First person: blade extends upward (+Y)
                poseStack.translate(0.5f, 0.1f, 0.5f);
                break;
            case THIRD_PERSON_RIGHT_HAND:
            case THIRD_PERSON_LEFT_HAND:
                // Third person: blade extends diagonally
                poseStack.translate(0.5f, 0.1f, 0.5f);
                break;
            case GUI:
            case GROUND:
            case FIXED:
            case HEAD:
            default:
                // Other contexts: center on model
                poseStack.translate(0.5f, 0.5f, 0.5f);
                break;
        }
        
        // Scale lightning to match sword size
        float scale = 0.8f;
        poseStack.scale(scale, scale, scale);
        
        renderLightning(poseStack, buffer, displayContext);
        
        poseStack.popPose();
    }
    
    private void renderLightning(PoseStack poseStack, MultiBufferSource buffer, ItemDisplayContext context) {
        long currentTime = System.currentTimeMillis();
        
        // Generate new arcs periodically
        if (currentTime - lastArcGenTime > ARC_INTERVAL_MS) {
            lastArcGenTime = currentTime;
            generateArcs(currentTime, context);
        }
        
        // Remove expired arcs
        arcs.removeIf(arc -> currentTime > arc.expireTime);
        
        // Render all arcs
        VertexConsumer consumer = buffer.getBuffer(RenderType.lightning());
        Matrix4f matrix = poseStack.last().pose();
        
        for (LightningArc arc : arcs) {
            renderArc(matrix, consumer, arc, currentTime);
        }
    }
    
    private void generateArcs(long currentTime, ItemDisplayContext context) {
        // Remove old arcs if too many
        while (arcs.size() >= MAX_ARCS) {
            arcs.remove(0);
        }
        
        // Blade dimensions - sword blade in model space
        // Blade extends along Y axis (upward from grip)
        float bladeLength = 0.7f;
        float bladeRadius = 0.05f;
        
        // Longitudinal arcs along the blade
        for (int i = 0; i < 3; i++) {
            float startT = random.nextFloat() * 0.4f;
            float endT = startT + 0.25f + random.nextFloat() * 0.35f;
            float angle = random.nextFloat() * (float)(Math.PI * 2);
            float r = bladeRadius * (0.7f + random.nextFloat() * 0.5f);
            
            Vector3f start = new Vector3f(
                (float)Math.cos(angle) * r,
                startT * bladeLength,
                (float)Math.sin(angle) * r
            );
            Vector3f end = new Vector3f(
                (float)Math.cos(angle) * r,
                endT * bladeLength,
                (float)Math.sin(angle) * r
            );
            arcs.add(new LightningArc(start, end, currentTime + 80 + random.nextInt(80)));
        }
        
        // Spiral wrapping arcs
        for (int i = 0; i < 3; i++) {
            float y1 = random.nextFloat() * bladeLength * 0.7f;
            float y2 = y1 + 0.08f + random.nextFloat() * 0.12f;
            float startAngle = random.nextFloat() * (float)(Math.PI * 2);
            float arcLength = (float)(Math.PI * (0.6 + random.nextFloat() * 0.9));
            
            Vector3f start = new Vector3f(
                (float)Math.cos(startAngle) * bladeRadius,
                y1,
                (float)Math.sin(startAngle) * bladeRadius
            );
            Vector3f end = new Vector3f(
                (float)Math.cos(startAngle + arcLength) * bladeRadius,
                y2,
                (float)Math.sin(startAngle + arcLength) * bladeRadius
            );
            arcs.add(new LightningArc(start, end, currentTime + 60 + random.nextInt(70)));
        }
        
        // Cross arcs
        for (int i = 0; i < 2; i++) {
            float y = 0.1f + random.nextFloat() * (bladeLength - 0.15f);
            float angle = random.nextFloat() * (float)(Math.PI * 2);
            float crossAngle = angle + (float)Math.PI + ((random.nextFloat() - 0.5f) * 0.6f);
            
            Vector3f start = new Vector3f(
                (float)Math.cos(angle) * bladeRadius,
                y,
                (float)Math.sin(angle) * bladeRadius
            );
            Vector3f end = new Vector3f(
                (float)Math.cos(crossAngle) * bladeRadius,
                y + (random.nextFloat() - 0.5f) * 0.04f,
                (float)Math.sin(crossAngle) * bladeRadius
            );
            arcs.add(new LightningArc(start, end, currentTime + 50 + random.nextInt(50)));
        }
        
        // Tip corona
        if (random.nextFloat() < 0.7f) {
            float angle = random.nextFloat() * (float)(Math.PI * 2);
            float sparkLength = 0.03f + random.nextFloat() * 0.05f;
            Vector3f start = new Vector3f(0, bladeLength, 0);
            Vector3f end = new Vector3f(
                (float)Math.cos(angle) * sparkLength,
                bladeLength + sparkLength * 0.6f,
                (float)Math.sin(angle) * sparkLength
            );
            arcs.add(new LightningArc(start, end, currentTime + 40 + random.nextInt(40)));
        }
    }
    
    private void renderArc(Matrix4f matrix, VertexConsumer consumer, LightningArc arc, long currentTime) {
        float lifeRemaining = (arc.expireTime - currentTime) / 120f;
        float alpha = Math.min(1.0f, lifeRemaining * 2.0f);
        
        float r = ((LIGHTNING_COLOR >> 16) & 0xFF) / 255f;
        float g = ((LIGHTNING_COLOR >> 8) & 0xFF) / 255f;
        float b = (LIGHTNING_COLOR & 0xFF) / 255f;
        
        List<Vector3f> points = generateBoltPoints(arc.start, arc.end, 4);
        
        float thickness = 0.012f;
        
        // Render colored outer bolt
        for (int i = 0; i < points.size() - 1; i++) {
            Vector3f p1 = points.get(i);
            Vector3f p2 = points.get(i + 1);
            renderBoltSegment(matrix, consumer, p1, p2, thickness, r, g, b, alpha, 0);
            renderBoltSegment(matrix, consumer, p1, p2, thickness, r, g, b, alpha, (float)(Math.PI / 2));
        }
        
        // White core
        for (int i = 0; i < points.size() - 1; i++) {
            Vector3f p1 = points.get(i);
            Vector3f p2 = points.get(i + 1);
            renderBoltSegment(matrix, consumer, p1, p2, thickness * 0.4f, 1f, 1f, 1f, alpha * 0.8f, 0);
        }
    }
    
    private List<Vector3f> generateBoltPoints(Vector3f start, Vector3f end, int segments) {
        List<Vector3f> points = new ArrayList<>();
        points.add(new Vector3f(start));
        
        Vector3f dir = new Vector3f(end).sub(start);
        float length = dir.length();
        if (length < 0.001f) {
            points.add(new Vector3f(end));
            return points;
        }
        dir.normalize();
        
        float jitterAmount = length * 0.12f;
        
        for (int i = 1; i < segments; i++) {
            float t = (float)i / segments;
            Vector3f point = new Vector3f(start).add(new Vector3f(dir).mul(length * t));
            point.x += (random.nextFloat() - 0.5f) * jitterAmount;
            point.y += (random.nextFloat() - 0.5f) * jitterAmount;
            point.z += (random.nextFloat() - 0.5f) * jitterAmount;
            points.add(point);
        }
        
        points.add(new Vector3f(end));
        return points;
    }
    
    private void renderBoltSegment(Matrix4f matrix, VertexConsumer consumer,
                                   Vector3f p1, Vector3f p2, float thickness,
                                   float r, float g, float b, float alpha, float rotation) {
        Vector3f dir = new Vector3f(p2).sub(p1);
        if (dir.lengthSquared() < 0.0001f) return;
        dir.normalize();
        
        Vector3f up = new Vector3f(0, 1, 0);
        if (Math.abs(dir.dot(up)) > 0.99f) {
            up = new Vector3f(1, 0, 0);
        }
        
        Vector3f perpendicular = new Vector3f(dir).cross(up).normalize().mul(thickness);
        
        if (rotation != 0) {
            float cos = (float)Math.cos(rotation);
            float sin = (float)Math.sin(rotation);
            Vector3f perp2 = new Vector3f(up).cross(dir).normalize().mul(thickness);
            perpendicular = new Vector3f(
                perpendicular.x * cos + perp2.x * sin,
                perpendicular.y * cos + perp2.y * sin,
                perpendicular.z * cos + perp2.z * sin
            );
        }
        
        // Quad vertices
        consumer.addVertex(matrix, p1.x - perpendicular.x, p1.y - perpendicular.y, p1.z - perpendicular.z)
                .setColor(r, g, b, alpha);
        consumer.addVertex(matrix, p1.x + perpendicular.x, p1.y + perpendicular.y, p1.z + perpendicular.z)
                .setColor(r, g, b, alpha);
        consumer.addVertex(matrix, p2.x + perpendicular.x, p2.y + perpendicular.y, p2.z + perpendicular.z)
                .setColor(r, g, b, alpha);
        consumer.addVertex(matrix, p2.x - perpendicular.x, p2.y - perpendicular.y, p2.z - perpendicular.z)
                .setColor(r, g, b, alpha);
    }
}
