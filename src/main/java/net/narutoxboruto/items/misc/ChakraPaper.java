package net.narutoxboruto.items.misc;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.narutoxboruto.attachments.MainAttachment;
import net.narutoxboruto.jutsu.JutsuWheel;
import net.narutoxboruto.util.JutsuGrantHelper;
import net.narutoxboruto.util.ModUtil;

import static net.narutoxboruto.util.ModUtil.*;

public class ChakraPaper extends Item {
    public ChakraPaper(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        if (!pLevel.isClientSide() && pPlayer instanceof ServerPlayer serverPlayer) {
            ItemStack stack = serverPlayer.getItemInHand(pUsedHand);

            // Get attachments
            var releaseListAttachment = serverPlayer.getData(MainAttachment.RELEASE_LIST);

            if (releaseListAttachment.getValue().isEmpty()) {
                String newReleaseList = "";
                int l = 0;
                while (l <= RANDOM.nextInt(3)) {
                    String release = getRandomIndex(RELEASES_LIST);
                    if (!newReleaseList.contains(release)) {
                        newReleaseList = ModUtil.concatAndFormat(newReleaseList, release);
                        l++;
                    }
                }
                releaseListAttachment.concatList(newReleaseList, serverPlayer);
                
                // Auto-populate jutsu wheel with jutsus for the new nature(s)
                JutsuWheel wheel = serverPlayer.getData(MainAttachment.JUTSU_WHEEL);
                wheel.autoPopulate(releaseListAttachment.getValue());
                wheel.syncValue(serverPlayer);
                
                // Grant corresponding jutsu items to player's storage
                JutsuGrantHelper.grantJutsusForReleases(serverPlayer, newReleaseList);
                
                String s = newReleaseList.contains(",") ? "s" : "";
                displayTranslatableMessage(serverPlayer, "release" + s, releaseListAttachment.getValue(), true);

              // serverPlayer.addItem(ModUtil.getSelectedReleaseItem().getDefaultInstance());
                this.consume(stack, serverPlayer);
            }
            return InteractionResultHolder.success(stack);
        }
        return super.use(pLevel, pPlayer, pUsedHand);
    }

    public ItemStack consume(ItemStack stack, ServerPlayer serverPlayer) {
        if (!serverPlayer.getAbilities().instabuild) {
            stack.shrink(1);
        }
        CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
        serverPlayer.awardStat(Stats.ITEM_USED.get(this));
        serverPlayer.gameEvent(GameEvent.DRINK);
        return stack;
    }
}
