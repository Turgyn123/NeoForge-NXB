package net.narutoxboruto.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.narutoxboruto.client.PlayerData;
import net.narutoxboruto.items.swords.Kiba;
import net.narutoxboruto.main.Main;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Renders lightning effects on the Kiba sword model and Lightning Chakra Mode cloak.
 * For the sword: renders the model again with a glowing overlay that pulses.
 * For the cloak: uses lightning arcs around the player body.
 */
@EventBusSubscriber(modid = Main.MOD_ID, value = Dist.CLIENT)
public class KibaLightningOverlay {
    
    private static final Random random = new Random();
    
    // Lightning colors - RGB format: 0x00RRGGBB (alpha handled separately in render)
    // Bright electric blue for both sword and cloak
    private static final int KIBA_LIGHTNING_COLOR = 0x40B0FF; // Bright electric blue for sword
    private static final int CLOAK_LIGHTNING_COLOR = 0x60C0FF; // Bright electric blue for cloak
    
    // Arc generation timing - separate for sword and cloak
    private static long lastSwordArcGenTime = 0;
    private static long lastCloakArcGenTime = 0;
    private static final long SWORD_ARC_INTERVAL_MS = 50;
    private static final long CLOAK_ARC_INTERVAL_MS = 160; // Reduced frequency (was 80)
    private static final long CLOAK_BURST_ARC_INTERVAL_MS = 20; // Fast interval during activation burst
    
    // Store current arcs for rendering - separate lists
    private static final List<LightningArc> swordArcs = new ArrayList<>();
    private static final List<LightningArc> cloakArcs = new ArrayList<>();
    private static final int MAX_SWORD_ARCS = 15;
    private static final int MAX_CLOAK_ARCS = 40; // Increased for better body coverage
    private static final int BURST_MAX_CLOAK_ARCS = 80; // More arcs during burst
    
    // NOTE: Kiba sword lightning is now handled by MixinItemRenderer
    // which renders the glow directly on the model using the same UV coordinates.
    // The onRenderHand method below is disabled.
    
    /**
     * First-person hand rendering - DISABLED, now handled by MixinItemRenderer
     * The mixin renders the model again with a glowing overlay using same UVs.
     */
    // @SubscribeEvent - DISABLED
    public static void onRenderHand_DISABLED(RenderHandEvent event) {
        // Now handled by MixinItemRenderer for perfect UV-locked rendering
    }
    
    /**
     * First-person cloak rendering - uses RenderLevelStageEvent since RenderPlayerEvent doesn't fire in first-person
     * Renders the lightning cloak around the player in world space
     */
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        // Only render after translucent stage for proper blending
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;
        
        // Only render in first-person (third-person handled by RenderPlayerEvent)
        if (!mc.options.getCameraType().isFirstPerson()) return;
        
        // Check if Lightning Chakra Mode is active
        if (!PlayerData.isLightningChakraModeActive()) return;
        
        PoseStack poseStack = event.getPoseStack();
        
        // Get player position interpolated for smooth rendering
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 playerPos = player.getPosition(partialTick);
        Vec3 cameraPos = event.getCamera().getPosition();
        
        // Translate to player position relative to camera
        poseStack.pushPose();
        poseStack.translate(
            playerPos.x - cameraPos.x,
            playerPos.y - cameraPos.y,
            playerPos.z - cameraPos.z
        );
        
        // Render cloak at player position - use first-person variant (lower body only)
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        renderCloakLightningFirstPerson(poseStack, bufferSource, player);
        bufferSource.endBatch(RenderType.lightning());
        
        poseStack.popPose();
    }
    
    /**
     * Third-person player rendering - renders lightning on sword AND cloak
     */
    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        Minecraft mc = Minecraft.getInstance();
        
        // Only render effects for the local player (since we use PlayerData which is local)
        // For other players, we would need a different sync mechanism
        if (player != mc.player) return;
        
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource bufferSource = event.getMultiBufferSource();
        
        // === LIGHTNING CHAKRA MODE CLOAK ===
        if (PlayerData.isLightningChakraModeActive()) {
            poseStack.pushPose();
            // Cloak renders at player origin - the pose stack is already transformed
            renderCloakLightning(poseStack, bufferSource, player);
            poseStack.popPose();
        }
        
        // NOTE: Kiba sword lightning is now handled by MixinItemRenderer
        // which renders the glow directly on the model using the same UV coordinates.
    }
    
    // ==================== SWORD LIGHTNING OVERLAY (DISABLED) ====================
    // Now handled by MixinItemRenderer for perfect UV-locked rendering
    // All old sword lightning methods have been removed - see git history for reference
    
    // ==================== CLOAK LIGHTNING ====================
    
    /**
     * First-person cloak rendering - only shows lower body arcs to avoid blocking view
     */
    private static void renderCloakLightningFirstPerson(PoseStack poseStack, MultiBufferSource buffer, Player player) {
        long currentTime = System.currentTimeMillis();
        
        // Check if we're in activation burst mode
        boolean isBurst = PlayerData.isCloakBurstActive();
        long interval = isBurst ? CLOAK_BURST_ARC_INTERVAL_MS : CLOAK_ARC_INTERVAL_MS;
        float thickness = isBurst ? 0.03f : 0.018f; // Thicker bolts during burst
        
        if (currentTime - lastCloakArcGenTime > interval) {
            lastCloakArcGenTime = currentTime;
            generateCloakArcs(currentTime, player, isBurst);
        }
        
        cloakArcs.removeIf(arc -> currentTime > arc.expireTime);
        
        VertexConsumer consumer = buffer.getBuffer(RenderType.lightning());
        Matrix4f matrix = poseStack.last().pose();
        
        // Only render arcs below chest level (Y < 1.0) to avoid blocking first-person view
        float maxY = 1.0f;
        for (LightningArc arc : cloakArcs) {
            // Skip arcs that are primarily in upper body/head area
            if (arc.start.y() < maxY && arc.end.y() < maxY) {
                renderArc(matrix, consumer, arc, currentTime, CLOAK_LIGHTNING_COLOR, thickness);
            }
        }
    }
    
    /**
     * Third-person cloak rendering - shows full body arcs
     */
    private static void renderCloakLightning(PoseStack poseStack, MultiBufferSource buffer, Player player) {
        long currentTime = System.currentTimeMillis();
        
        // Check if we're in activation burst mode
        boolean isBurst = PlayerData.isCloakBurstActive();
        long interval = isBurst ? CLOAK_BURST_ARC_INTERVAL_MS : CLOAK_ARC_INTERVAL_MS;
        float thickness = isBurst ? 0.03f : 0.018f; // Thicker bolts during burst
        
        if (currentTime - lastCloakArcGenTime > interval) {
            lastCloakArcGenTime = currentTime;
            generateCloakArcs(currentTime, player, isBurst);
        }
        
        cloakArcs.removeIf(arc -> currentTime > arc.expireTime);
        
        VertexConsumer consumer = buffer.getBuffer(RenderType.lightning());
        Matrix4f matrix = poseStack.last().pose();
        
        for (LightningArc arc : cloakArcs) {
            renderArc(matrix, consumer, arc, currentTime, CLOAK_LIGHTNING_COLOR, thickness);
        }
    }
    
    private static void generateCloakArcs(long currentTime, Player player, boolean isBurst) {
        int maxArcs = isBurst ? BURST_MAX_CLOAK_ARCS : MAX_CLOAK_ARCS;
        while (cloakArcs.size() >= maxArcs) {
            cloakArcs.remove(0);
        }
        
        // Player body dimensions with more organic variance
        float bodyWidth = 0.35f;  // Half width of body
        float headY = 1.7f;       // Top of head
        float neckY = 1.5f;       // Neck level
        float shoulderY = 1.3f;   // Shoulder level
        float chestY = 1.1f;      // Chest level
        float hipY = 0.85f;       // Hip level
        float headRadius = 0.22f; // Head radius
        
        // Burst multiplier for arc counts
        int burstMult = isBurst ? 4 : 1;
        int baseDuration = isBurst ? 40 : 100; // Shorter duration during burst for rapid flashing
        
        // === DIAGONAL BODY ARCS - Chaotic lightning across torso ===
        for (int i = 0; i < 3 * burstMult; i++) {
            // Random start point on body surface
            float startAngle = random.nextFloat() * (float)(Math.PI * 2);
            float startY = hipY + random.nextFloat() * (shoulderY - hipY);
            float startRadius = bodyWidth * (0.7f + random.nextFloat() * 0.4f);
            
            // End point at different angle AND height for diagonal effect
            float endAngle = startAngle + (random.nextFloat() - 0.5f) * (float)(Math.PI * 1.5f);
            float endY = startY + (random.nextFloat() - 0.3f) * 0.6f; // Bias upward
            endY = Math.max(0.2f, Math.min(endY, neckY));
            float endRadius = bodyWidth * (0.7f + random.nextFloat() * 0.4f);
            
            Vector3f start = new Vector3f(
                (float)Math.cos(startAngle) * startRadius,
                startY,
                (float)Math.sin(startAngle) * startRadius
            );
            Vector3f end = new Vector3f(
                (float)Math.cos(endAngle) * endRadius,
                endY,
                (float)Math.sin(endAngle) * endRadius
            );
            
            cloakArcs.add(new LightningArc(start, end, currentTime + baseDuration + random.nextInt(baseDuration)));
        }
        
        // === HEAD ARCS - More coverage around the head ===
        for (int i = 0; i < 2 * burstMult; i++) {
            float startAngle = random.nextFloat() * (float)(Math.PI * 2);
            float endAngle = startAngle + (random.nextFloat() - 0.5f) * (float)(Math.PI);
            
            // Arcs on the sphere of the head
            float startPhi = 0.3f + random.nextFloat() * 0.5f; // Elevation on head sphere
            float endPhi = 0.2f + random.nextFloat() * 0.6f;
            
            float r1 = headRadius * (float)Math.sin(startPhi * Math.PI);
            float y1 = neckY + headRadius * (float)Math.cos(startPhi * Math.PI) * 0.8f;
            
            float r2 = headRadius * (float)Math.sin(endPhi * Math.PI);
            float y2 = neckY + headRadius * (float)Math.cos(endPhi * Math.PI) * 0.8f;
            
            Vector3f start = new Vector3f(
                (float)Math.cos(startAngle) * r1,
                y1,
                (float)Math.sin(startAngle) * r1
            );
            Vector3f end = new Vector3f(
                (float)Math.cos(endAngle) * r2,
                y2,
                (float)Math.sin(endAngle) * r2
            );
            
            cloakArcs.add(new LightningArc(start, end, currentTime + (int)(baseDuration * 0.7f) + random.nextInt((int)(baseDuration * 0.6f))));
        }
        
        // === HEAD TOP CORONA - Sparks above the head ===
        float coronaChance = isBurst ? 1.0f : 0.6f;
        int coronaCount = isBurst ? 3 : 1;
        for (int c = 0; c < coronaCount; c++) {
            if (random.nextFloat() < coronaChance) {
                float angle1 = random.nextFloat() * (float)(Math.PI * 2);
                float angle2 = angle1 + (random.nextFloat() - 0.5f) * (float)Math.PI;
                float r = 0.05f + random.nextFloat() * 0.1f;
                
                Vector3f start = new Vector3f(
                    (float)Math.cos(angle1) * r,
                    headY - 0.1f + random.nextFloat() * 0.1f,
                    (float)Math.sin(angle1) * r
                );
                Vector3f end = new Vector3f(
                    (float)Math.cos(angle2) * (r + 0.08f),
                    headY + random.nextFloat() * 0.15f,
                    (float)Math.sin(angle2) * (r + 0.08f)
                );
                
                cloakArcs.add(new LightningArc(start, end, currentTime + (int)(baseDuration * 0.5f) + random.nextInt((int)(baseDuration * 0.5f))));
            }
        }
        
        // === SHOULDER TO HEAD ARCS - Connect body to head ===
        float shoulderChance = isBurst ? 0.8f : 0.4f;
        for (int side = -1; side <= 1; side += 2) {
            if (random.nextFloat() < shoulderChance) {
                float shoulderX = side * bodyWidth * 0.9f;
                float headX = side * headRadius * 0.5f * (0.5f + random.nextFloat());
                
                Vector3f start = new Vector3f(
                    shoulderX,
                    shoulderY + random.nextFloat() * 0.1f,
                    (random.nextFloat() - 0.5f) * 0.1f
                );
                Vector3f end = new Vector3f(
                    headX,
                    neckY + random.nextFloat() * 0.2f,
                    (random.nextFloat() - 0.5f) * headRadius * 0.5f
                );
                
                cloakArcs.add(new LightningArc(start, end, currentTime + (int)(baseDuration * 0.8f) + random.nextInt((int)(baseDuration * 0.7f))));
            }
        }
        
        // === HORIZONTAL CHAOTIC ARCS - Wrapping with variance ===
        for (int i = 0; i < 2 * burstMult; i++) {
            float y = hipY + random.nextFloat() * (chestY - hipY);
            float startAngle = random.nextFloat() * (float)(Math.PI * 2);
            float arcSpan = (float)(Math.PI * (0.4 + random.nextFloat() * 0.8));
            float endAngle = startAngle + arcSpan;
            
            float startRadius = bodyWidth * (0.8f + random.nextFloat() * 0.3f);
            float endRadius = bodyWidth * (0.8f + random.nextFloat() * 0.3f);
            float yVariance = (random.nextFloat() - 0.5f) * 0.25f;
            
            Vector3f start = new Vector3f(
                (float)Math.cos(startAngle) * startRadius,
                y,
                (float)Math.sin(startAngle) * startRadius
            );
            Vector3f end = new Vector3f(
                (float)Math.cos(endAngle) * endRadius,
                y + yVariance,
                (float)Math.sin(endAngle) * endRadius
            );
            
            cloakArcs.add(new LightningArc(start, end, currentTime + (int)(baseDuration * 0.9f) + random.nextInt((int)(baseDuration * 0.7f))));
        }
        
        // === ARM ARCS - Along the arms with more randomness ===
        float armChance = isBurst ? 0.9f : 0.45f;
        for (int side = -1; side <= 1; side += 2) {
            if (random.nextFloat() < armChance) {
                float armX = side * (bodyWidth + 0.05f + random.nextFloat() * 0.1f);
                float armStartY = shoulderY - random.nextFloat() * 0.15f;
                float armEndY = shoulderY - 0.3f - random.nextFloat() * 0.4f;
                float zWander = (random.nextFloat() - 0.5f) * 0.2f;
                
                Vector3f start = new Vector3f(armX, armStartY, (random.nextFloat() - 0.5f) * 0.1f);
                Vector3f end = new Vector3f(
                    armX + side * (0.1f + random.nextFloat() * 0.15f),
                    armEndY,
                    zWander
                );
                
                cloakArcs.add(new LightningArc(start, end, currentTime + (int)(baseDuration * 0.9f) + random.nextInt((int)(baseDuration * 0.8f))));
            }
        }
        
        // === LEG ARCS - With more organic feel ===
        float legChance = isBurst ? 0.7f : 0.35f;
        for (int side = -1; side <= 1; side += 2) {
            if (random.nextFloat() < legChance) {
                float legX = side * bodyWidth * (0.4f + random.nextFloat() * 0.2f);
                float startY = hipY - random.nextFloat() * 0.15f;
                float endY = 0.1f + random.nextFloat() * 0.35f;
                float zOff = (random.nextFloat() - 0.5f) * 0.15f;
                
                Vector3f start = new Vector3f(legX, startY, zOff * 0.3f);
                Vector3f end = new Vector3f(
                    legX + (random.nextFloat() - 0.5f) * 0.08f,
                    endY,
                    zOff
                );
                
                cloakArcs.add(new LightningArc(start, end, currentTime + baseDuration + random.nextInt((int)(baseDuration * 0.9f))));
            }
        }
        
        // === RANDOM SURFACE SPARKS - Small chaotic sparks ===
        int sparkCount = isBurst ? 15 : 5;
        for (int i = 0; i < sparkCount; i++) {
            float y = 0.15f + random.nextFloat() * 1.55f;
            float angle = random.nextFloat() * (float)(Math.PI * 2);
            
            // Vary radius based on body shape at that height
            float radius;
            if (y > neckY) {
                radius = headRadius * (0.7f + random.nextFloat() * 0.4f);
            } else {
                radius = bodyWidth * (0.75f + random.nextFloat() * 0.35f);
            }
            
            float x = (float)Math.cos(angle) * radius;
            float z = (float)Math.sin(angle) * radius;
            
            // Random direction spark
            float sparkLen = 0.04f + random.nextFloat() * 0.08f;
            float sparkAngle = random.nextFloat() * (float)(Math.PI * 2);
            float sparkY = (random.nextFloat() - 0.5f) * sparkLen * 1.5f;
            
            Vector3f start = new Vector3f(x, y, z);
            Vector3f end = new Vector3f(
                x + (float)Math.cos(sparkAngle) * sparkLen,
                y + sparkY,
                z + (float)Math.sin(sparkAngle) * sparkLen
            );
            
            cloakArcs.add(new LightningArc(start, end, currentTime + (int)(baseDuration * 0.4f) + random.nextInt((int)(baseDuration * 0.5f))));
        }
    }
    
    // ==================== SHARED RENDERING ====================
    
    private static void renderArc(Matrix4f matrix, VertexConsumer consumer, LightningArc arc, long currentTime, int color, float baseThickness) {
        float lifeRemaining = (arc.expireTime - currentTime) / 150f;
        float alpha = Math.min(1.0f, lifeRemaining * 2.5f);
        
        // Color format: 0x00RRGGBB
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        
        List<Vector3f> points = generateBoltPoints(arc.start, arc.end, 4);
        
        float thickness = baseThickness;
        
        // Render colored outer bolt
        for (int i = 0; i < points.size() - 1; i++) {
            Vector3f p1 = points.get(i);
            Vector3f p2 = points.get(i + 1);
            renderBoltSegment(matrix, consumer, p1, p2, thickness, r, g, b, alpha, 0);
            renderBoltSegment(matrix, consumer, p1, p2, thickness, r, g, b, alpha, (float)(Math.PI / 2));
        }
        
        // Render bright white core
        for (int i = 0; i < points.size() - 1; i++) {
            Vector3f p1 = points.get(i);
            Vector3f p2 = points.get(i + 1);
            renderBoltSegment(matrix, consumer, p1, p2, thickness * 0.4f, 1f, 1f, 1f, alpha * 0.8f, 0);
        }
    }
    
    private static List<Vector3f> generateBoltPoints(Vector3f start, Vector3f end, int segments) {
        List<Vector3f> points = new ArrayList<>();
        points.add(new Vector3f(start));
        
        Vector3f dir = new Vector3f(end).sub(start);
        float length = dir.length();
        if (length < 0.001f) {
            points.add(new Vector3f(end));
            return points;
        }
        dir.normalize();
        
        float jitterAmount = length * 0.1f;
        
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
    
    private static void renderBoltSegment(Matrix4f matrix, VertexConsumer consumer,
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
    
    private static class LightningArc {
        final Vector3f start;
        final Vector3f end;
        final long expireTime;
        
        LightningArc(Vector3f start, Vector3f end, long expireTime) {
            this.start = start;
            this.end = end;
            this.expireTime = expireTime;
        }
    }
}
