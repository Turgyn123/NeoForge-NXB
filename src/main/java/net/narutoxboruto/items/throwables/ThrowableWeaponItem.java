package net.narutoxboruto.items.throwables;

import net.minecraft.client.Minecraft;
import net.minecraft.server.Main;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.narutoxboruto.attachments.MainAttachment;
import net.narutoxboruto.attachments.stats.Shurikenjutsu;
import net.narutoxboruto.entities.throwables.*;
import net.narutoxboruto.items.PreventSlow;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ThrowableWeaponItem extends Item implements PreventSlow {
    private final String name;
    
    // Track special throw cooldowns per player (server-side)
    private static final Map<UUID, Long> specialThrowCooldowns = new HashMap<>();
    private static final long SPECIAL_THROW_COOLDOWN_MS = 2000; // 2 seconds

    public ThrowableWeaponItem(Properties props, String name) {
        super(props);
        this.name = String.valueOf(name);
    }

    public static float getPowerForTime(ServerPlayer player, int charge) {
        AtomicInteger stat = new AtomicInteger();
        // player.getCapability(StatCapabilityProvider.SHURIKENJUTSU).ifPresent(cap -> stat.set(cap.getValue()));
        float bonus = stat.get() >= 10 ? stat.get() * 0.02f : 0f;
        float f = charge / 20f;
        f = (f * f + f * 2f) / 3f + bonus;
        return Math.min(f, 1f);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!world.isClientSide) {
            throwWeapon(world, player, stack, 1.0f, 0f);
            if (!player.getAbilities().instabuild) stack.shrink(1);
            player.awardStat(Stats.ITEM_USED.get(this));
        }
        return InteractionResultHolder.sidedSuccess(stack, world.isClientSide);
    }

    public void throwWeapon(Level world, LivingEntity shooter, ItemStack stack, float power, float angleOffset) {

        AbstractThrowableWeapon proj = getProjectile(world, shooter, stack);
        proj.setPos(shooter.getX(), shooter.getEyeY() - 0.1, shooter.getZ());
        proj.shootFromRotation(shooter, shooter.getXRot(), shooter.getYRot(), angleOffset, power * 3f, 1f);

        if (power >= 1f) {
            proj.setCritArrow(true);
        }

        world.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS,
                1f, 1f / (world.getRandom().nextFloat() * 0.4f + 1.2f) + power * 0.5f);
        world.addFreshEntity(proj);

        if (shooter instanceof Player player) {
            player.getCooldowns().addCooldown(this, 2);
        }
    }

    public AbstractThrowableWeapon getProjectile(Level world, LivingEntity shooter, ItemStack stack) {
        return switch (name) {
            case "shuriken" -> new Shuriken(world, shooter);
            case "senbon" -> new Senbon(world, shooter);
            case "poison_senbon" -> new PoisonSenbon(world, shooter);
            case "explosive_kunai" -> new ExplosiveKunai(world, shooter);
            default -> new Kunai(world, shooter);
        };
    }

    @Override
    public void releaseUsing(ItemStack stack, Level world, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof ServerPlayer player)) return;
        int charge = this.getUseDuration(stack) - timeLeft;
        float power = getPowerForTime(player, charge);
        if (power > 0.2f || getPowerForTime(player, charge) >= 1f) {
            if (!world.isClientSide && !player.getCooldowns().isOnCooldown(this)) {
                throwWeapon(world, player, stack, power, 0f);
            }
            if (!player.getAbilities().instabuild) stack.shrink(1);
            player.awardStat(Stats.ITEM_USED.get(this));
        }
    }


    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPEAR;
    }

    /**
     * Called from SpecialThrowPacket to perform the special throw ability
     */
    public void performSpecialThrow(ServerPlayer serverPlayer, ItemStack stack) {
        UUID playerUUID = serverPlayer.getUUID();
        long currentTime = System.currentTimeMillis();
        
        // Check if special throw is on cooldown
        Long lastUseTime = specialThrowCooldowns.get(playerUUID);
        if (lastUseTime != null && (currentTime - lastUseTime) < SPECIAL_THROW_COOLDOWN_MS) {
            return; // Still on cooldown
        }
        
        // Check if player has enough items
        if (!serverPlayer.getAbilities().instabuild && stack.getCount() < 3) {
            return; // Not enough items
        }
        
        // Throw 3 projectiles with slight angle spread for visual effect
        float angleSpread = 2.0f; // Degrees between projectiles
        throwWeapon(serverPlayer.level(), serverPlayer, stack, 1.0f, -angleSpread);
        throwWeapon(serverPlayer.level(), serverPlayer, stack, 1.0f, 0f);
        throwWeapon(serverPlayer.level(), serverPlayer, stack, 1.0f, angleSpread);
        
        // Consume 3 items (unless creative mode)
        if (!serverPlayer.getAbilities().instabuild) {
            stack.shrink(3);
        }
        
        // Set cooldown time (only affects special throw, not normal throws)
        specialThrowCooldowns.put(playerUUID, currentTime);
        
        // Clean up old entries to prevent memory leak
        specialThrowCooldowns.entrySet().removeIf(entry -> 
            (currentTime - entry.getValue()) > SPECIAL_THROW_COOLDOWN_MS * 2
        );
    }

    public boolean canSpecialThrow(ServerPlayer serverPlayer, ItemStack stack) {
        Shurikenjutsu shurikenjutsu = serverPlayer.getData(MainAttachment.SHURIKENJUTSU);
        int statValue = (shurikenjutsu != null) ? shurikenjutsu.getValue() : 0;

        return statValue >= 20
                && !serverPlayer.getCooldowns().isOnCooldown(this)
                && (serverPlayer.getAbilities().instabuild || stack.getCount() >= 3);
    }

    public void specialThrow(ServerPlayer serverPlayer, ItemStack stack, float angle) {
        if (canSpecialThrow(serverPlayer, stack)) {
            this.throwWeapon(serverPlayer.level(), serverPlayer, stack, 0.5F, -angle);
            this.throwWeapon(serverPlayer.level(), serverPlayer, stack, 0.5F, 0F);
            this.throwWeapon(serverPlayer.level(), serverPlayer, stack, 0.5F, angle);
            if (!serverPlayer.getAbilities().instabuild) {
                stack.shrink(3);
            }
            Minecraft.getInstance().player.swing(InteractionHand.MAIN_HAND);
            serverPlayer.getCooldowns().addCooldown(this, 30);
        }
    }
}