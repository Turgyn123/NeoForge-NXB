package net.narutoxboruto.items.misc;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.narutoxboruto.attachments.MainAttachment;
import net.narutoxboruto.attachments.info.ReleaseList;
import net.narutoxboruto.util.JutsuGrantHelper;
import net.narutoxboruto.util.ModUtil;

public class ReleaseDnaBottleItem extends Item {
    private final String natureType;

    public ReleaseDnaBottleItem(Properties properties, String natureType) {
        super(properties);
        this.natureType = natureType;
    }

    protected void implementNature(ServerPlayer serverPlayer) {
        ReleaseList releaseList = serverPlayer.getData(MainAttachment.RELEASE_LIST);

        if (!releaseList.getValue().contains(natureType)) {
            if (serverPlayer.getAbilities().instabuild || serverPlayer.getRandom().nextInt(3) == 0) {
                releaseList.concatList(natureType, serverPlayer);
                
                // Grant corresponding jutsu item to player's jutsu storage
                JutsuGrantHelper.grantJutsuForRelease(serverPlayer, natureType);
                
                ModUtil.displayColoredMessage(serverPlayer, "dna_bottle.release.success",
                        "release." + natureType, ChatFormatting.GREEN);
                serverPlayer.getCooldowns().addCooldown(this, 20);
            }
            else {
                ModUtil.displayColoredMessage(serverPlayer, "dna_bottle.release.fail",
                        "release." + natureType, ChatFormatting.RED);
            }
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack pStack, Level pLevel, LivingEntity pLivingEntity) {
        if (pLivingEntity instanceof ServerPlayer serverPlayer) {
            this.implementNature(serverPlayer);
            return this.consume(pStack, pLevel, serverPlayer);
        }
        return super.finishUsingItem(pStack, pLevel, pLivingEntity);
    }

    public ItemStack consume(ItemStack stack, Level pLevel, ServerPlayer serverPlayer) {
        pLevel.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(), SoundEvents.GENERIC_DRINK,
                SoundSource.NEUTRAL, 1.0F, 1.0F + (pLevel.random.nextFloat() - pLevel.random.nextFloat()) * 0.4F);
        if (!serverPlayer.getAbilities().instabuild) {
            stack.shrink(1);
        }
        CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
        serverPlayer.awardStat(Stats.ITEM_USED.get(this));
        serverPlayer.gameEvent(GameEvent.DRINK);
        return stack;
    }

    public int getUseDuration(ItemStack pStack) {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack pStack) {
        return UseAnim.DRINK;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player player, InteractionHand pHand) {
        if (!pLevel.isClientSide) {
            ReleaseList playerReleaseList = player.getData(MainAttachment.RELEASE_LIST);
            if (!player.getCooldowns().isOnCooldown(this) && !playerReleaseList.getValue().contains(natureType)) {
                return ItemUtils.startUsingInstantly(pLevel, player, pHand);
            }
            else {
                ModUtil.displayColoredMessage(player, "dna_bottle.already_implemented",
                        "release." + natureType, ChatFormatting.YELLOW);
            }
        }
        return super.use(pLevel, player, pHand);
    }
}
