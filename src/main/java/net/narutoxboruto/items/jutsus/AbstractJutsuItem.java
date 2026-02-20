package net.narutoxboruto.items.jutsus;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.narutoxboruto.attachments.MainAttachment;
import net.narutoxboruto.attachments.info.Chakra;
import net.narutoxboruto.attachments.info.ReleaseList;
import net.narutoxboruto.util.ModUtil;

import java.util.List;

/**
 * Abstract base class for all jutsu items.
 * 
 * Features:
 * - Cannot be dropped (stays in inventory)
 * - Cannot be placed in regular containers (only jutsu storage)
 * - Has chakra cost and cooldown
 * - Requires specific nature release
 * - Checks chakra before use
 */
public abstract class AbstractJutsuItem extends Item {
    
    public AbstractJutsuItem(Properties properties) {
        // Jutsu items don't stack
        super(properties.stacksTo(1));
    }
    
    /**
     * Get the chakra cost for this jutsu.
     */
    public abstract int getChakraCost();
    
    /**
     * Get the cooldown in ticks (20 ticks = 1 second).
     */
    public abstract int getCooldownTicks();
    
    /**
     * Get the required nature release (e.g., "fire", "earth").
     * Return null or empty string if no specific release is required.
     */
    public abstract String getRequiredRelease();
    
    /**
     * Execute the jutsu effect.
     * @return true if the jutsu was successfully cast
     */
    protected abstract boolean executeJutsu(ServerPlayer player, Level level);
    
    /**
     * Get the jutsu's display name for the tooltip.
     */
    public abstract String getJutsuName();
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        // Client side - just pass through, let server handle everything
        if (level.isClientSide()) {
            return InteractionResultHolder.pass(stack);
        }
        
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.fail(stack);
        }
        
        // Check cooldown first - silently fail, don't spam message
        if (serverPlayer.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        
        // Check if player has the required nature release
        String requiredRelease = getRequiredRelease();
        if (requiredRelease != null && !requiredRelease.isEmpty()) {
            ReleaseList releaseList = serverPlayer.getData(MainAttachment.RELEASE_LIST);
            if (!releaseList.getValue().toLowerCase().contains(requiredRelease.toLowerCase())) {
                ModUtil.displayColoredMessage(serverPlayer, "msg.no_release", ChatFormatting.RED);
                return InteractionResultHolder.fail(stack);
            }
        }
        
        // Check chakra
        Chakra chakra = serverPlayer.getData(MainAttachment.CHAKRA);
        if (chakra.getValue() < getChakraCost()) {
            ModUtil.displayColoredMessage(serverPlayer, "msg.no_chakra", ChatFormatting.RED);
            return InteractionResultHolder.fail(stack);
        }
        
        // Consume chakra first
        chakra.subValue(getChakraCost(), serverPlayer);
        
        // Execute the jutsu
        boolean success = executeJutsu(serverPlayer, level);
        
        if (success) {
            // Apply cooldown
            serverPlayer.getCooldowns().addCooldown(this, getCooldownTicks());
            
            // Play success sound (subclasses can override to silence)
            playCastSound(level, player);
            
            return InteractionResultHolder.consume(stack);
        } else {
            // Refund chakra if jutsu failed
            chakra.addValue(getChakraCost(), serverPlayer);
            return InteractionResultHolder.fail(stack);
        }
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        
        // Show chakra cost
        tooltip.add(Component.literal("Chakra Cost: " + getChakraCost())
            .withStyle(ChatFormatting.BLUE));
        
        // Show cooldown
        float cooldownSeconds = getCooldownTicks() / 20.0F;
        tooltip.add(Component.literal("Cooldown: " + String.format("%.1f", cooldownSeconds) + "s")
            .withStyle(ChatFormatting.GRAY));
        
        // Show required release
        String release = getRequiredRelease();
        if (release != null && !release.isEmpty()) {
            tooltip.add(Component.literal("Requires: " + capitalize(release) + " Release")
                .withStyle(ChatFormatting.GOLD));
        }
    }
    
    /**
     * Prevent dropping jutsu items.
     * This is enforced via events, but we also mark them here.
     */
    public boolean isJutsuItem() {
        return true;
    }
    
    /**
     * Play a sound when the jutsu is successfully cast.
     * Override and leave empty to silence for specific jutsus.
     */
    protected void playCastSound(Level level, Player player) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.5F, 1.0F);
    }
    
    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
}
