package net.narutoxboruto.events;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.narutoxboruto.attachments.MainAttachment;
import net.narutoxboruto.attachments.info.Chakra;
import net.narutoxboruto.attachments.info.MaxChakra;
import net.narutoxboruto.attachments.modes.ChakraControl;
import net.narutoxboruto.attachments.stats.*;
import net.narutoxboruto.effect.ModEffects;
import net.narutoxboruto.items.ModItems;
import net.narutoxboruto.items.swords.Kiba;
import net.narutoxboruto.items.jutsus.LightningChakraMode;
import net.narutoxboruto.util.ModUtil;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StatEvents {

    private static final Map<UUID, Integer> playerHitCounters = new HashMap<>();
    private static final Map<UUID, Integer> playerDamageCounters = new HashMap<>();
    private static final Map<UUID, Integer> playerThrowCounters = new HashMap<>(); // NEW: Counter for tracking thrown items
    private static final Map<UUID, Integer> kibaDrainTimers = new HashMap<>(); // Timer for Kiba chakra drain
    private static final Map<UUID, Integer> lightningChakraModeDrainTimers = new HashMap<>(); // Timer for Lightning Chakra Mode drain
    private static boolean taiFlag, kenFlag;
    private static int chakraDrainTimer;

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        if (event.getSource().getEntity() instanceof ServerPlayer serverPlayer) {
            UUID playerId = serverPlayer.getUUID();

            // Increment hit counter
            int currentHits = playerHitCounters.getOrDefault(playerId, 0) + 1;
            playerHitCounters.put(playerId, currentHits);

            // Every 20 hits, give combat point
            if (currentHits >= 20) {
                if (serverPlayer.getMainHandItem().isEmpty()) {
                    Taijutsu taijutsu = serverPlayer.getData(MainAttachment.TAIJUTSU);
                    taijutsu.incrementValue(1, serverPlayer);
                    // Award SP for actual stat gain
                    serverPlayer.getData(MainAttachment.SHINOBI_POINTS).incrementValue(1, serverPlayer);
                    taiFlag = true;
                }
                else if (serverPlayer.getMainHandItem().getItem() instanceof SwordItem) {
                    Kenjutsu kenjutsu = serverPlayer.getData(MainAttachment.KENJUTSU);
                    kenjutsu.incrementValue(1, serverPlayer);
                    // Award SP for actual stat gain
                    serverPlayer.getData(MainAttachment.SHINOBI_POINTS).incrementValue(1, serverPlayer);
                    kenFlag = true;
                }

                // Reset counter
                playerHitCounters.put(playerId, 0);
            }
        }

        // Check if the entity taking damage is a player (for receiving damage)
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            UUID playerId = serverPlayer.getUUID();

            // Increment damage taken counter
            int currentDamageTaken = playerDamageCounters.getOrDefault(playerId, 0) + 1;
            playerDamageCounters.put(playerId, currentDamageTaken);

            // Every 20 times the player gets hit, give extra max HP
            if (currentDamageTaken >= 20) {
                // Get medical attachment and increment it
                Medical medical = serverPlayer.getData(MainAttachment.MEDICAL);
                medical.incrementValue(1, serverPlayer);
                // Award SP for actual stat gain
                serverPlayer.getData(MainAttachment.SHINOBI_POINTS).incrementValue(1, serverPlayer);

                // Increase max health by 1 heart (2 HP)
                AttributeInstance maxHealthAttr = serverPlayer.getAttribute(Attributes.MAX_HEALTH);
                if (maxHealthAttr != null) {
                    double currentMaxHealth = maxHealthAttr.getBaseValue();
                    maxHealthAttr.setBaseValue(currentMaxHealth + 2.0); // Add 1 heart
                }
                // Reset counter
                playerDamageCounters.put(playerId, 0);
            }
        }
    }

    @SubscribeEvent
    public static void onProjectileLaunch(ProjectileImpactEvent event) {
        if (event.getProjectile() instanceof AbstractArrow arrow) {
            // Check if shooter is a player
            if (arrow.getOwner() instanceof ServerPlayer player) {
                // Check what item the player is holding
                ItemStack heldItem = player.getMainHandItem();

                // Only give points for specific items
                if (isCustomThrownWeapon(heldItem)) {
                    // Get shurikenjutsu attachment
                    Shurikenjutsu shurikenjutsu = player.getData(MainAttachment.SHURIKENJUTSU);

                    // Increment by 1 point
                    shurikenjutsu.incrementValue(1, player);

                    // Also give SP
                    player.getData(MainAttachment.SHINOBI_POINTS).incrementValue(1, player);

                    // Optional feedback message
                    player.displayClientMessage(
                            Component.translatable("msg.shurikenjutsu_increased", shurikenjutsu.getValue()),
                            true
                    );

                    // Debug log (optional)
                    // System.out.println("Shurikenjutsu increased to: " + shurikenjutsu.getValue());
                }
            }
        }
    }

    private static boolean isCustomThrownWeapon(ItemStack stack) {
        Item item = stack.getItem();
        return item == ModItems.SHURIKEN.get() ||
                item == ModItems.KUNAI.get() ||
                item == ModItems.EXPLOSIVE_KUNAI.get() ||
                item == ModItems.SENBON.get() ||
                item == ModItems.POISON_SENBON.get() ||
                item == ModItems.FUMA_SHURIKEN.get();
    }


    @SubscribeEvent
    public static void addStatBonuses(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            // Skip stat-based speed if Lightning Chakra Mode is active
            // Let the jutsu handle speed effects instead
            if (LightningChakraMode.isActive(serverPlayer)) {
                return;
            }
            
            Speed speed = serverPlayer.getData(MainAttachment.SPEED);
            int speedLevel = speed.getValue() / 10;

            // Only apply effect if we have a positive level
            if (speedLevel > 0) {
                MobEffectInstance currentEffect = serverPlayer.getEffect(MobEffects.MOVEMENT_SPEED);

                if (currentEffect == null || currentEffect.getAmplifier() != speedLevel - 1) {
                    serverPlayer.removeEffect(MobEffects.MOVEMENT_SPEED);
                    serverPlayer.addEffect(
                            new MobEffectInstance(MobEffects.MOVEMENT_SPEED, -1, speedLevel - 1,
                                    false, false, true));
                }
            } else if (speedLevel == 0) {
                serverPlayer.removeEffect(MobEffects.MOVEMENT_SPEED);
            }
        }
    }

    @SubscribeEvent
    public static void tickStats(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            if (serverPlayer.tickCount % 6000 == 0) {
                Chakra chakra = serverPlayer.getData(MainAttachment.CHAKRA);
                MaxChakra maxChakra = serverPlayer.getData(MainAttachment.MAX_CHAKRA);

                if (chakra.getValue() < maxChakra.getValue()) {
                    int missingChakra = maxChakra.getValue() - chakra.getValue();
                    int regenAmount = Math.max(1, missingChakra / 5);
                    chakra.addValue(regenAmount, serverPlayer);
                }
            }

            if (serverPlayer.tickCount % 100 == 0) {
                Speed speed = serverPlayer.getData(MainAttachment.SPEED);
                int distanceSprinted = ModUtil.getPlayerStatistics(serverPlayer, Stats.SPRINT_ONE_CM) / 100;
                int targetSpeedValue = distanceSprinted / 150;
                int currentSpeedValue = speed.getValue();

                if (targetSpeedValue > currentSpeedValue) {
                    speed.setValue(targetSpeedValue, serverPlayer);
                    serverPlayer.getData(MainAttachment.SHINOBI_POINTS).incrementValue(serverPlayer);
                }
            }
        }
    }

    @SubscribeEvent
    public static void chakraControl(PlayerTickEvent.Pre event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer)  {
            ChakraControl chakraControl = serverPlayer.getData(MainAttachment.CHAKRA_CONTROL);
            if (serverPlayer.hasEffect(ModEffects.CHAKRA_CONTROL)) {
                Chakra chakra = serverPlayer.getData(MainAttachment.CHAKRA);
                if (chakra.getValue() > 0) {
                    if (chakraDrainTimer > 0) {
                        chakraDrainTimer--;
                    }
                    else {
                        chakraDrainTimer = 600;
                        chakra.subValue(1, serverPlayer);
                    }
                }
                else {
                    chakraControl.setValue(false, serverPlayer);
                    chakraDrainTimer = 0;
                    serverPlayer.displayClientMessage(Component.translatable("msg.no_chakra"), true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void kibaChakraDrain(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            UUID playerId = serverPlayer.getUUID();
            int timer = kibaDrainTimers.getOrDefault(playerId, 0);
            
            if (timer > 0) {
                kibaDrainTimers.put(playerId, timer - 1);
            } else {
                // Drain every 20 ticks (1 second)
                kibaDrainTimers.put(playerId, 20);
                Kiba.tickChakraDrain(serverPlayer);
            }
        }
    }

    @SubscribeEvent
    public static void lightningChakraModeChakraDrain(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            UUID playerId = serverPlayer.getUUID();
            int timer = lightningChakraModeDrainTimers.getOrDefault(playerId, 0);
            
            if (timer > 0) {
                lightningChakraModeDrainTimers.put(playerId, timer - 1);
            } else {
                // Drain every 100 ticks (5 seconds)
                lightningChakraModeDrainTimers.put(playerId, 100);
                LightningChakraMode.tickChakraDrain(serverPlayer);
            }
        }
    }
}
