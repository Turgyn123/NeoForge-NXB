package net.narutoxboruto.items.jutsus;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.narutoxboruto.attachments.MainAttachment;
import net.narutoxboruto.attachments.info.Chakra;
import net.narutoxboruto.attachments.info.ReleaseList;
import net.narutoxboruto.attachments.modes.LightningChakraModeActive;
import net.narutoxboruto.entities.effects.LightningArcEntity;
import net.narutoxboruto.util.ModUtil;

import java.util.List;

/**
 * Lightning Chakra Mode - A powerful lightning release jutsu that envelops
 * the user in lightning chakra, granting enhanced speed and strength.
 * 
 * Effects when active:
 * - Strength II
 * - Speed III  
 * - Lightning particle effects around the player
 * 
 * Costs:
 * - 15 chakra for initial activation
 * - 5 chakra every 5 seconds while active
 * 
 * Toggle: Right-click to activate/deactivate
 * Auto-deactivates when:
 * - Player runs out of chakra
 * - Item is removed from hotbar
 */
public class LightningChakraMode extends Item {
    
    private static final int ACTIVATION_COST = 15;
    private static final int DRAIN_COST = 5;
    private static final int DRAIN_INTERVAL_SECONDS = 5;
    
    // Effect durations - infinite (-1) so they persist while mode is active
    private static final int EFFECT_DURATION = -1; // Infinite duration
    private static final int STRENGTH_AMPLIFIER = 1; // Strength II (0-indexed)
    private static final int SPEED_AMPLIFIER = 2; // Speed III (0-indexed)

    public LightningChakraMode(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (level.isClientSide()) {
            return InteractionResultHolder.consume(stack);
        }
        
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.fail(stack);
        }
        
        // Check if player has lightning release
        ReleaseList releaseList = serverPlayer.getData(MainAttachment.RELEASE_LIST);
        List<String> releases = releaseList.getReleasesAsList();
        
        boolean hasLightning = releases.stream()
                .anyMatch(r -> r.equalsIgnoreCase("lightning"));
        
        if (!hasLightning) {
            ModUtil.displayColoredMessage(serverPlayer, "msg.no_release", ChatFormatting.RED);
            return InteractionResultHolder.fail(stack);
        }
        
        LightningChakraModeActive modeActive = serverPlayer.getData(MainAttachment.LIGHTNING_CHAKRA_MODE_ACTIVE);
        Chakra chakra = serverPlayer.getData(MainAttachment.CHAKRA);
        
        if (!modeActive.isActive()) {
            // Activating - check initial chakra cost
            if (chakra.getValue() < ACTIVATION_COST) {
                ModUtil.displayColoredMessage(serverPlayer, "msg.no_chakra", ChatFormatting.RED);
                return InteractionResultHolder.fail(stack);
            }
            
            // Consume activation chakra
            chakra.subValue(ACTIVATION_COST, serverPlayer);
            
            // Activate the mode
            modeActive.setActive(true, serverPlayer);
            
            // Apply initial effects
            applyEffects(serverPlayer);
            
            // No sound effect - owner will provide custom sounds later
            
            // NOTE: Activation particles now handled by client-side burst system in KibaLightningOverlay
            // which detects state change and renders intense lightning for 0.5 seconds
            
            serverPlayer.displayClientMessage(
                    Component.translatable("jutsu.activate", Component.translatable("item.narutoxboruto.lightning_chakra_mode")), true);
        } else {
            // Deactivating
            modeActive.setActive(false, serverPlayer);
            
            // Remove effects
            serverPlayer.removeEffect(MobEffects.DAMAGE_BOOST);
            serverPlayer.removeEffect(MobEffects.MOVEMENT_SPEED);
            
            // No sound effect - owner will provide custom sounds later
            
            serverPlayer.displayClientMessage(
                    Component.translatable("jutsu.deactivate", Component.translatable("item.narutoxboruto.lightning_chakra_mode")), true);
        }
        
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
    
    /**
     * Apply Strength II and Speed III effects to the player.
     * Particles are hidden since we have custom lightning aura.
     */
    public static void applyEffects(ServerPlayer player) {
        // Strength II (amplifier 1 = level 2)
        // Hide potion particles since we have lightning aura
        player.addEffect(new MobEffectInstance(
                MobEffects.DAMAGE_BOOST,
                EFFECT_DURATION,
                STRENGTH_AMPLIFIER,
                false, // Not ambient
                false, // Hide particles - we have lightning aura instead
                true   // Show icon
        ));
        
        // Speed III (amplifier 2 = level 3)
        player.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SPEED,
                EFFECT_DURATION,
                SPEED_AMPLIFIER,
                false,
                false, // Hide particles
                true
        ));
    }
    
    // Lightning Chakra Mode color - cyan/electric blue (matching reference image)
    private static final int LIGHTNING_AURA_COLOR = 0xE040E0FF; // Bright cyan-blue
    
    /**
     * Spawn lightning aura around the player using the LightningArcEntity system.
     * Creates a continuous shell of lightning arcs around the player body.
     */
    public static void spawnLightningParticles(ServerLevel level, ServerPlayer player, int count) {
        // Spawn more frequent, larger lightning arcs for visible aura effect
        // Increased count and longer duration for persistent aura
        LightningArcEntity.spawnArcsAroundEntity(level, player, count, LIGHTNING_AURA_COLOR, 6);
        
        // Also spawn some vertical arcs running up the body for "chakra cloak" effect
        spawnVerticalAuraArcs(level, player, 3);
    }
    
    /**
     * Spawn vertical lightning arcs running up/down the player body for aura effect.
     */
    private static void spawnVerticalAuraArcs(ServerLevel level, ServerPlayer player, int count) {
        double radius = player.getBbWidth() * 0.4;
        double height = player.getBbHeight();
        
        for (int i = 0; i < count; i++) {
            double angle = Math.random() * Math.PI * 2;
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;
            
            // Vertical arc from feet to head with slight curve
            double startY = player.getY() + Math.random() * 0.3;
            double endY = player.getY() + height * (0.7 + Math.random() * 0.3);
            double endX = x + (Math.random() - 0.5) * 0.3;
            double endZ = z + (Math.random() - 0.5) * 0.3;
            
            LightningArcEntity.spawnArcBetween(
                    level,
                    new Vec3(x, startY, z),
                    new Vec3(endX, endY, endZ),
                    LIGHTNING_AURA_COLOR,
                    5,
                    0.02f
            );
        }
    }
    
    /**
     * Called by StatEvents to drain chakra periodically while mode is active.
     * Also handles auto-deactivation and effect refresh.
     */
    public static void tickChakraDrain(ServerPlayer serverPlayer) {
        LightningChakraModeActive modeActive = serverPlayer.getData(MainAttachment.LIGHTNING_CHAKRA_MODE_ACTIVE);
        
        if (modeActive.isActive()) {
            // Check if player still has the jutsu in inventory (hotbar OR main inventory)
            // But NOT in jutsu storage - placing in jutsu storage should deactivate
            boolean hasJutsuInInventory = false;
            
            // Check all inventory slots (0-35: hotbar + main inventory)
            for (int i = 0; i < serverPlayer.getInventory().getContainerSize(); i++) {
                ItemStack stack = serverPlayer.getInventory().getItem(i);
                if (stack.getItem() instanceof LightningChakraMode) {
                    hasJutsuInInventory = true;
                    break;
                }
            }
            
            if (!hasJutsuInInventory) {
                // Auto-deactivate if not in player inventory (moved to jutsu storage or dropped)
                deactivate(serverPlayer);
                return;
            }
            
            Chakra chakra = serverPlayer.getData(MainAttachment.CHAKRA);
            
            if (chakra.getValue() >= DRAIN_COST) {
                // Drain chakra
                chakra.subValue(DRAIN_COST, serverPlayer);
                
                // Refresh effects
                applyEffects(serverPlayer);
            } else {
                // Not enough chakra - deactivate
                deactivate(serverPlayer);
                serverPlayer.displayClientMessage(Component.translatable("msg.no_chakra"), true);
            }
        }
    }
    
    /**
     * Called by StatEvents every second to spawn visual lightning aura.
     * NOTE: Visual effects now handled entirely by client-side KibaLightningOverlay
     * which renders lightning directly on the player model with proper positioning.
     */
    public static void tickVisualEffects(ServerPlayer serverPlayer) {
        // Visual effects now handled client-side - see KibaLightningOverlay
        // Keeping method signature for compatibility with StatEvents
    }
    
    /**
     * Deactivate the mode and remove effects.
     */
    private static void deactivate(ServerPlayer serverPlayer) {
        LightningChakraModeActive modeActive = serverPlayer.getData(MainAttachment.LIGHTNING_CHAKRA_MODE_ACTIVE);
        modeActive.setActive(false, serverPlayer);
        
        // Remove effects
        serverPlayer.removeEffect(MobEffects.DAMAGE_BOOST);
        serverPlayer.removeEffect(MobEffects.MOVEMENT_SPEED);
        
        // Restore stat-based speed effect if player has speed points
        net.narutoxboruto.attachments.stats.Speed speed = serverPlayer.getData(MainAttachment.SPEED);
        int speedLevel = speed.getValue() / 10;
        if (speedLevel > 0) {
            serverPlayer.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SPEED, -1, speedLevel - 1,
                    false, false, true));
        }
        
        // No sound effect - owner will provide custom sounds later
    }
    
    public static int getDrainCost() {
        return DRAIN_COST;
    }
    
    public static int getDrainIntervalSeconds() {
        return DRAIN_INTERVAL_SECONDS;
    }
    
    /**
     * Check if Lightning Chakra Mode is active for a player.
     * Used by StatEvents to avoid overwriting speed effects.
     */
    public static boolean isActive(ServerPlayer player) {
        return player.getData(MainAttachment.LIGHTNING_CHAKRA_MODE_ACTIVE).isActive();
    }
    
    @Override
    public boolean isFoil(ItemStack stack) {
        // Show enchantment glint when mode is active (client-side visual indicator)
        return net.narutoxboruto.client.PlayerData.isLightningChakraModeActive();
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("tooltip.lightning_chakra_mode.desc").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("tooltip.lightning_chakra_mode.effects").withStyle(ChatFormatting.YELLOW));
        tooltipComponents.add(Component.literal("  ").append(Component.translatable("tooltip.lightning_chakra_mode.strength")).withStyle(ChatFormatting.RED));
        tooltipComponents.add(Component.literal("  ").append(Component.translatable("tooltip.lightning_chakra_mode.speed")).withStyle(ChatFormatting.AQUA));
        tooltipComponents.add(Component.translatable("tooltip.chakra_cost", ACTIVATION_COST).withStyle(ChatFormatting.BLUE));
        tooltipComponents.add(Component.translatable("tooltip.chakra_drain", DRAIN_COST, DRAIN_INTERVAL_SECONDS).withStyle(ChatFormatting.DARK_BLUE));
        tooltipComponents.add(Component.translatable("tooltip.requires_release", "Lightning").withStyle(ChatFormatting.GOLD));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
