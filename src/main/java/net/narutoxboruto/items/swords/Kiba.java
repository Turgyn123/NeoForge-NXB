package net.narutoxboruto.items.swords;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.Vec3;
import net.narutoxboruto.attachments.MainAttachment;
import net.narutoxboruto.attachments.info.Chakra;
import net.narutoxboruto.attachments.info.ReleaseList;
import net.narutoxboruto.attachments.modes.KibaActive;
import net.narutoxboruto.entities.effects.LightningArcEntity;

import java.util.List;

/**
 * Kiba - The Lightning Blades (雷刀・牙)
 * One of the Seven Swords of the Mist.
 * 
 * Ability: When activated, the sword is imbued with lightning.
 * - Requires Lightning Release affinity to use
 * - Drains 5 chakra per second while active
 * - Stuns enemies hit while ability is active
 * - Right-click to toggle on/off
 */
public class Kiba extends SwordItem implements Vanishable {
    private static final int CHAKRA_DRAIN_PER_SECOND = 5;
    private static final int STUN_DURATION_TICKS = 40; // 2 seconds
    private static final int STUN_AMPLIFIER = 4; // Slowness V (almost frozen)

    public Kiba(Properties pProperties) {
        super(SwordCustomTiers.KIBA, pProperties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pHand) {
        ItemStack stack = pPlayer.getItemInHand(pHand);
        
        if (pPlayer instanceof ServerPlayer serverPlayer) {
            // Check if player has lightning release
            ReleaseList releaseList = serverPlayer.getData(MainAttachment.RELEASE_LIST);
            List<String> releases = releaseList.getReleasesAsList();
            
            boolean hasLightning = releases.stream()
                    .anyMatch(r -> r.equalsIgnoreCase("lightning"));
            
            if (!hasLightning) {
                serverPlayer.displayClientMessage(
                        Component.translatable("msg.kiba.no_lightning_release"), true);
                return InteractionResultHolder.fail(stack);
            }
            
            // Check if player has chakra to activate
            Chakra chakra = serverPlayer.getData(MainAttachment.CHAKRA);
            KibaActive kibaActive = serverPlayer.getData(MainAttachment.KIBA_ACTIVE);
            
            if (!kibaActive.isActive() && chakra.getValue() < CHAKRA_DRAIN_PER_SECOND) {
                serverPlayer.displayClientMessage(
                        Component.translatable("msg.no_chakra"), true);
                return InteractionResultHolder.fail(stack);
            }
            
            // Toggle the ability
            kibaActive.toggle(serverPlayer);
            
            // NOTE: Activation particles now handled by client-side burst system in MixinItemRenderer
            // which detects state change and renders intense lightning for 0.5 seconds
            
            String messageKey = kibaActive.isActive() ? "sword_ability.activate" : "sword_ability.deactivate";
            serverPlayer.displayClientMessage(
                    Component.translatable(messageKey, Component.translatable("item.narutoxboruto.kiba")), true);
        }
        
        return InteractionResultHolder.sidedSuccess(stack, pLevel.isClientSide());
    }

    @Override
    public boolean hurtEnemy(ItemStack pStack, LivingEntity pTarget, LivingEntity pAttacker) {
        pStack.hurtAndBreak(1, pAttacker, EquipmentSlot.MAINHAND);
        
        if (pAttacker instanceof ServerPlayer serverPlayer) {
            KibaActive kibaActive = serverPlayer.getData(MainAttachment.KIBA_ACTIVE);
            
            if (kibaActive.isActive()) {
                // Apply stun effect (heavy slowness + weakness + brief blindness)
                pTarget.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SLOWDOWN, 
                        STUN_DURATION_TICKS, 
                        STUN_AMPLIFIER, 
                        false, true));
                
                pTarget.addEffect(new MobEffectInstance(
                        MobEffects.WEAKNESS, 
                        STUN_DURATION_TICKS, 
                        2, // Weakness III
                        false, true));
                
                // Brief shock/stun effect
                pTarget.addEffect(new MobEffectInstance(
                        MobEffects.BLINDNESS, 
                        10, // 0.5 seconds
                        0, 
                        false, false));
                
                // Spawn lightning particles around the stunned target - electric shock effect
                if (serverPlayer.level() instanceof ServerLevel serverLevel) {
                    spawnElectricShockParticles(serverLevel, pTarget);
                }
            }
        }
        
        return true;
    }

    /**
     * Called by StatEvents to drain chakra every second while Kiba is active.
     * Also handles automatic deactivation when chakra runs out.
     */
    public static void tickChakraDrain(ServerPlayer serverPlayer) {
        KibaActive kibaActive = serverPlayer.getData(MainAttachment.KIBA_ACTIVE);
        
        if (kibaActive.isActive()) {
            Chakra chakra = serverPlayer.getData(MainAttachment.CHAKRA);
            
            // Check if player still has the sword equipped
            ItemStack mainHand = serverPlayer.getMainHandItem();
            ItemStack offHand = serverPlayer.getOffhandItem();
            boolean holdingKiba = mainHand.getItem() instanceof Kiba || offHand.getItem() instanceof Kiba;
            
            if (!holdingKiba) {
                // Auto-deactivate if sword is no longer held
                kibaActive.setActive(false, serverPlayer);
                return;
            }
            
            if (chakra.getValue() >= CHAKRA_DRAIN_PER_SECOND) {
                chakra.subValue(CHAKRA_DRAIN_PER_SECOND, serverPlayer);
            } else {
                // Not enough chakra - deactivate immediately
                kibaActive.setActive(false, serverPlayer);
                serverPlayer.displayClientMessage(
                        Component.translatable("msg.no_chakra"), true);
            }
        }
    }
    
    /**
     * Called by StatEvents every half second (10 ticks) to spawn visual lightning aura.
     * NOTE: Visual effects now handled entirely by client-side MixinItemRenderer
     * which renders lightning directly on the sword model with proper positioning.
     */
    public static void tickVisualEffects(ServerPlayer serverPlayer) {
        // Visual effects now handled client-side - see MixinItemRenderer
        // Keeping method signature for compatibility with StatEvents
    }

    public static int getChakraDrainPerSecond() {
        return CHAKRA_DRAIN_PER_SECOND;
    }

    // Lightning color for Kiba - bright electric blue/cyan (matching reference image)
    private static final int KIBA_LIGHTNING_COLOR = 0xE060FFFF; // Bright cyan-blue

    /**
     * Spawns lightning aura around the sword using the LightningArcEntity system.
     * Creates both arcs emanating from the blade AND an aura wrapping around it.
     */
    public static void spawnSwordArcParticles(ServerLevel serverLevel, ServerPlayer player, int arcCount) {
        // Get player facing direction (horizontal only)
        float yaw = player.getYRot() * ((float)Math.PI / 180F);
        
        // Right hand offset (sword held to the right of player)
        double rightX = -Math.cos(yaw) * 0.35;
        double rightZ = -Math.sin(yaw) * 0.35;
        
        // Sword base is at hand level (right side of player)
        double swordBaseX = player.getX() + rightX;
        double swordBaseY = player.getY() + 0.9; // Hand height
        double swordBaseZ = player.getZ() + rightZ;
        
        // Sword tip extends UPWARD (blade points up when held)
        // Slight forward tilt based on player look
        double swordTipX = swordBaseX + Math.sin(yaw) * 0.15; // Slight forward tilt
        double swordTipY = swordBaseY + 1.1; // Blade extends upward
        double swordTipZ = swordBaseZ - Math.cos(yaw) * 0.15;
        
        // Spawn small lightning sparks emanating FROM the blade
        for (int arc = 0; arc < arcCount; arc++) {
            // Random start point along the blade
            double t = Math.random();
            double startX = swordBaseX + (swordTipX - swordBaseX) * t;
            double startY = swordBaseY + (swordTipY - swordBaseY) * t;
            double startZ = swordBaseZ + (swordTipZ - swordBaseZ) * t;
            
            // Smaller arcs for subtle sparks
            double arcLength = 0.2 + Math.random() * 0.3;
            double arcAngle = Math.random() * Math.PI * 2;
            double arcPitch = (Math.random() - 0.3) * Math.PI * 0.5;
            
            double endX = startX + Math.cos(arcAngle) * Math.cos(arcPitch) * arcLength;
            double endY = startY + Math.sin(arcPitch) * arcLength;
            double endZ = startZ + Math.sin(arcAngle) * Math.cos(arcPitch) * arcLength;
            
            // Small sparks with moderate duration
            LightningArcEntity.spawnArcBetween(
                    serverLevel,
                    new Vec3(startX, startY, startZ),
                    new Vec3(endX, endY, endZ),
                    KIBA_LIGHTNING_COLOR,
                    8, // Longer duration for persistence
                    0.015f + (float)(Math.random() * 0.01f)
            );
        }
        
        // Spawn persistent aura arcs ALONG the blade (wrapping around the sword) - longer duration for glow effect
        spawnSwordAura(serverLevel, player, swordBaseX, swordBaseY, swordBaseZ, swordTipX, swordTipY, swordTipZ, 3);
    }
    
    /**
     * Spawn lightning aura that envelops and wraps around the sword blade.
     * Creates a coating effect with arcs running along the blade and wrapping around it.
     */
    private static void spawnSwordAura(ServerLevel serverLevel, ServerPlayer player, 
            double baseX, double baseY, double baseZ, double tipX, double tipY, double tipZ, int count) {
        
        // Blade direction vector
        double bladeX = tipX - baseX;
        double bladeY = tipY - baseY;
        double bladeZ = tipZ - baseZ;
        double bladeLength = Math.sqrt(bladeX * bladeX + bladeY * bladeY + bladeZ * bladeZ);
        
        // Normalize blade vector
        double bladeDirX = bladeX / bladeLength;
        double bladeDirY = bladeY / bladeLength;
        double bladeDirZ = bladeZ / bladeLength;
        
        // Get perpendicular vectors for wrapping around the blade
        Vec3 bladeDir = new Vec3(bladeDirX, bladeDirY, bladeDirZ);
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 perpA = bladeDir.cross(up).normalize();
        if (perpA.lengthSqr() < 0.01) {
            perpA = bladeDir.cross(new Vec3(1, 0, 0)).normalize();
        }
        Vec3 perpB = bladeDir.cross(perpA).normalize();
        
        double wrapRadius = 0.05; // Tight around the blade surface
        
        // === LONGITUDINAL ARCS - Run along the blade length ===
        for (int i = 0; i < count; i++) {
            // Start and end at different points along the blade
            double t1 = Math.random() * 0.4;
            double t2 = 0.6 + Math.random() * 0.4;
            
            // Random angle around the blade circumference
            double angle = Math.random() * Math.PI * 2;
            double offsetX = perpA.x * Math.cos(angle) * wrapRadius + perpB.x * Math.sin(angle) * wrapRadius;
            double offsetY = perpA.y * Math.cos(angle) * wrapRadius + perpB.y * Math.sin(angle) * wrapRadius;
            double offsetZ = perpA.z * Math.cos(angle) * wrapRadius + perpB.z * Math.sin(angle) * wrapRadius;
            
            double startX = baseX + bladeX * t1 + offsetX;
            double startY = baseY + bladeY * t1 + offsetY;
            double startZ = baseZ + bladeZ * t1 + offsetZ;
            
            double endX = baseX + bladeX * t2 + offsetX;
            double endY = baseY + bladeY * t2 + offsetY;
            double endZ = baseZ + bladeZ * t2 + offsetZ;
            
            LightningArcEntity.spawnArcBetween(
                    serverLevel,
                    new Vec3(startX, startY, startZ),
                    new Vec3(endX, endY, endZ),
                    KIBA_LIGHTNING_COLOR,
                    12, // Persistent glow
                    0.012f // Thin arcs for coating effect
            );
        }
        
        // === SPIRAL/WRAPPING ARCS - Wrap around the blade circumference ===
        for (int i = 0; i < count + 2; i++) {
            // Position along the blade
            double t = 0.1 + Math.random() * 0.8;
            
            // Start angle and end angle (partial wrap around blade)
            double startAngle = Math.random() * Math.PI * 2;
            double endAngle = startAngle + Math.PI * (0.5 + Math.random() * 1.0); // 90-270 degree wrap
            
            double startOffsetX = perpA.x * Math.cos(startAngle) * wrapRadius + perpB.x * Math.sin(startAngle) * wrapRadius;
            double startOffsetY = perpA.y * Math.cos(startAngle) * wrapRadius + perpB.y * Math.sin(startAngle) * wrapRadius;
            double startOffsetZ = perpA.z * Math.cos(startAngle) * wrapRadius + perpB.z * Math.sin(startAngle) * wrapRadius;
            
            double endOffsetX = perpA.x * Math.cos(endAngle) * wrapRadius + perpB.x * Math.sin(endAngle) * wrapRadius;
            double endOffsetY = perpA.y * Math.cos(endAngle) * wrapRadius + perpB.y * Math.sin(endAngle) * wrapRadius;
            double endOffsetZ = perpA.z * Math.cos(endAngle) * wrapRadius + perpB.z * Math.sin(endAngle) * wrapRadius;
            
            // Slight movement along blade as it wraps
            double tShift = 0.05 + Math.random() * 0.1;
            
            double startX = baseX + bladeX * t + startOffsetX;
            double startY = baseY + bladeY * t + startOffsetY;
            double startZ = baseZ + bladeZ * t + startOffsetZ;
            
            double endX = baseX + bladeX * (t + tShift) + endOffsetX;
            double endY = baseY + bladeY * (t + tShift) + endOffsetY;
            double endZ = baseZ + bladeZ * (t + tShift) + endOffsetZ;
            
            LightningArcEntity.spawnArcBetween(
                    serverLevel,
                    new Vec3(startX, startY, startZ),
                    new Vec3(endX, endY, endZ),
                    KIBA_LIGHTNING_COLOR,
                    10, // Slightly shorter for variety
                    0.010f // Very thin for wrapping effect
            );
        }
        
        // === TIP CORONA - Small arcs emanating from the sword tip ===
        for (int i = 0; i < 2; i++) {
            double angle = Math.random() * Math.PI * 2;
            double pitch = (Math.random() - 0.5) * Math.PI * 0.3;
            double length = 0.08 + Math.random() * 0.12;
            
            double endX = tipX + Math.cos(angle) * Math.cos(pitch) * length;
            double endY = tipY + Math.sin(pitch) * length + 0.02;
            double endZ = tipZ + Math.sin(angle) * Math.cos(pitch) * length;
            
            LightningArcEntity.spawnArcBetween(
                    serverLevel,
                    new Vec3(tipX, tipY, tipZ),
                    new Vec3(endX, endY, endZ),
                    KIBA_LIGHTNING_COLOR,
                    8,
                    0.015f
            );
        }
    }
    
    /**
     * Spawns electric shock effect on a stunned entity using lightning arc entities.
     * Creates lightning bolts that arc across the target's body.
     */
    public static void spawnElectricShockParticles(ServerLevel serverLevel, LivingEntity target) {
        double centerX = target.getX();
        double centerY = target.getY();
        double centerZ = target.getZ();
        double radius = target.getBbWidth() * 0.6;
        double height = target.getBbHeight();
        
        // Create lightning bolts arcing across the body
        int boltCount = 4;
        for (int i = 0; i < boltCount; i++) {
            // Random start point on body surface
            double startAngle = Math.random() * Math.PI * 2;
            double startHeight = Math.random() * height;
            double startX = centerX + Math.cos(startAngle) * radius;
            double startY = centerY + startHeight;
            double startZ = centerZ + Math.sin(startAngle) * radius;
            
            // End point on opposite side or different height
            double endAngle = startAngle + Math.PI * (0.5 + Math.random()); // Roughly opposite
            double endHeight = Math.random() * height;
            double endX = centerX + Math.cos(endAngle) * radius;
            double endY = centerY + endHeight;
            double endZ = centerZ + Math.sin(endAngle) * radius;
            
            // Spawn lightning arc entity
            LightningArcEntity.spawnArcBetween(
                    serverLevel,
                    new Vec3(startX, startY, startZ),
                    new Vec3(endX, endY, endZ),
                    KIBA_LIGHTNING_COLOR,
                    4, // Slightly longer duration for shock effect
                    0.015f
            );
        }
        
        // Add some vertical bolts running up/down
        for (int i = 0; i < 2; i++) {
            double angle = Math.random() * Math.PI * 2;
            double x = centerX + Math.cos(angle) * radius * 0.3;
            double z = centerZ + Math.sin(angle) * radius * 0.3;
            
            LightningArcEntity.spawnArcBetween(
                    serverLevel,
                    new Vec3(x, centerY, z),
                    new Vec3(x + (Math.random() - 0.5) * 0.3, centerY + height, z + (Math.random() - 0.5) * 0.3),
                    KIBA_LIGHTNING_COLOR,
                    4,
                    0.012f
            );
        }
    }
}
