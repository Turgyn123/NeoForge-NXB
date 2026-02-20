package net.narutoxboruto.util;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.narutoxboruto.attachments.MainAttachment;

import javax.management.Attribute;
import java.util.*;

import static net.narutoxboruto.client.PlayerData.*;

public class ModUtil {
    public static final List<String> CLAN_LIST = Arrays.asList("fuma", "nara", "shiin", "shirogane", "uzumaki");

    public static final Map<String, Integer> CLAN_MAP = new HashMap<>();
    static {
        CLAN_MAP.put("fuma", 25);
        CLAN_MAP.put("nara", 10);
        CLAN_MAP.put("shiin", 4);
        CLAN_MAP.put("shirogane", 25);
        CLAN_MAP.put("uzumaki", 1);
        //CLAN_MAP.put("jugo", 10);
        //CLAN_MAP.put("hyuuga", 5);
        //CLAN_MAP.put("sarutobi", 15);
        // CLAN_MAP.put("senju", 1);
        // CLAN_MAP.put("tsuchigumo", 25);
        // CLAN_MAP.put("uchiha", 1);
        // CLAN_MAP.put("kaguya", 5);
        // CLAN_MAP.put("kurama", 15);
        // CLAN_MAP.put("ryu", 15);
        // CLAN_MAP.put("inuzuka", 25);
    }
    public static final List<String> RANK_LIST = Arrays.asList("civilian", "student", "genin", "chuunin", "jounin",
            "special_jounin", "anbu", "sage", "kage", "rogue");

    public static final List<String> AFF_LIST = Arrays.asList("cloud", "leaf", "mist", "rain", "sand", "sound",
            "stone");
    public static final List<String> STAT_LIST = Arrays.asList("taijutsu", "ninjutsu", "genjutsu", "kenjutsu",
            "kinjutsu", "medical", "senjutsu", "shurikenjutsu", "speed", "summoning");

    public static final Random RANDOM = new Random();

    public static final List<String> RELEASES_LIST = Arrays.asList("earth", "fire", "lightning", "water", "wind",
            "yang", "yin");

    /**
     * Get the chakra growth multiplier for a player based on their clan.
     * Uzumaki clan gets 3x multiplier (15 per point instead of 5).
     * @param serverPlayer The player to check
     * @return The multiplier (1 for normal, 3 for Uzumaki)
     */
    public static int getChakraGrowthMultiplier(ServerPlayer serverPlayer) {
        String clan = serverPlayer.getData(MainAttachment.CLAN).getValue();
        return "uzumaki".equals(clan) ? 3 : 1;
    }

    /**
     * Ensures current chakra doesn't exceed max chakra.
     * Call this after any operation that might change max chakra.
     * @param serverPlayer The player to cap chakra for
     */
    public static void capChakraToMax(ServerPlayer serverPlayer) {
        var maxChakra = serverPlayer.getData(MainAttachment.MAX_CHAKRA.get());
        var currentChakra = serverPlayer.getData(MainAttachment.CHAKRA.get());
        
        if (currentChakra.getValue() > maxChakra.getValue()) {
            currentChakra.setValue(maxChakra.getValue());
            currentChakra.syncValue(serverPlayer);
        }
    }

    /**
     * Recalculates max chakra when switching to/from Uzumaki clan.
     * @param serverPlayer The player whose chakra needs recalculation
     * @param leavingUzumaki True if leaving Uzumaki (reduce), false if joining (increase)
     */
    private static void recalculateMaxChakraForClanChange(ServerPlayer serverPlayer, boolean leavingUzumaki) {
        int ninjutsuValue = serverPlayer.getData(MainAttachment.NINJUTSU).getValue();
        
        // Calculate the chakra difference: Uzumaki gets 15 per point, normal gets 5 per point
        // So the difference is 10 per point
        int chakraDifference = ninjutsuValue * 10;
        
        if (chakraDifference > 0) {
            var maxChakra = serverPlayer.getData(MainAttachment.MAX_CHAKRA.get());
            var currentChakra = serverPlayer.getData(MainAttachment.CHAKRA.get());
            
            if (leavingUzumaki) {
                // Leaving Uzumaki: reduce max chakra
                maxChakra.subValue(chakraDifference, serverPlayer);
                // Adjust current chakra if it exceeds new max
                if (currentChakra.getValue() > maxChakra.getValue()) {
                    currentChakra.setValue(maxChakra.getValue());
                }
            } else {
                // Joining Uzumaki: increase max chakra
                maxChakra.addValue(chakraDifference, serverPlayer);
            }
        }
    }

    public static void giveClanStatBonuses(ServerPlayer serverPlayer) {
        String clan = serverPlayer.getData(MainAttachment.CLAN).getValue();

        switch (clan) {
            case "fuma" -> {
                serverPlayer.getData(MainAttachment.SHURIKENJUTSU).incrementValue(25, serverPlayer);
                // Give permanent Fuma Shuriken to Fuma clan members
                ClanItemHelper.giveFumaClanItem(serverPlayer);
            }
            case "nara" -> {
                serverPlayer.getData(MainAttachment.NINJUTSU).incrementValue(15, serverPlayer);
                serverPlayer.getData(MainAttachment.SHURIKENJUTSU).incrementValue(10, serverPlayer);
                serverPlayer.getData(MainAttachment.KINJUTSU).incrementValue(5, serverPlayer);
            }
            case "shiin" -> {
                serverPlayer.getData(MainAttachment.KINJUTSU).incrementValue(15, serverPlayer);
            }

            case "shirogane" -> {
                serverPlayer.getData(MainAttachment.SUMMONING).incrementValue(20, serverPlayer);
                serverPlayer.getData(MainAttachment.NINJUTSU).incrementValue(10, serverPlayer);
            }
            case "uzumaki" -> {
                serverPlayer.getData(MainAttachment.NINJUTSU).incrementValue(15, serverPlayer);
                serverPlayer.getData(MainAttachment.MEDICAL).incrementValue(10, serverPlayer);
                serverPlayer.getData(MainAttachment.KENJUTSU).incrementValue(5, serverPlayer);
                // After giving stat bonuses, recalculate max chakra with Uzumaki multiplier
                recalculateMaxChakraForClanChange(serverPlayer, false);
            }
        }
    }

    public static void removeClanStatBonuses(ServerPlayer serverPlayer) {
        String clan = serverPlayer.getData(MainAttachment.CLAN).getValue();

        // If leaving Uzumaki clan, recalculate max chakra with normal multiplier
        if ("uzumaki".equals(clan)) {
            recalculateMaxChakraForClanChange(serverPlayer, true);
        }
        
        // If leaving Fuma clan, remove the permanent Fuma Shuriken
        if ("fuma".equals(clan)) {
            ClanItemHelper.removeClanItems(serverPlayer);
        }

        switch (clan) {
            case "fuma" -> {
                serverPlayer.getData(MainAttachment.SHURIKENJUTSU).subValue(25, serverPlayer);
            }
            case "nara" -> {
                serverPlayer.getData(MainAttachment.NINJUTSU).subValue(15, serverPlayer);
                serverPlayer.getData(MainAttachment.SHURIKENJUTSU).subValue(10, serverPlayer);
                serverPlayer.getData(MainAttachment.KINJUTSU).subValue(5, serverPlayer);
            }
            case "shiin" -> {
                serverPlayer.getData(MainAttachment.KINJUTSU).subValue(15, serverPlayer);
            }
            case "shirogane" -> {
                serverPlayer.getData(MainAttachment.SUMMONING).subValue(20, serverPlayer);
                serverPlayer.getData(MainAttachment.NINJUTSU).subValue(10, serverPlayer);
            }
            case "uzumaki" -> {
                serverPlayer.getData(MainAttachment.NINJUTSU).subValue(15, serverPlayer);
                serverPlayer.getData(MainAttachment.MEDICAL).subValue(10, serverPlayer);
                serverPlayer.getData(MainAttachment.KENJUTSU).subValue(5, serverPlayer);
            }
        }
    }

    public static int getPlayerStatistics(ServerPlayer serverPlayer, ResourceLocation stat) {
        return serverPlayer.getStats().getValue(Stats.CUSTOM.get(stat));
    }

    public static String concatAndFormat(String pList, String value) {
        return (pList + (pList.isEmpty() ? "" : ", ") + value).toLowerCase();
    }

    public static List<String> getArrayFrom(String s) {
        return List.of(s.replace(" ", "").split(","));
    }

    public static void displayTranslatableMessage(ServerPlayer serverPlayer, String msg, String msg2, String s, boolean b) {
        Component message = Component.translatable("msg.narutoxboruto." + msg,
                b ? s : Component.translatable(msg2 + ".narutoxboruto." + s));
        serverPlayer.displayClientMessage(message, false);
    }

    public static void displayTranslatableMessage(ServerPlayer serverPlayer, String msg, String s, boolean b) {
        displayTranslatableMessage(serverPlayer, msg, msg, s, b);
    }

    public static String getRandomIndex(List<String> array) {
        return array.get(RANDOM.nextInt(array.size()));
    }

    public static void displayColoredMessage(Player player, String pKey, String pArg, ChatFormatting color) {
        player.displayClientMessage(Component.translatable(pKey, Component.translatable(pArg)).withStyle(color), true);
    }

    public static void displayColoredMessage(Player player, String s, ChatFormatting chatFormatting) {
        displayColoredMessage(player, s, "", chatFormatting);
    }

   public static void msgPlayerInfo(ServerPlayer serverPlayer) {
       displayTranslatableMessage(serverPlayer, "affiliation", getAffiliation(), false);
       displayTranslatableMessage(serverPlayer, "clan", getClan(), false);
       displayTranslatableMessage(serverPlayer, "rank", getRank(), false);
   }

    public static double getEntitySpeed(Entity entity) {
        double motionX = entity.getX() - entity.xo;
        double motionY = entity.getY() - entity.yo;
        double motionZ = entity.getZ() - entity.zo;
        return Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
    }
}
