package net.narutoxboruto.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.narutoxboruto.attachments.MainAttachment;
import net.narutoxboruto.attachments.info.Clan;
import net.narutoxboruto.command.argument.ClanArgument;
import net.narutoxboruto.util.ModUtil;

import java.util.Collection;

import static net.narutoxboruto.events.AttachmentEvents.syncAllStatsToClient;
import static net.narutoxboruto.util.ModUtil.capChakraToMax;
import static net.narutoxboruto.util.ModUtil.giveClanStatBonuses;
import static net.narutoxboruto.util.ModUtil.removeClanStatBonuses;

public class ClanCommand {
    public static int displayClan(CommandSourceStack pSource, ServerPlayer serverPlayer) {
        Clan clan = serverPlayer.getData(MainAttachment.CLAN);
        Component message = Component.translatable("command.clan.display", serverPlayer.getDisplayName(),
                Component.translatable("clan." + clan.getValue()));
        pSource.sendSuccess(() -> message, false);
        return 1;
    }

    public static int setClan(CommandSourceStack pSource, Collection<? extends ServerPlayer> pTargets, String pClan) {
        if (!ModUtil.CLAN_LIST.contains(pClan)) {
            pSource.sendFailure(Component.translatable("argument.clan.invalid", pClan));
            return 0;
        }

        for (ServerPlayer player : pTargets) {
            // Remove current clan bonuses first
            removeClanStatBonuses(player);

            // Set the new clan
            Clan clan = player.getData(MainAttachment.CLAN);
            clan.setValue(pClan, player);

            // Apply new clan bonuses
            giveClanStatBonuses(player);

            // Ensure current chakra doesn't exceed new max chakra
            capChakraToMax(player);

            // Sync all stats to client
            syncAllStatsToClient(player);
        }

        if (pTargets.size() == 1) {
            pSource.sendSuccess(() -> Component.translatable("command.clan.set.single",
                    pTargets.iterator().next().getDisplayName(),
                    Component.translatable("clan." + pClan)), true);
        } else {
            pSource.sendSuccess(() -> Component.translatable("command.clan.set.multiple", pTargets.size(),
                    Component.translatable("clan." + pClan)), true);
        }
        return 1;
    }

    public static void register(CommandDispatcher<CommandSourceStack> pDispatcher) {
        pDispatcher.register(Commands.literal("clan").requires((r) -> r.hasPermission(0))
                .then(Commands.literal("set").requires((r) -> r.hasPermission(2))
                        .then(Commands.argument("target", EntityArgument.players())
                                .then(Commands.argument("clan", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(ModUtil.CLAN_LIST, builder))
                                        .executes((r) -> setClan(r.getSource(),
                                                EntityArgument.getPlayers(r, "target"),
                                                StringArgumentType.getString(r, "clan")))))));
    }
}
