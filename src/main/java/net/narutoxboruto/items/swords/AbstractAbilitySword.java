package net.narutoxboruto.items.swords;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.narutoxboruto.attachments.MainAttachment;
import net.narutoxboruto.attachments.info.Chakra;

public class AbstractAbilitySword extends SwordItem implements Vanishable {
    public boolean isActive = false;
    protected int cooldown;

    public AbstractAbilitySword(Tier tier, Item.Properties pProperties) {
        super(tier, pProperties);
        this.cooldown = (int) (20 / (4 + tier.getSpeed()));
    }
    public int getChakraCost() {
        return 1;
    }

    public void toggleAbility(Player pPlayer) {
        if (!pPlayer.level().isClientSide) {
            this.isActive = !this.isActive;
            String s = this.isActive ? "activate" : "deactivate";
            pPlayer.displayClientMessage(Component.translatable("sword_ability." + s, this.getDescription()), true);
        }
    }

    protected void doSpecialAbility(LivingEntity pTarget, ServerPlayer serverPlayer) {
    }

    @Override
    public boolean hurtEnemy(ItemStack pStack, LivingEntity pTarget, LivingEntity pAttacker) {
        pStack.hurtAndBreak(1, pAttacker, LivingEntity.getSlotForHand(pAttacker.getUsedItemHand()));
        if (pAttacker instanceof ServerPlayer serverPlayer && this.isActive && !serverPlayer.level().isClientSide()) {
            ItemCooldowns cooldowns = serverPlayer.getCooldowns();
            Chakra chakra = serverPlayer.getData(MainAttachment.CHAKRA);
            if (!cooldowns.isOnCooldown(pStack.getItem()) && chakra.getValue() >= getChakraCost()) {
                // Consume chakra FIRST, then execute ability
                chakra.subValue(getChakraCost(), serverPlayer);
                doSpecialAbility(pTarget, serverPlayer);
                // Only add cooldown if ability was successfully used
                cooldowns.addCooldown(this, cooldown);
            }
        }
        return true;
    }
}
