package net.narutoxboruto.items.misc;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.network.chat.Component;
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

import static net.narutoxboruto.util.ModUtil.*;

public class ClanReroll extends Item {

    public ClanReroll(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        if (pPlayer instanceof ServerPlayer serverPlayer) {
            ItemStack stack = serverPlayer.getItemInHand(pUsedHand);

            // Get the clan attachment
            var clanAttachment = serverPlayer.getData(MainAttachment.CLAN);

            removeClanStatBonuses(serverPlayer);

            String currentClan = clanAttachment.getValue();
            String newClan = getRandomIndex(CLAN_LIST);

            // Ensure we get a different clan
            while (newClan.equals(currentClan)) {
                newClan = getRandomIndex(CLAN_LIST);
            }

            // Set the new clan value
            clanAttachment.setValue(newClan, serverPlayer);
            Component message = Component.translatable("clan_reroll.success",
                    Component.translatable("clan." + newClan));
            serverPlayer.displayClientMessage(message, false);

            giveClanStatBonuses(serverPlayer);

            // Ensure current chakra doesn't exceed new max chakra
            capChakraToMax(serverPlayer);

            // SYNC ALL STATS TO CLIENT
            syncAllStatsToClient(serverPlayer);

            this.consume(stack, serverPlayer);
            return InteractionResultHolder.success(stack);
        }
        return super.use(pLevel, pPlayer, pUsedHand);
    }

    // Add this method to sync all stat changes to the client
    private void syncAllStatsToClient(ServerPlayer serverPlayer) {
        // Sync all relevant stats to client
        serverPlayer.getData(MainAttachment.SHURIKENJUTSU).syncValue(serverPlayer);
        serverPlayer.getData(MainAttachment.NINJUTSU).syncValue(serverPlayer);
        serverPlayer.getData(MainAttachment.KINJUTSU).syncValue(serverPlayer);
        serverPlayer.getData(MainAttachment.SUMMONING).syncValue(serverPlayer);
        serverPlayer.getData(MainAttachment.MEDICAL).syncValue(serverPlayer);
        serverPlayer.getData(MainAttachment.KENJUTSU).syncValue(serverPlayer);
        serverPlayer.getData(MainAttachment.SPEED).syncValue(serverPlayer);
        serverPlayer.getData(MainAttachment.TAIJUTSU).syncValue(serverPlayer);
        serverPlayer.getData(MainAttachment.SENJUTSU).syncValue(serverPlayer);
        serverPlayer.getData(MainAttachment.GENJUTSU).syncValue(serverPlayer);




        // Also sync the clan itself
        serverPlayer.getData(MainAttachment.CLAN).syncValue(serverPlayer);
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
