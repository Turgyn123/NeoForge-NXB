package net.narutoxboruto.items.jutsus;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Earth Wave Jutsu - Creates a circular wave of earth that expands outward from the caster,
 * launching blocks and entities into the air with consistent height.
 * 
 * Features:
 * - Circular wave expands in all directions around player
 * - All blocks launch to same height (consistent wave)
 * - Consistent damage across the entire wave
 * - Applies Slowness I for 5 seconds on hit
 * - Entities are launched high for additional fall damage
 * - No target required - just right click to activate
 */
@EventBusSubscriber(modid = "narutoxboruto")
public class EarthWave extends AbstractJutsuItem {
    
    private static final int CHAKRA_COST = 5;
    private static final int COOLDOWN_TICKS = 40; // 2 seconds
    private static final int MAX_DISTANCE = 5; // Smaller radius for circular wave
    private static final float DAMAGE = 6.0f; // Consistent damage
    private static final double KNOCKUP = 1.2; // High launch for fall damage
    private static final int SLOWNESS_DURATION = 100; // 5 seconds
    private static final int TICKS_PER_RING = 4; // Ticks between each ring (4 ticks = 0.2 sec)
    
    // Track active waves - processed by tick event
    private static final Queue<WaveData> ACTIVE_WAVES = new ConcurrentLinkedQueue<>();

    public EarthWave(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public String getJutsuName() {
        return "Earth Wave";
    }

    @Override
    public String getRequiredRelease() {
        return "earth";
    }

    @Override
    public int getChakraCost() {
        return CHAKRA_COST;
    }

    @Override
    public int getCooldownTicks() {
        return COOLDOWN_TICKS;
    }

    @Override
    protected boolean executeJutsu(ServerPlayer player, Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        
        // Get where player is standing
        BlockPos origin = player.getOnPos();
        
        // Check if standing on valid ground
        if (!isNaturalBlock(level, origin)) {
            return false;
        }
        
        // Create wave data and add to queue for processing
        // No direction needed - wave expands in a circle
        WaveData wave = new WaveData(serverLevel, player, origin);
        ACTIVE_WAVES.add(wave);
        
        // No sound effect - wave is silent
        
        return true;
    }
    
    /**
     * Server tick event handler - processes all active waves
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (ACTIVE_WAVES.isEmpty()) return;
        
        Iterator<WaveData> iterator = ACTIVE_WAVES.iterator();
        while (iterator.hasNext()) {
            WaveData wave = iterator.next();
            wave.tickCounter++;
            
            // Check if it's time to process the next ring
            if (wave.tickCounter >= TICKS_PER_RING) {
                wave.tickCounter = 0;
                wave.currentDistance++;
                
                if (wave.currentDistance <= MAX_DISTANCE) {
                    // Process this ring
                    processWaveRing(wave.level, wave.player, wave.origin, wave.currentDistance);
                } else {
                    // Wave complete, remove from queue
                    iterator.remove();
                }
            }
        }
    }
    
    /**
     * Process a circular ring of the wave at the given distance from origin.
     */
    private static void processWaveRing(ServerLevel level, ServerPlayer player, BlockPos origin, int distance) {
        Set<BlockPos> affectedPositions = new HashSet<>();
        
        // Calculate points around the circle at this distance
        // Use enough points to cover all blocks at this radius
        int numPoints = Math.max(8, distance * 8); // More points for larger rings
        
        for (int i = 0; i < numPoints; i++) {
            double angle = (2 * Math.PI * i) / numPoints;
            
            int blockX = origin.getX() + (int) Math.round(Math.cos(angle) * distance);
            int blockZ = origin.getZ() + (int) Math.round(Math.sin(angle) * distance);
            
            // Only check the exact Y level where the wave was activated
            BlockPos checkPos = new BlockPos(blockX, origin.getY(), blockZ);
            
            if (affectedPositions.contains(checkPos)) continue;
            
            BlockState state = level.getBlockState(checkPos);
            BlockState aboveState = level.getBlockState(checkPos.above());
            
            // Check if this is a valid ground block (solid block with air above)
            if (!state.isAir() && state.isSolidRender(level, checkPos) && aboveState.isAir()) {
                affectedPositions.add(checkPos);
                
                // Create the rising block effect
                if (isNaturalBlock(level, checkPos)) {
                    createRisingBlock(level, checkPos);
                }
            }
        }
        
        // Handle entity damage and knockup in this ring
        // Create a ring-shaped search area
        AABB searchBox = new AABB(
                origin.getX() - distance - 1, origin.getY() - 1, origin.getZ() - distance - 1,
                origin.getX() + distance + 2, origin.getY() + 4, origin.getZ() + distance + 2
        );
        
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, searchBox);
        entities.remove(player);
        
        for (LivingEntity entity : entities) {
            // Check if entity is roughly on this ring (within 1.5 blocks of the ring distance)
            double entityDx = entity.getX() - origin.getX();
            double entityDz = entity.getZ() - origin.getZ();
            double entityDistance = Math.sqrt(entityDx * entityDx + entityDz * entityDz);
            
            if (Math.abs(entityDistance - distance) <= 1.5) {
                // Apply consistent damage
                entity.hurt(level.damageSources().magic(), DAMAGE);
                
                // Apply knockup - straight up with slight outward push
                double pushX = entityDx / Math.max(entityDistance, 0.1) * 0.3;
                double pushZ = entityDz / Math.max(entityDistance, 0.1) * 0.3;
                entity.setDeltaMovement(pushX, KNOCKUP, pushZ);
                entity.hurtMarked = true;
                
                // Apply Slowness I for 5 seconds
                entity.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SLOWDOWN,
                        SLOWNESS_DURATION,
                        0,
                        false, true
                ));
            }
        }
        
        // No sound effect for wave rings
    }
    
    private static boolean isNaturalBlock(Level level, BlockPos pos) {
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
               block == Blocks.MUD ||
               block == Blocks.CLAY ||
               block == Blocks.GRAVEL ||
               block == Blocks.SAND ||
               block == Blocks.RED_SAND ||
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
    
    private static void createRisingBlock(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        
        // Skip if invalid
        if (state.isAir() || level.getBlockEntity(pos) != null) {
            return;
        }
        
        double x = pos.getX() + 0.5;
        double y = pos.getY();
        double z = pos.getZ() + 0.5;
        
        // Remove the block first
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        
        // Create falling block entity manually
        FallingBlockEntity fallingBlock = new FallingBlockEntity(EntityType.FALLING_BLOCK, level);
        
        // Now read the block state from NBT using load() which is public
        CompoundTag tag = new CompoundTag();
        tag.put("BlockState", NbtUtils.writeBlockState(state));
        tag.putInt("Time", 1);
        tag.putBoolean("DropItem", true);
        // Position data for load()
        ListTag posList = new ListTag();
        posList.add(DoubleTag.valueOf(x));
        posList.add(DoubleTag.valueOf(y));
        posList.add(DoubleTag.valueOf(z));
        tag.put("Pos", posList);
        // Motion data - CONSTANT velocity so all blocks peak at the same height
        ListTag motionList = new ListTag();
        double upwardVelocity = 0.55; // All blocks rise to same height
        motionList.add(DoubleTag.valueOf(0));
        motionList.add(DoubleTag.valueOf(upwardVelocity));
        motionList.add(DoubleTag.valueOf(0));
        tag.put("Motion", motionList);
        
        fallingBlock.load(tag);
        
        // Add to world
        level.addFreshEntity(fallingBlock);
    }
    
    /**
     * Data class to track an active earth wave.
     */
    private static class WaveData {
        final ServerLevel level;
        final ServerPlayer player;
        final BlockPos origin;
        int currentDistance = 0;
        int tickCounter = 0;
        
        WaveData(ServerLevel level, ServerPlayer player, BlockPos origin) {
            this.level = level;
            this.player = player;
            this.origin = origin;
        }
    }
}

