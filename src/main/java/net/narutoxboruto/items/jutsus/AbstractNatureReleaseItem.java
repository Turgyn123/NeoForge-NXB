package net.narutoxboruto.items.jutsus;

import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.narutoxboruto.attachments.MainAttachment;
import net.narutoxboruto.attachments.info.Chakra;
import net.narutoxboruto.attachments.info.ReleaseList;
import net.narutoxboruto.items.PreventSlow;
import net.narutoxboruto.util.ModUtil;

/**
 * Abstract base class for all nature release jutsu items.
 * Handles chakra consumption, nature affinity verification, and cooldowns.
 */
public abstract class AbstractNatureReleaseItem extends Item implements PreventSlow {

    public AbstractNatureReleaseItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack pStack) {
        return UseAnim.BOW;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.fail(stack);
        }

        // Check if player has the required nature release
        String requiredRelease = getRequiredRelease();
        if (requiredRelease != null && !requiredRelease.isEmpty()) {
            ReleaseList releaseList = serverPlayer.getData(MainAttachment.RELEASE_LIST.get());
            if (!releaseList.getValue().contains(requiredRelease)) {
                ModUtil.displayColoredMessage(serverPlayer, "msg.no_release", ChatFormatting.RED);
                return InteractionResultHolder.fail(stack);
            }
        }

        // Check chakra
        Chakra chakra = serverPlayer.getData(MainAttachment.CHAKRA.get());
        int chakraCost = getJutsuChakraCost(serverPlayer);

        if (chakraCost <= 0) {
            return InteractionResultHolder.fail(stack);
        }

        if (chakra.getValue() < chakraCost) {
            ModUtil.displayColoredMessage(serverPlayer, "msg.no_chakra", ChatFormatting.RED);
            return InteractionResultHolder.fail(stack);
        }

        // Attempt to cast the jutsu
        if (castJutsu(serverPlayer, level)) {
            // Only consume chakra and apply cooldown if jutsu was successful
            chakra.subValue(chakraCost, serverPlayer);
            serverPlayer.awardStat(Stats.ITEM_USED.get(this));
            serverPlayer.getCooldowns().addCooldown(this, getCooldownTicks());

            // Display jutsu name
            String jutsuName = getSelectedJutsu(serverPlayer);
            if (jutsuName != null && !jutsuName.isEmpty()) {
                ModUtil.displayColoredMessage(serverPlayer, "jutsu." + jutsuName, ChatFormatting.YELLOW);
            }

            return InteractionResultHolder.consume(stack);
        }

        return InteractionResultHolder.fail(stack);
    }

    /**
     * Cast the jutsu. Override in subclasses to implement specific behavior.
     * @param serverPlayer The player casting the jutsu
     * @param level The level/world
     * @return true if the jutsu was successfully cast, false otherwise
     */
    protected abstract boolean castJutsu(ServerPlayer serverPlayer, Level level);

    /**
     * Get the chakra cost for this jutsu.
     * @param serverPlayer The player casting the jutsu
     * @return The chakra cost
     */
    protected abstract int getJutsuChakraCost(ServerPlayer serverPlayer);

    /**
     * Get the required nature release type (e.g., "fire", "earth", "water", "wind", "lightning").
     * Return null or empty string if no release is required.
     * @return The required release type
     */
    protected abstract String getRequiredRelease();

    /**
     * Get the translation key for this jutsu's name.
     * @param serverPlayer The player casting the jutsu
     * @return The jutsu name key
     */
    public String getSelectedJutsu(ServerPlayer serverPlayer) {
        return "";
    }

    /**
     * Get the cooldown duration in ticks (20 ticks = 1 second).
     * Override in subclasses for custom cooldowns.
     * @return Cooldown in ticks
     */
    protected int getCooldownTicks() {
        return 20; // Default: 1 second cooldown for testing
    }
}
