package net.narutoxboruto.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.narutoxboruto.client.PlayerData;
import net.narutoxboruto.client.renderer.item.SwordLightningArc;
import net.narutoxboruto.items.swords.Kiba;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Mixin to add lightning spark overlay to Kiba sword when ability is active.
 * Renders lightning arcs along the sword model geometry.
 */
@Mixin(ItemRenderer.class)
public class MixinItemRenderer {
    
    @Unique
    private static final Random narutoxboruto$random = new Random();
    
    @Unique
    private static final int KIBA_LIGHTNING_COLOR = 0x40B0FF; // Bright electric blue
    
    @Unique
    private static final List<SwordLightningArc> narutoxboruto$swordArcs = new ArrayList<>();
    
    @Unique
    private static long narutoxboruto$lastArcGenTime = 0;
    
    @Unique
    private static boolean narutoxboruto$isFirstPerson = false;
    
    @Unique
    private static final long ARC_INTERVAL_MS = 120; // Reduced frequency (was 80)
    
    @Unique
    private static final long BURST_ARC_INTERVAL_MS = 15; // Fast interval during activation burst
    
    @Unique
    private static final int MAX_ARCS = 10; // Halved max (was 20)
    
    @Unique
    private static final int BURST_MAX_ARCS = 40; // More arcs during activation burst
    
    /**
     * After the normal item render, render a glowing overlay if it's Kiba with active ability
     */
    @Inject(method = "render", at = @At("TAIL"))
    private void narutoxboruto$renderKibaLightningOverlay(
            ItemStack itemStack, 
            ItemDisplayContext displayContext, 
            boolean leftHand, 
            PoseStack poseStack, 
            MultiBufferSource buffer, 
            int combinedLight, 
            int combinedOverlay, 
            BakedModel model,
            CallbackInfo ci) {
        
        // Only apply to Kiba sword when ability is active
        if (!(itemStack.getItem() instanceof Kiba)) return;
        if (!PlayerData.isKibaActive()) return;
        
        // Skip for GUI rendering - only show in hand
        if (displayContext == ItemDisplayContext.GUI || 
            displayContext == ItemDisplayContext.GROUND ||
            displayContext == ItemDisplayContext.FIXED) {
            return;
        }
        
        // Calculate pulsing effect
        float time = (System.currentTimeMillis() % 1500) / 1500.0f;
        float pulse = 0.5f + 0.5f * (float)Math.sin(time * Math.PI * 2);
        
        // Render the model again with a glowing blue tint
        // Use full brightness (240) and the lightning render type for glow effect
        poseStack.pushPose();
        
        // Apply different offsets for first person vs third person
        narutoxboruto$isFirstPerson = displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || 
                                displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
        
        if (narutoxboruto$isFirstPerson) {
            // First person - rotate back ~10 degrees to match the upright sword model
            poseStack.translate(-0.5f, -0.15f, -0.4f);
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(10.0f));
        } else {
            // Third person - current working values
            poseStack.translate(-0.5f, -0.15f, -0.4f);
        }
        
        // Get the glowing vertex consumer
        // Use lightning render type for bright electric bolts
        VertexConsumer consumer = buffer.getBuffer(RenderType.lightning());
        
        // Render lightning sparks along the sword model
        narutoxboruto$renderSwordLightning(model, itemStack, poseStack, consumer);
        
        poseStack.popPose();
    }
    
    /**
     * Renders lightning arcs along the sword model geometry
     */
    @Unique
    private void narutoxboruto$renderSwordLightning(BakedModel model, ItemStack stack, PoseStack poseStack, 
                                                     VertexConsumer consumer) {
        long currentTime = System.currentTimeMillis();
        Matrix4f matrix = poseStack.last().pose();
        
        // Check if we're in activation burst mode (more aggressive lightning)
        boolean isBurst = PlayerData.isKibaBurstActive();
        long interval = isBurst ? BURST_ARC_INTERVAL_MS : ARC_INTERVAL_MS;
        int maxArcs = isBurst ? BURST_MAX_ARCS : MAX_ARCS;
        float thickness = isBurst ? 0.02f : 0.012f; // Thicker bolts during burst
        
        // Generate new arcs periodically
        if (currentTime - narutoxboruto$lastArcGenTime > interval) {
            narutoxboruto$lastArcGenTime = currentTime;
            narutoxboruto$generateSwordArcs(model, currentTime, isBurst);
        }
        
        // Cap arcs based on mode
        while (narutoxboruto$swordArcs.size() > maxArcs) {
            narutoxboruto$swordArcs.remove(0);
        }
        
        // Remove expired arcs
        narutoxboruto$swordArcs.removeIf(arc -> currentTime > arc.expireTime);
        
        // Render all active arcs
        for (SwordLightningArc arc : narutoxboruto$swordArcs) {
            narutoxboruto$renderArc(matrix, consumer, arc, currentTime, KIBA_LIGHTNING_COLOR, thickness);
        }
    }
    
    /**
     * Generates lightning arcs along the sword geometry
     * @param isBurst If true, generates more aggressive arcs for activation burst
     */
    @Unique
    private void narutoxboruto$generateSwordArcs(BakedModel model, long currentTime, boolean isBurst) {
        int maxArcs = isBurst ? BURST_MAX_ARCS : MAX_ARCS;
        while (narutoxboruto$swordArcs.size() >= maxArcs) {
            narutoxboruto$swordArcs.remove(0);
        }
        
        // Collect vertex positions from the model to know where the sword is
        List<Vector3f> modelVertices = new ArrayList<>();
        var random = net.minecraft.util.RandomSource.create();
        
        // Get vertices from all model quads
        for (var direction : net.minecraft.core.Direction.values()) {
            var quads = model.getQuads(null, direction, random);
            for (var quad : quads) {
                narutoxboruto$extractVertices(quad, modelVertices);
            }
        }
        var generalQuads = model.getQuads(null, null, random);
        for (var quad : generalQuads) {
            narutoxboruto$extractVertices(quad, modelVertices);
        }
        
        // If we have vertices, generate arcs between random pairs (blade only, Y > 0.25)
        if (modelVertices.size() >= 2) {
            // During burst: 8 arcs, normal: 2 arcs
            int arcCount = isBurst ? 8 : 2;
            for (int i = 0; i < arcCount; i++) {
                int idx1 = narutoxboruto$random.nextInt(modelVertices.size());
                int idx2 = narutoxboruto$random.nextInt(modelVertices.size());
                if (idx1 != idx2) {
                    Vector3f start = new Vector3f(modelVertices.get(idx1));
                    Vector3f end = new Vector3f(modelVertices.get(idx2));
                    
                    // More jitter during burst for chaotic effect
                    float jitter = isBurst ? 0.05f : 0.02f;
                    start.add(
                        (narutoxboruto$random.nextFloat() - 0.5f) * jitter,
                        (narutoxboruto$random.nextFloat() - 0.5f) * jitter,
                        (narutoxboruto$random.nextFloat() - 0.5f) * jitter
                    );
                    end.add(
                        (narutoxboruto$random.nextFloat() - 0.5f) * jitter,
                        (narutoxboruto$random.nextFloat() - 0.5f) * jitter,
                        (narutoxboruto$random.nextFloat() - 0.5f) * jitter
                    );
                    
                    // Shorter duration during burst for rapid flashing
                    int duration = isBurst ? (30 + narutoxboruto$random.nextInt(40)) : (60 + narutoxboruto$random.nextInt(80));
                    narutoxboruto$swordArcs.add(new SwordLightningArc(start, end, currentTime + duration));
                }
            }
            
            // During burst: 4 sparks, normal: 1 spark
            int sparkCount = isBurst ? 4 : 1;
            for (int i = 0; i < sparkCount; i++) {
                int idx = narutoxboruto$random.nextInt(modelVertices.size());
                Vector3f base = new Vector3f(modelVertices.get(idx));
                
                // Longer sparks during burst
                float sparkLen = isBurst ? (0.06f + narutoxboruto$random.nextFloat() * 0.1f) : (0.03f + narutoxboruto$random.nextFloat() * 0.05f);
                Vector3f end = new Vector3f(base);
                end.add(
                    (narutoxboruto$random.nextFloat() - 0.5f) * sparkLen,
                    (narutoxboruto$random.nextFloat() - 0.5f) * sparkLen,
                    (narutoxboruto$random.nextFloat() - 0.5f) * sparkLen
                );
                
                // Shorter duration during burst for rapid flashing
                int sparkDuration = isBurst ? (20 + narutoxboruto$random.nextInt(30)) : (40 + narutoxboruto$random.nextInt(50));
                narutoxboruto$swordArcs.add(new SwordLightningArc(base, end, currentTime + sparkDuration));
            }
        }
    }
    
    /**
     * Extract vertex positions from a baked quad
     */
    @Unique
    private void narutoxboruto$extractVertices(net.minecraft.client.renderer.block.model.BakedQuad quad, List<Vector3f> vertices) {
        int[] data = quad.getVertices();
        int vertexCount = data.length / 8;
        
        for (int i = 0; i < vertexCount; i++) {
            int offset = i * 8;
            float x = Float.intBitsToFloat(data[offset]);
            float y = Float.intBitsToFloat(data[offset + 1]);
            float z = Float.intBitsToFloat(data[offset + 2]);
            // Different clamps for first person (stricter to avoid hilt) vs third person (looser to reach hilt)
            // First person: Y > 0.25 (avoid hilt), Third person: Y > 0.0 (reach full blade with offset)
            float clampThreshold = narutoxboruto$isFirstPerson ? 0.25f : 0.0f;
            // Y offset and tilt only for third person to align with blade tip
            if (y > clampThreshold) {
                if (narutoxboruto$isFirstPerson) {
                    vertices.add(new Vector3f(x, y, z));
                } else {
                    // Third person: Add Y offset of 0.25 and 1 degree tilt
                    float tiltOffset = y * 0.0175f;
                    vertices.add(new Vector3f(x + tiltOffset, y + 0.25f, z));
                }
            }
        }
    }
    
    /**
     * Renders a lightning arc with jagged segments
     */
    @Unique
    private void narutoxboruto$renderArc(Matrix4f matrix, VertexConsumer consumer, SwordLightningArc arc, 
                                          long currentTime, int color, float baseThickness) {
        float lifeRemaining = (arc.expireTime - currentTime) / 150f;
        float alpha = Math.min(1.0f, lifeRemaining * 2.5f);
        
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        
        List<Vector3f> points = narutoxboruto$generateBoltPoints(arc.start, arc.end, 4);
        
        // Render colored outer bolt
        for (int i = 0; i < points.size() - 1; i++) {
            Vector3f p1 = points.get(i);
            Vector3f p2 = points.get(i + 1);
            narutoxboruto$renderBoltSegment(matrix, consumer, p1, p2, baseThickness, r, g, b, alpha, 0);
            narutoxboruto$renderBoltSegment(matrix, consumer, p1, p2, baseThickness, r, g, b, alpha, (float)(Math.PI / 2));
        }
        
        // Render bright white core
        for (int i = 0; i < points.size() - 1; i++) {
            Vector3f p1 = points.get(i);
            Vector3f p2 = points.get(i + 1);
            narutoxboruto$renderBoltSegment(matrix, consumer, p1, p2, baseThickness * 0.4f, 1f, 1f, 1f, alpha * 0.8f, 0);
        }
    }
    
    @Unique
    private List<Vector3f> narutoxboruto$generateBoltPoints(Vector3f start, Vector3f end, int segments) {
        List<Vector3f> points = new ArrayList<>();
        points.add(new Vector3f(start));
        
        Vector3f dir = new Vector3f(end).sub(start);
        float length = dir.length();
        if (length < 0.001f) {
            points.add(new Vector3f(end));
            return points;
        }
        dir.normalize();
        
        float jitterAmount = length * 0.15f;
        
        for (int i = 1; i < segments; i++) {
            float t = (float)i / segments;
            Vector3f point = new Vector3f(start).add(new Vector3f(dir).mul(length * t));
            point.x += (narutoxboruto$random.nextFloat() - 0.5f) * jitterAmount;
            point.y += (narutoxboruto$random.nextFloat() - 0.5f) * jitterAmount;
            point.z += (narutoxboruto$random.nextFloat() - 0.5f) * jitterAmount;
            points.add(point);
        }
        
        points.add(new Vector3f(end));
        return points;
    }
    
    @Unique
    private void narutoxboruto$renderBoltSegment(Matrix4f matrix, VertexConsumer consumer,
                                                  Vector3f p1, Vector3f p2, float thickness,
                                                  float r, float g, float b, float alpha, float rotation) {
        Vector3f dir = new Vector3f(p2).sub(p1);
        if (dir.lengthSquared() < 0.0001f) return;
        dir.normalize();
        
        Vector3f perp;
        if (Math.abs(dir.y) < 0.9f) {
            perp = new Vector3f(dir).cross(new Vector3f(0, 1, 0)).normalize();
        } else {
            perp = new Vector3f(dir).cross(new Vector3f(1, 0, 0)).normalize();
        }
        
        if (rotation != 0) {
            Vector3f perp2 = new Vector3f(dir).cross(perp).normalize();
            float cos = (float)Math.cos(rotation);
            float sin = (float)Math.sin(rotation);
            perp = new Vector3f(
                perp.x * cos + perp2.x * sin,
                perp.y * cos + perp2.y * sin,
                perp.z * cos + perp2.z * sin
            );
        }
        
        perp.mul(thickness);
        
        Vector3f v1 = new Vector3f(p1).add(perp);
        Vector3f v2 = new Vector3f(p1).sub(perp);
        Vector3f v3 = new Vector3f(p2).sub(perp);
        Vector3f v4 = new Vector3f(p2).add(perp);
        
        int argb = ((int)(alpha * 255) << 24) | ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255);
        
        consumer.addVertex(matrix, v1.x, v1.y, v1.z).setColor(argb);
        consumer.addVertex(matrix, v2.x, v2.y, v2.z).setColor(argb);
        consumer.addVertex(matrix, v3.x, v3.y, v3.z).setColor(argb);
        consumer.addVertex(matrix, v4.x, v4.y, v4.z).setColor(argb);
    }
}
