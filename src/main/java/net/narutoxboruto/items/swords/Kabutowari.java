package net.narutoxboruto.items.swords;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.narutoxboruto.attachments.MainAttachment;
import net.narutoxboruto.attachments.info.Chakra;

import java.util.*;

/**
 * Kabutowari - The Bluntsword (鈍刀・兜割)
 * One of the Seven Swords of the Mist.
 * 
 * Ability: V-shaped earthquake attack in front of the player.
 * - Right-click on ground to activate
 * - Creates a cone-shaped shockwave that lifts blocks
 * - Entities caught in the V take damage (more at origin, less further out)
 * - Entities are knocked up (less at origin, more further out)
 * - Works on all natural blocks (dirt, stone, grass, sand, gravel, etc.)
 */
public class Kabutowari extends AbstractAbilitySword {
    
    private static final int CHAKRA_COST = 15;
    private static final int MAX_DISTANCE = 5; // How far the V extends
    private static final double V_ANGLE = 45.0; // Half-angle of the V cone in degrees
    private static final float BASE_DAMAGE = 8.0f; // Max damage at origin
    private static final float MIN_DAMAGE = 2.0f; // Min damage at max distance
    private static final double BASE_KNOCKUP = 0.3; // Knockup at origin
    private static final double MAX_KNOCKUP = 1.2; // Knockup at max distance

    public Kabutowari(Properties pProperties) {
        super(SwordCustomTiers.NUIBARI, pProperties);
    }

    @Override
    public int getChakraCost() {
        return CHAKRA_COST;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pHand) {
        ItemStack itemStack = pPlayer.getItemInHand(pHand);

        if (pLevel.isClientSide()) {
            return InteractionResultHolder.consume(itemStack);
        }

        if (pPlayer instanceof ServerPlayer serverPlayer) {
            // Check chakra
            Chakra chakra = serverPlayer.getData(MainAttachment.CHAKRA);
            if (chakra.getValue() < CHAKRA_COST) {
                serverPlayer.displayClientMessage(Component.translatable("msg.no_chakra"), true);
                return InteractionResultHolder.fail(itemStack);
            }
            
            // Get the ground block the player is looking at
            BlockPos targetGround = getGroundBlockLookingAt(serverPlayer, 5);
            
            if (targetGround == null) {
                serverPlayer.displayClientMessage(Component.translatable("msg.no_ground"), true);
                return InteractionResultHolder.fail(itemStack);
            }
            
            // Consume chakra
            chakra.subValue(CHAKRA_COST, serverPlayer);
            
            // Execute the earthquake
            executeEarthquake(serverPlayer, targetGround);
            
            // Swing animation
            serverPlayer.swing(pHand, true);
            
            // Add cooldown
            serverPlayer.getCooldowns().addCooldown(this, 60); // 3 second cooldown
            
            return InteractionResultHolder.success(itemStack);
        }

        return InteractionResultHolder.pass(itemStack);
    }

    @Override
    protected void doSpecialAbility(LivingEntity pTarget, ServerPlayer serverPlayer) {
        // Not used - ability is triggered via use()
    }
    
    private BlockPos getGroundBlockLookingAt(ServerPlayer player, double maxDistance) {
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getViewVector(1.0F);
        Vec3 endPos = eyePos.add(lookVec.scale(maxDistance));
        
        ClipContext context = new ClipContext(eyePos, endPos, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, CollisionContext.empty());
        BlockHitResult result = player.level().clip(context);
        
        if (result.getType() == HitResult.Type.BLOCK) {
            return result.getBlockPos();
        }
        return null;
    }

    private void executeEarthquake(ServerPlayer player, BlockPos origin) {
        Level level = player.level();
        if (!(level instanceof ServerLevel serverLevel)) return;
        
        // Player's facing direction (horizontal only)
        Vec3 lookVec = player.getViewVector(1.0F);
        double facingAngle = Math.atan2(lookVec.z, lookVec.x);
        
        int currentTick = player.getServer().getTickCount();
        
        // Play initial slam sound (reduced volume)
        level.playSound(null, origin, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.6f, 0.5f);
        
        // Process origin point instantly (distance 0)
        processEarthquakeRow(serverLevel, player, origin, facingAngle, 0);
        
        // Process the V-cone in waves (from near to far)
        // Fast propagation - 2 ticks per row for snappy feel
        int ticksPerRow = 2;
        
        for (int distance = 1; distance <= MAX_DISTANCE; distance++) {
            final int dist = distance;
            int delayTicks = distance * ticksPerRow; // 2, 4, 6, 8, 10 ticks
            
            player.getServer().tell(new TickTask(currentTick + delayTicks, () -> {
                processEarthquakeRow(serverLevel, player, origin, facingAngle, dist);
            }));
        }
    }
    
    private void processEarthquakeRow(ServerLevel level, ServerPlayer player, BlockPos origin, double facingAngle, int distance) {
        // Calculate the width of the V at this distance (origin is just 1 block)
        double vAngleRad = Math.toRadians(V_ANGLE);
        int halfWidth = distance == 0 ? 0 : (int) Math.ceil(distance * Math.tan(vAngleRad));
        
        // Height/displacement increases with distance (further = higher launch)
        // Reduced height for smoother visual
        float heightMultiplier = 0.1f + (distance * 0.1f); // 0.1 at origin, up to 0.6 at max
        
        Set<BlockPos> affectedPositions = new HashSet<>();
        
        // For distance 0 (origin), just affect the clicked block
        if (distance == 0) {
            BlockPos groundPos = findGroundLevel(level, origin, origin.getY());
            if (groundPos != null) {
                affectedPositions.add(groundPos);
                // Origin block gets particles but doesn't launch (it's where they slammed)
                level.sendParticles(
                        ParticleTypes.EXPLOSION,
                        groundPos.getX() + 0.5, groundPos.getY() + 1, groundPos.getZ() + 0.5,
                        1, 0.2, 0.1, 0.2, 0.0
                );
            }
        } else {
            // Calculate positions along the V-cone at this distance
            for (int offset = -halfWidth; offset <= halfWidth; offset++) {
                // Calculate position based on facing angle
                double perpAngle = facingAngle + Math.PI / 2; // Perpendicular to facing
                
                double x = origin.getX() + 0.5 + Math.cos(facingAngle) * distance + Math.cos(perpAngle) * offset;
                double z = origin.getZ() + 0.5 + Math.sin(facingAngle) * distance + Math.sin(perpAngle) * offset;
                
                BlockPos checkPos = new BlockPos((int) Math.floor(x), origin.getY(), (int) Math.floor(z));
                
                // Only check at the exact Y level of origin - no searching up/down
                BlockPos groundPos = findGroundLevel(level, checkPos, origin.getY());
                if (groundPos != null && !affectedPositions.contains(groundPos)) {
                    affectedPositions.add(groundPos);
                    
                    // Only move blocks that have air above (don't move tree trunks, etc.)
                    BlockState aboveState = level.getBlockState(groundPos.above());
                    boolean hasBlockAbove = !aboveState.isAir() && aboveState.isSolidRender(level, groundPos.above());
                    
                    // Create the rising block effect (blocks launch higher further out)
                    if (isNaturalBlock(level, groundPos) && !hasBlockAbove) {
                        createRisingBlock(level, groundPos, heightMultiplier, distance);
                    }
                    
                    // Spawn particles
                    level.sendParticles(
                            ParticleTypes.CLOUD,
                            groundPos.getX() + 0.5, groundPos.getY() + 1, groundPos.getZ() + 0.5,
                            2, 0.2, 0.1, 0.2, 0.02
                    );
                }
            }
        }
        
        // Handle entity damage and knockup in this row
        double searchRadius = Math.max(1.5, halfWidth + 1.0);
        double centerX = origin.getX() + 0.5 + Math.cos(facingAngle) * distance;
        double centerZ = origin.getZ() + 0.5 + Math.sin(facingAngle) * distance;
        
        AABB searchBox = new AABB(
                centerX - searchRadius, origin.getY() - 1, centerZ - searchRadius,
                centerX + searchRadius, origin.getY() + 4, centerZ + searchRadius
        );
        
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, searchBox);
        entities.remove(player);
        
        for (LivingEntity entity : entities) {
            // Check if entity is within the V-cone
            if (isInVCone(entity.position(), origin, facingAngle, distance)) {
                // Calculate damage (more at origin, less further out)
                float damageRatio = 1.0f - ((float) distance / MAX_DISTANCE);
                float damage = MIN_DAMAGE + (BASE_DAMAGE - MIN_DAMAGE) * damageRatio;
                
                // Calculate knockup (less at origin, more further out)
                double knockupRatio = (double) distance / MAX_DISTANCE;
                double knockup = BASE_KNOCKUP + (MAX_KNOCKUP - BASE_KNOCKUP) * knockupRatio;
                
                // Apply damage
                entity.hurt(level.damageSources().magic(), damage);
                
                // Apply knockup with slight outward push
                double pushAngle = Math.atan2(entity.getZ() - origin.getZ(), entity.getX() - origin.getX());
                double pushStrength = 0.2 + (distance * 0.05);
                
                entity.setDeltaMovement(
                        entity.getDeltaMovement().x + Math.cos(pushAngle) * pushStrength,
                        knockup,
                        entity.getDeltaMovement().z + Math.sin(pushAngle) * pushStrength
                );
                entity.hurtMarked = true;
            }
        }
        
        // Play rumbling sound for each wave
        level.playSound(null, centerX, origin.getY(), centerZ, 
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.5f, 0.5f + (distance * 0.05f));
    }
    
    private boolean isInVCone(Vec3 entityPos, BlockPos origin, double facingAngle, int rowDistance) {
        double dx = entityPos.x - (origin.getX() + 0.5);
        double dz = entityPos.z - (origin.getZ() + 0.5);
        double distanceFromOrigin = Math.sqrt(dx * dx + dz * dz);
        
        // For origin (distance 0), check if entity is within 1.5 blocks
        if (rowDistance == 0) {
            return distanceFromOrigin <= 1.5;
        }
        
        // Check if roughly at the right distance (within 1.5 blocks of the row)
        if (Math.abs(distanceFromOrigin - rowDistance) > 1.5) {
            return false;
        }
        
        // Check angle - must be within the V cone
        double entityAngle = Math.atan2(dz, dx);
        double angleDiff = Math.abs(normalizeAngle(entityAngle - facingAngle));
        
        return angleDiff <= Math.toRadians(V_ANGLE);
    }
    
    private double normalizeAngle(double angle) {
        while (angle > Math.PI) angle -= 2 * Math.PI;
        while (angle < -Math.PI) angle += 2 * Math.PI;
        return angle;
    }
    
    private BlockPos findGroundLevel(Level level, BlockPos startPos, int originY) {
        // Only check at the exact Y level of the clicked block
        BlockPos checkPos = new BlockPos(startPos.getX(), originY, startPos.getZ());
        BlockState state = level.getBlockState(checkPos);
        
        // Must be a solid block
        if (!state.isAir() && state.isSolidRender(level, checkPos)) {
            return checkPos;
        }
        return null;
    }
    
    private boolean isNaturalBlock(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        
        // List of natural blocks that can be affected
        return block == Blocks.DIRT ||
               block == Blocks.GRASS_BLOCK ||
               block == Blocks.COARSE_DIRT ||
               block == Blocks.PODZOL ||
               block == Blocks.ROOTED_DIRT ||
               block == Blocks.MYCELIUM ||
               block == Blocks.DIRT_PATH ||
               block == Blocks.FARMLAND ||
               block == Blocks.SAND ||
               block == Blocks.RED_SAND ||
               block == Blocks.GRAVEL ||
               block == Blocks.CLAY ||
               block == Blocks.MUD ||
               block == Blocks.SOUL_SAND ||
               block == Blocks.SOUL_SOIL ||
               block == Blocks.STONE ||
               block == Blocks.COBBLESTONE ||
               block == Blocks.MOSSY_COBBLESTONE ||
               block == Blocks.DEEPSLATE ||
               block == Blocks.COBBLED_DEEPSLATE ||
               block == Blocks.GRANITE ||
               block == Blocks.DIORITE ||
               block == Blocks.ANDESITE ||
               block == Blocks.TUFF ||
               block == Blocks.CALCITE ||
               block == Blocks.SANDSTONE ||
               block == Blocks.RED_SANDSTONE ||
               block == Blocks.NETHERRACK ||
               block == Blocks.BASALT ||
               block == Blocks.BLACKSTONE ||
               block == Blocks.END_STONE ||
               block == Blocks.TERRACOTTA ||
               state.is(BlockTags.TERRACOTTA) ||
               state.is(BlockTags.DIRT) ||
               state.is(BlockTags.SAND) ||
               state.is(BlockTags.BASE_STONE_OVERWORLD) ||
               state.is(BlockTags.BASE_STONE_NETHER);
    }
    
    private void createRisingBlock(Level level, BlockPos pos, float heightMultiplier, int distance) {
        if (level.isClientSide) return;
        
        BlockState state = level.getBlockState(pos);
        
        // Skip if invalid
        if (state.isAir() || level.getBlockEntity(pos) != null) {
            return;
        }
        
        // Don't remove the origin block - only lift blocks further out
        if (distance == 0) {
            return;
        }
        
        if (level instanceof ServerLevel serverLevel) {
            double x = pos.getX() + 0.5;
            double y = pos.getY();
            double z = pos.getZ() + 0.5;
            
            // Remove the block first
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            
            // Create the falling block using spawn with consumer
            FallingBlockEntity fallingBlock = new FallingBlockEntity(EntityType.FALLING_BLOCK, serverLevel);
            
            // Now read the block state from NBT using load() which is public
            // We need to include all required entity data
            net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
            tag.put("BlockState", net.minecraft.nbt.NbtUtils.writeBlockState(state));
            tag.putInt("Time", 1);
            tag.putBoolean("DropItem", true);
            // Position data for load()
            net.minecraft.nbt.ListTag posList = new net.minecraft.nbt.ListTag();
            posList.add(net.minecraft.nbt.DoubleTag.valueOf(x));
            posList.add(net.minecraft.nbt.DoubleTag.valueOf(y));
            posList.add(net.minecraft.nbt.DoubleTag.valueOf(z));
            tag.put("Pos", posList);
            // Motion data
            net.minecraft.nbt.ListTag motionList = new net.minecraft.nbt.ListTag();
            double upwardVelocity = 0.25 + (distance * 0.08);
            motionList.add(net.minecraft.nbt.DoubleTag.valueOf(0));
            motionList.add(net.minecraft.nbt.DoubleTag.valueOf(upwardVelocity));
            motionList.add(net.minecraft.nbt.DoubleTag.valueOf(0));
            tag.put("Motion", motionList);
            
            fallingBlock.load(tag);
            
            // Add to world
            serverLevel.addFreshEntity(fallingBlock);
            
            // Play rumble sound (quiet)
            float pitch = 0.7f + (float)(Math.random() * 0.3);
            level.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 0.25f, pitch);
        }
    }
}
