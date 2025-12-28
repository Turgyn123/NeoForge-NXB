package net.narutoxboruto.jutsu;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.narutoxboruto.attachments.MainAttachment;
import net.narutoxboruto.attachments.info.Chakra;
import net.narutoxboruto.attachments.info.ReleaseList;
import net.narutoxboruto.attachments.modes.ChakraControl;
import net.narutoxboruto.entities.ModEntities;
import net.narutoxboruto.entities.jutsus.FireBallEntity;
import net.narutoxboruto.util.ModUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles the server-side logic for casting jutsus.
 */
public class JutsuCaster {
    
    // Cooldown tracking per player per jutsu
    private static final Map<UUID, Map<String, Long>> COOLDOWNS = new HashMap<>();
    
    /**
     * Try to cast the player's currently selected jutsu.
     */
    public static boolean tryCastSelectedJutsu(ServerPlayer player) {
        // Get the jutsu wheel
        JutsuWheel wheel = player.getData(MainAttachment.JUTSU_WHEEL);
        String jutsuId = wheel.getSelectedJutsu();
        
        if (jutsuId == null || jutsuId.isEmpty()) {
            ModUtil.displayColoredMessage(player, "msg.no_jutsu_selected", ChatFormatting.RED);
            return false;
        }
        
        return tryCastJutsu(player, jutsuId);
    }
    
    /**
     * Try to cast a specific jutsu.
     */
    public static boolean tryCastJutsu(ServerPlayer player, String jutsuId) {
        JutsuData jutsu = JutsuRegistry.getJutsuById(jutsuId);
        if (jutsu == null) {
            ModUtil.displayColoredMessage(player, "msg.unknown_jutsu", ChatFormatting.RED);
            return false;
        }
        
        // Check if player is channeling chakra
        ChakraControl chakraControl = player.getData(MainAttachment.CHAKRA_CONTROL);
        if (!chakraControl.getValue()) {
            ModUtil.displayColoredMessage(player, "msg.not_channeling", ChatFormatting.RED);
            return false;
        }
        
        // Check if player has the required nature release
        String requiredNature = JutsuRegistry.getNatureForJutsu(jutsuId);
        if (requiredNature != null) {
            ReleaseList releases = player.getData(MainAttachment.RELEASE_LIST);
            if (!releases.getValue().toLowerCase().contains(requiredNature.toLowerCase())) {
                ModUtil.displayColoredMessage(player, "msg.no_release", ChatFormatting.RED);
                return false;
            }
        }
        
        // Check cooldown
        if (isOnCooldown(player, jutsuId)) {
            ModUtil.displayColoredMessage(player, "msg.jutsu_cooldown", ChatFormatting.YELLOW);
            return false;
        }
        
        // Check chakra
        Chakra chakra = player.getData(MainAttachment.CHAKRA);
        if (chakra.getValue() < jutsu.getChakraCost()) {
            ModUtil.displayColoredMessage(player, "msg.no_chakra", ChatFormatting.RED);
            return false;
        }
        
        // Apply cooldown BEFORE executing to prevent duplicate cast attempts from showing cooldown message
        // (RightClickBlock and RightClickEmpty can both fire on the same click)
        setCooldown(player, jutsuId, jutsu.getCooldownTicks());
        
        // Consume chakra upfront
        chakra.subValue(jutsu.getChakraCost(), player);
        
        // Execute the jutsu
        boolean success = executeJutsu(player, jutsuId);
        
        if (success) {
            // Display jutsu name
            ModUtil.displayColoredMessage(player, jutsu.getTranslationKey(), ChatFormatting.YELLOW);
        }
        
        return success;
    }
    
    /**
     * Execute the actual jutsu effect.
     */
    private static boolean executeJutsu(ServerPlayer player, String jutsuId) {
        Level level = player.level();
        
        switch (jutsuId) {
            case "earth_wall":
                return castEarthWall(player, level);
            case "fire_ball":
                return castFireBall(player, level);
            default:
                return false;
        }
    }
    
    /**
     * Cast Earth Wall jutsu.
     */
    private static boolean castEarthWall(ServerPlayer player, Level level) {
        // Raycast to find where player is looking
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getViewVector(1.0F);
        Vec3 endPos = eyePos.add(lookVec.scale(10));
        
        ClipContext clipContext = new ClipContext(eyePos, endPos, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player);
        BlockHitResult hitResult = level.clip(clipContext);
        
        if (hitResult.getType() != HitResult.Type.BLOCK) {
            ModUtil.displayColoredMessage(player, "msg.no_ground", ChatFormatting.RED);
            return false;
        }
        
        BlockPos hitPos = hitResult.getBlockPos();
        
        // Check if we hit solid ground
        if (!level.getBlockState(hitPos).isSolidRender(level, hitPos)) {
            ModUtil.displayColoredMessage(player, "msg.no_ground", ChatFormatting.RED);
            return false;
        }
        
        // Build the wall (7 wide x 3 tall x 2 deep)
        Direction facing = player.getDirection();
        buildWall(level, hitPos, facing);
        
        // Play sound
        level.playSound(null, hitPos, SoundEvents.GRAVEL_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
        
        return true;
    }
    
    private static void buildWall(Level level, BlockPos center, Direction facing) {
        int width = 3; // 7 total (center + 3 each side)
        int height = 3;
        int depth = 2;
        
        // Determine offset directions based on facing
        Direction widthDir = facing.getClockWise();
        
        for (int w = -width; w <= width; w++) {
            for (int h = 1; h <= height; h++) {
                for (int d = 0; d < depth; d++) {
                    BlockPos wallPos = center
                        .relative(widthDir, w)
                        .above(h)
                        .relative(facing, d);
                    
                    if (level.getBlockState(wallPos).isAir()) {
                        // Always use dirt for the earth wall
                        level.setBlock(wallPos, Blocks.DIRT.defaultBlockState(), 3);
                    }
                }
            }
        }
    }
    
    /**
     * Cast Fire Ball jutsu.
     */
    private static boolean castFireBall(ServerPlayer player, Level level) {
        // Create and spawn the fireball entity
        FireBallEntity fireball = new FireBallEntity(ModEntities.FIRE_BALL.get(), level);
        fireball.setOwner(player);
        
        // Position at player's eye level
        Vec3 eyePos = player.getEyePosition();
        fireball.setPos(eyePos.x, eyePos.y - 0.2, eyePos.z);
        
        // Set velocity based on look direction
        Vec3 lookVec = player.getViewVector(1.0F);
        float speed = 1.5F;
        fireball.setDeltaMovement(lookVec.scale(speed));
        
        // Spawn the entity
        level.addFreshEntity(fireball);
        
        // Play sound
        level.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
        
        return true;
    }
    
    // Cooldown management
    
    private static boolean isOnCooldown(ServerPlayer player, String jutsuId) {
        Map<String, Long> playerCooldowns = COOLDOWNS.get(player.getUUID());
        if (playerCooldowns == null) return false;
        
        Long cooldownEnd = playerCooldowns.get(jutsuId);
        if (cooldownEnd == null) return false;
        
        return System.currentTimeMillis() < cooldownEnd;
    }
    
    private static void setCooldown(ServerPlayer player, String jutsuId, int ticks) {
        COOLDOWNS.computeIfAbsent(player.getUUID(), k -> new HashMap<>())
            .put(jutsuId, System.currentTimeMillis() + (ticks * 50L)); // 50ms per tick
    }
    
    /**
     * Clear cooldowns for a player (call on logout).
     */
    public static void clearCooldowns(UUID playerId) {
        COOLDOWNS.remove(playerId);
    }
}
