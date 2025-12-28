package net.narutoxboruto.attachments.info;

import com.mojang.serialization.Codec;
import net.minecraft.server.level.ServerPlayer;
import net.narutoxboruto.attachments.MainAttachment;
import net.narutoxboruto.networking.ModPacketHandler;
import net.narutoxboruto.networking.info.SyncChakra;


public class Chakra {
    private int value;

    public static final Codec<Chakra> CODEC = Codec.INT.xmap(Chakra::new, Chakra::getValue);

    public Chakra(int value) {
        this.value = value;
    }

    public Chakra() {
        this.value = 0;
    }

    public int getValue() {
        return value;
    }


    public void addValue(int add, ServerPlayer serverPlayer) {
        MaxChakra maxChakra = serverPlayer.getData(MainAttachment.MAX_CHAKRA.get());
        this.value = Math.min(this.value + add, maxChakra.getValue());
        this.syncValue(serverPlayer);
    }

    public void subValue(int sub, ServerPlayer serverPlayer) {
        // Chakra is consumed in all game modes including creative
        this.value = Math.max(this.value - sub, 0);
        this.syncValue(serverPlayer);
    }

    public void setValue(int value) {
        this.value = value;
    }

    public void syncValue(ServerPlayer serverPlayer) {
        serverPlayer.setData(MainAttachment.CHAKRA.get(), this);
        ModPacketHandler.sendToPlayer(new SyncChakra(getValue()), serverPlayer);
    }


    public void reset(ServerPlayer serverPlayer) {
        MaxChakra maxChakra = serverPlayer.getData(MainAttachment.MAX_CHAKRA.get());
        this.value = maxChakra.getValue() / 2;
        this.syncValue(serverPlayer);
    }

    public void replenish(ServerPlayer serverPlayer) {
        MaxChakra maxChakra = serverPlayer.getData(MainAttachment.MAX_CHAKRA.get());
        this.value = maxChakra.getValue();
        this.syncValue(serverPlayer);
    }
}

