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
import net.narutoxboruto.particles.ModParticles;

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
            
            // Spawn activation particles burst - arcing off the sword
            if (kibaActive.isActive() && serverPlayer.level() instanceof ServerLevel serverLevel) {
                spawnSwordArcParticles(serverLevel, serverPlayer, 20);
            }
            
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
                
                // Spawn ambient lightning arcs around the sword while active
                if (serverPlayer.level() instanceof ServerLevel serverLevel) {
                    spawnSwordArcParticles(serverLevel, serverPlayer, 3);
                }
            } else {
                // Not enough chakra - deactivate immediately
                kibaActive.setActive(false, serverPlayer);
                serverPlayer.displayClientMessage(
                        Component.translatable("msg.no_chakra"), true);
            }
        }
    }

    public static int getChakraDrainPerSecond() {
        return CHAKRA_DRAIN_PER_SECOND;
    }

    /**
     * Spawns lightning arcs coming off the sword position.
     * Creates actual arc shapes by spawning particles along curved paths.
     */
    public static void spawnSwordArcParticles(ServerLevel serverLevel, ServerPlayer player, int arcCount) {
        // Get the sword tip position (in front of player, offset by arm/sword length)
        Vec3 lookVec = player.getViewVector(1.0F);
        Vec3 rightVec = lookVec.cross(new Vec3(0, 1, 0)).normalize();
        
        // Sword is held to the right side, tip extends forward
        double swordBaseX = player.getX() + rightVec.x * 0.3;
        double swordBaseY = player.getY() + 1.0;
        double swordBaseZ = player.getZ() + rightVec.z * 0.3;
        
        double swordTipX = swordBaseX + lookVec.x * 1.0;
        double swordTipY = swordBaseY + 0.3;
        double swordTipZ = swordBaseZ + lookVec.z * 1.0;
        
        // Spawn multiple arc "bolts"
        for (int arc = 0; arc < arcCount; arc++) {
            // Random start point along the blade
            double t = Math.random();
            double startX = swordBaseX + (swordTipX - swordBaseX) * t;
            double startY = swordBaseY + (swordTipY - swordBaseY) * t;
            double startZ = swordBaseZ + (swordTipZ - swordBaseZ) * t;
            
            // Random end point - arc destination
            double arcLength = 0.5 + Math.random() * 0.8;
            double arcAngle = Math.random() * Math.PI * 2;
            double arcPitch = (Math.random() - 0.3) * Math.PI * 0.5; // Slight upward bias
            
            double endX = startX + Math.cos(arcAngle) * Math.cos(arcPitch) * arcLength;
            double endY = startY + Math.sin(arcPitch) * arcLength;
            double endZ = startZ + Math.sin(arcAngle) * Math.cos(arcPitch) * arcLength;
            
            // Spawn particles along the arc path with jagged offsets
            spawnLightningBolt(serverLevel, startX, startY, startZ, endX, endY, endZ, 6);
        }
    }
    
    /**
     * Spawns a lightning bolt between two points.
     * Creates a jagged line of particles that looks like an electric arc.
     */
    private static void spawnLightningBolt(ServerLevel serverLevel, 
            double startX, double startY, double startZ,
            double endX, double endY, double endZ, int segments) {
        
        double prevX = startX;
        double prevY = startY;
        double prevZ = startZ;
        
        for (int i = 1; i <= segments; i++) {
            double progress = (double) i / segments;
            
            // Base position along the line
            double baseX = startX + (endX - startX) * progress;
            double baseY = startY + (endY - startY) * progress;
            double baseZ = startZ + (endZ - startZ) * progress;
            
            // Add jagged offset (less at endpoints, more in middle)
            double jaggedStrength = 0.15 * Math.sin(progress * Math.PI); // Peak in middle
            double offsetX = (Math.random() - 0.5) * jaggedStrength;
            double offsetY = (Math.random() - 0.5) * jaggedStrength;
            double offsetZ = (Math.random() - 0.5) * jaggedStrength;
            
            double currentX = baseX + offsetX;
            double currentY = baseY + offsetY;
            double currentZ = baseZ + offsetZ;
            
            // Velocity points toward next segment for "flowing" effect
            double vx = (currentX - prevX) * 0.5;
            double vy = (currentY - prevY) * 0.5;
            double vz = (currentZ - prevZ) * 0.5;
            
            // Use custom lightning sparks particle
            serverLevel.sendParticles(
                    ModParticles.LIGHTNING_SPARKS.get(),
                    currentX, currentY, currentZ,
                    1,
                    vx, vy, vz,
                    0.01D
            );
            
            prevX = currentX;
            prevY = currentY;
            prevZ = currentZ;
        }
    }
    
    /**
     * Spawns electric shock particles wrapping around a stunned entity.
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
            
            // Spawn the bolt
            spawnLightningBolt(serverLevel, startX, startY, startZ, endX, endY, endZ, 5);
        }
        
        // Add some vertical bolts running up/down
        for (int i = 0; i < 2; i++) {
            double angle = Math.random() * Math.PI * 2;
            double x = centerX + Math.cos(angle) * radius * 0.3;
            double z = centerZ + Math.sin(angle) * radius * 0.3;
            
            spawnLightningBolt(serverLevel, x, centerY, z, x + (Math.random() - 0.5) * 0.3, centerY + height, z + (Math.random() - 0.5) * 0.3, 4);
        }
    }
}
