package net.narutoxboruto.attachments.stats;

import com.mojang.serialization.Codec;
import net.minecraft.server.level.ServerPlayer;
import net.narutoxboruto.attachments.MainAttachment;
import net.narutoxboruto.attachments.info.Chakra;
import net.narutoxboruto.attachments.info.MaxChakra;
import net.narutoxboruto.networking.ModPacketHandler;
import net.narutoxboruto.networking.stats.SyncNinjutsu;
import net.narutoxboruto.util.ModUtil;

public class Ninjutsu {

    private final String id;
    protected int maxValue;
    protected int value;

    public static final Codec<Ninjutsu> CODEC = Codec.INT.xmap(value -> {Ninjutsu ninjutsu = new Ninjutsu();ninjutsu.value = value;return ninjutsu;}, Ninjutsu::getValue);

    public Ninjutsu() {
        this.id = "ninjutsu";
        this.maxValue = 500;
        this.value = 0;
    }

    public int getValue() {
        return value;
    }

    public void syncValue(ServerPlayer serverPlayer) {
        ModPacketHandler.sendToPlayer(new SyncNinjutsu(getValue()), serverPlayer);
    }

    public void incrementValue(int add, ServerPlayer serverPlayer) {
        int oldValue = this.value;
        this.value = Math.min(value + add, maxValue);

        // Calculate how many new points were actually added
        int actualAdd = this.value - oldValue;

        // For every 1 point in ninjutsu, add 5 to max chakra (15 for Uzumaki clan)
        if (actualAdd > 0) {
            MaxChakra maxChakra = serverPlayer.getData(MainAttachment.MAX_CHAKRA.get());
            int multiplier = ModUtil.getChakraGrowthMultiplier(serverPlayer);
            maxChakra.addValue(actualAdd * 5 * multiplier, serverPlayer);
        }
        this.syncValue(serverPlayer);
    }

    public void addValue(int add, ServerPlayer serverPlayer) {
        int oldValue = this.value;
        this.value = add;

        // Calculate how many new points were actually added
        int actualAdd = this.value - oldValue;

        // For every 1 point in ninjutsu, add 5 to max chakra (15 for Uzumaki clan)
        if (actualAdd > 0) {
            MaxChakra maxChakra = serverPlayer.getData(MainAttachment.MAX_CHAKRA.get());
            int multiplier = ModUtil.getChakraGrowthMultiplier(serverPlayer);
            maxChakra.addValue(actualAdd * 5 * multiplier, serverPlayer);
        }
        this.syncValue(serverPlayer);
    }

    public void setValue(int value, ServerPlayer serverPlayer) {
        int oldValue = this.value;
        this.value = Math.min(value, maxValue);

        // Calculate how many new points were actually added
        int actualAdd = this.value - oldValue;

        // For every 1 point in ninjutsu, add 5 to max chakra (15 for Uzumaki clan)
        if (actualAdd > 0) {
            MaxChakra maxChakra = serverPlayer.getData(MainAttachment.MAX_CHAKRA.get());
            int multiplier = ModUtil.getChakraGrowthMultiplier(serverPlayer);
            maxChakra.addValue(actualAdd * 5 * multiplier, serverPlayer);
        }
        this.syncValue(serverPlayer);
    }

    public void setValue(int value) {
        this.value = Math.min(value, maxValue);
    }

    public void subValue(int sub, ServerPlayer serverPlayer) {
        int oldValue = this.value;
        this.value = Math.max(value - sub, 0);

        // Calculate how many points were removed
        int pointsRemoved = oldValue - this.value;

        // Reduce max chakra when ninjutsu points are lost (accounts for clan multiplier)
        if (pointsRemoved > 0) {
            MaxChakra maxChakra = serverPlayer.getData(MainAttachment.MAX_CHAKRA.get());
            int multiplier = ModUtil.getChakraGrowthMultiplier(serverPlayer);
            maxChakra.subValue(pointsRemoved * 5 * multiplier, serverPlayer);
        }
    }

    public void copyFrom(Ninjutsu source, ServerPlayer serverPlayer) {
        int oldValue = this.value;
        this.value = source.getValue();

        // Calculate how many new points were actually added
        int actualAdd = this.value - oldValue;

        // For every 1 point in ninjutsu, add 5 to max chakra (15 for Uzumaki clan)
        if (actualAdd > 0) {
            MaxChakra maxChakra = serverPlayer.getData(MainAttachment.MAX_CHAKRA.get());
            int multiplier = ModUtil.getChakraGrowthMultiplier(serverPlayer);
            maxChakra.addValue(actualAdd * 5 * multiplier, serverPlayer);
        }

        this.syncValue(serverPlayer);
    }

    public void resetValue(ServerPlayer serverPlayer) {
        int oldValue = this.value;
        this.value = 0;

        // Calculate how many points were removed (negative value)
        int pointsRemoved = this.value - oldValue;

        // If points were removed, we might want to reduce max chakra
        if (pointsRemoved < 0) {
            MaxChakra maxChakra = serverPlayer.getData(MainAttachment.MAX_CHAKRA.get());
            int multiplier = ModUtil.getChakraGrowthMultiplier(serverPlayer);
            maxChakra.subValue(Math.abs(pointsRemoved) * 5 * multiplier, serverPlayer);
            adjustCurrentChakra(serverPlayer);

        }

        this.syncValue(serverPlayer);
    }

    /**
     * Adjusts current chakra if it exceeds the new max chakra
     */
    private void adjustCurrentChakra(ServerPlayer serverPlayer) {
        Chakra chakra = serverPlayer.getData(MainAttachment.CHAKRA.get());
        MaxChakra maxChakra = serverPlayer.getData(MainAttachment.MAX_CHAKRA.get());

        // If current chakra exceeds new max chakra, reduce it to max
        if (chakra.getValue() > maxChakra.getValue()) {
            chakra.setValue(maxChakra.getValue());
        }
    }
}
