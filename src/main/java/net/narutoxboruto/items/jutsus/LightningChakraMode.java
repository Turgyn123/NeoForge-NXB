package net.narutoxboruto.items.jutsus;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.narutoxboruto.particles.ModParticles;
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
    
    // Effect durations - long enough to persist between ticks (7 seconds, refreshed every 5)
    private static final int EFFECT_DURATION = 140; // 7 seconds in ticks
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
            
            // Play activation sound
            level.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.5f, 1.5f);
            
            // Spawn activation burst particles
            if (level instanceof ServerLevel serverLevel) {
                spawnLightningParticles(serverLevel, serverPlayer, 30);
            }
            
            serverPlayer.displayClientMessage(
                    Component.translatable("jutsu.activate", Component.translatable("item.narutoxboruto.lightning_chakra_mode")), true);
        } else {
            // Deactivating
            modeActive.setActive(false, serverPlayer);
            
            // Remove effects
            serverPlayer.removeEffect(MobEffects.DAMAGE_BOOST);
            serverPlayer.removeEffect(MobEffects.MOVEMENT_SPEED);
            
            // Play deactivation sound
            level.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                    SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.5f, 1.2f);
            
            serverPlayer.displayClientMessage(
                    Component.translatable("jutsu.deactivate", Component.translatable("item.narutoxboruto.lightning_chakra_mode")), true);
        }
        
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
    
    /**
     * Apply Strength II and Speed III effects to the player.
     */
    public static void applyEffects(ServerPlayer player) {
        // Strength II (amplifier 1 = level 2)
        player.addEffect(new MobEffectInstance(
                MobEffects.DAMAGE_BOOST,
                EFFECT_DURATION,
                STRENGTH_AMPLIFIER,
                false, // Not ambient
                true,  // Show particles
                true   // Show icon
        ));
        
        // Speed III (amplifier 2 = level 3)
        player.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SPEED,
                EFFECT_DURATION,
                SPEED_AMPLIFIER,
                false,
                true,
                true
        ));
    }
    
    /**
     * Spawn lightning particles around the player.
     * Uses the same particle system as Kiba.
     */
    public static void spawnLightningParticles(ServerLevel level, ServerPlayer player, int count) {
        double radius = 0.8;
        double height = player.getBbHeight();
        
        for (int i = 0; i < count; i++) {
            // Random position around the player
            double angle = Math.random() * Math.PI * 2;
            double yOffset = Math.random() * height;
            double r = radius * (0.5 + Math.random() * 0.5);
            
            double x = player.getX() + Math.cos(angle) * r;
            double y = player.getY() + yOffset;
            double z = player.getZ() + Math.sin(angle) * r;
            
            // Random velocity for dynamic effect
            double vx = (Math.random() - 0.5) * 0.1;
            double vy = (Math.random() - 0.3) * 0.1; // Slight upward bias
            double vz = (Math.random() - 0.5) * 0.1;
            
            level.sendParticles(
                    ModParticles.LIGHTNING_SPARKS.get(),
                    x, y, z,
                    1,
                    vx, vy, vz,
                    0.02D
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
            // Check if player still has the jutsu in hotbar
            boolean hasJutsuInHotbar = false;
            for (int i = 0; i < 9; i++) {
                ItemStack stack = serverPlayer.getInventory().getItem(i);
                if (stack.getItem() instanceof LightningChakraMode) {
                    hasJutsuInHotbar = true;
                    break;
                }
            }
            
            if (!hasJutsuInHotbar) {
                // Auto-deactivate if not in hotbar
                deactivate(serverPlayer);
                return;
            }
            
            Chakra chakra = serverPlayer.getData(MainAttachment.CHAKRA);
            
            if (chakra.getValue() >= DRAIN_COST) {
                // Drain chakra
                chakra.subValue(DRAIN_COST, serverPlayer);
                
                // Refresh effects
                applyEffects(serverPlayer);
                
                // Spawn ambient particles
                if (serverPlayer.level() instanceof ServerLevel serverLevel) {
                    spawnLightningParticles(serverLevel, serverPlayer, 8);
                }
            } else {
                // Not enough chakra - deactivate
                deactivate(serverPlayer);
                serverPlayer.displayClientMessage(Component.translatable("msg.no_chakra"), true);
            }
        }
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
        
        // Play deactivation sound
        serverPlayer.level().playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.5f, 1.2f);
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
