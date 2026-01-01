package net.narutoxboruto.items.jutsus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.tags.FluidTags;
import net.narutoxboruto.fluids.ModFluidBlocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Water Prison Jutsu - Traps entities in a 3x3 water prison.
 * 
 * Features:
 * - Creates a 3x3 water block prison lasting 10 seconds
 * - Entities inside are pulled to center and immobilized
 * - Causes suffocation damage over time
 * - Can target entities directly or place at location
 * - Maximum 1 prison per player at a time
 * - 10 second cooldown
 * 
 * Uses ServerTickEvent instead of TickTask for reliable timing on integrated servers.
 */
@EventBusSubscriber(modid = "narutoxboruto")
public class WaterPrison extends AbstractJutsuItem {
    
    private static final int CHAKRA_COST = 15;
    private static final int COOLDOWN_TICKS = 200; // 10 seconds
    private static final int PRISON_DURATION_TICKS = 200; // 10 seconds
    private static final int MAX_PRISONS_PER_PLAYER = 1;
    private static final float SUFFOCATION_DAMAGE = 1.0f; // Half heart per second
    private static final int SUFFOCATION_INTERVAL = 20; // Damage every second
    private static final int TICK_INTERVAL = 5; // Process effects every 5 ticks
    
    // Queue for managing active prisons - processed by ServerTickEvent
    private static final Queue<PrisonData> ACTIVE_PRISONS_QUEUE = new ConcurrentLinkedQueue<>();
    
    // Track prisons per player for max limit
    private static final Map<UUID, List<PrisonData>> PLAYER_PRISONS = new ConcurrentHashMap<>();
    
    // Track entities trapped in prisons
    private static final Set<UUID> TRAPPED_ENTITIES = ConcurrentHashMap.newKeySet();
    
    public WaterPrison(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public String getJutsuName() {
        return "Water Prison";
    }

    @Override
    public String getRequiredRelease() {
        return "water";
    }

    @Override
    public int getChakraCost() {
        return CHAKRA_COST;
    }

    @Override
    public int getCooldownTicks() {
        return COOLDOWN_TICKS;
    }
    
    /**
     * ServerTickEvent handler - processes all active water prisons.
     * This replaces the broken TickTask approach.
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (ACTIVE_PRISONS_QUEUE.isEmpty()) {
            return;
        }
        
        // Process each active prison
        Iterator<PrisonData> iterator = ACTIVE_PRISONS_QUEUE.iterator();
        while (iterator.hasNext()) {
            PrisonData prison = iterator.next();
            
            // Check if prison has expired
            if (!prison.isActive || prison.ticksRemaining <= 0) {
                // Remove the prison
                removePrison(prison);
                iterator.remove();
                continue;
            }
            
            // Process the prison effects
            tickPrison(prison);
            
            // Decrement remaining time
            prison.ticksRemaining--;
        }
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (level.isClientSide()) {
            return InteractionResultHolder.pass(stack);
        }
        
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.fail(stack);
        }
        
        // Check max prisons
        List<PrisonData> playerPrisons = PLAYER_PRISONS.getOrDefault(player.getUUID(), new ArrayList<>());
        if (playerPrisons.size() >= MAX_PRISONS_PER_PLAYER) {
            serverPlayer.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("Maximum water prisons active (1)!")
                            .withStyle(net.minecraft.ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        
        // Let parent class handle chakra/cooldown
        return super.use(level, player, hand);
    }

    @Override
    protected boolean executeJutsu(ServerPlayer player, Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        
        // Check max prisons again (safety)
        List<PrisonData> playerPrisons = PLAYER_PRISONS.computeIfAbsent(player.getUUID(), k -> new ArrayList<>());
        if (playerPrisons.size() >= MAX_PRISONS_PER_PLAYER) {
            return false;
        }
        
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getViewVector(1.0F);
        Vec3 endPos = eyePos.add(lookVec.scale(10.0)); // Extended range for block detection
        
        // Check if player is in water
        boolean playerInWater = player.isInWater();
        
        // Check for entity target first
        AABB searchBox = player.getBoundingBox().expandTowards(lookVec.scale(5.0)).inflate(1.0);
        EntityHitResult entityHit = findEntityOnPath(level, player, eyePos, endPos, searchBox);
        
        BlockPos centerPos;
        LivingEntity targetEntity = null;
        
        if (entityHit != null && entityHit.getEntity() instanceof LivingEntity target && target != player) {
            // Direct entity target - center prison on them
            targetEntity = target;
            centerPos = target.blockPosition();
        } else if (playerInWater) {
            // Player is IN water - ignore water blocks, place prison 5 blocks away in air
            // Treat water as air for placement purposes
            Vec3 targetPos = eyePos.add(lookVec.scale(5.0));
            centerPos = new BlockPos((int) Math.floor(targetPos.x), (int) Math.floor(targetPos.y), (int) Math.floor(targetPos.z));
        } else {
            // Smart placement - raycast to find what block/face player is looking at
            // Use FLUIDS.SOURCE_ONLY to detect water surface as a hittable surface
            BlockHitResult blockHit = level.clip(new ClipContext(
                    eyePos, endPos,
                    ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.SOURCE_ONLY, // Detect water surface!
                    player
            ));
            
            if (blockHit.getType() == HitResult.Type.BLOCK) {
                BlockState hitState = level.getBlockState(blockHit.getBlockPos());
                
                // Check if we hit water surface
                if (hitState.getFluidState().is(FluidTags.WATER)) {
                    // Clicked on water - treat it like a solid block for placement
                    // Use top/side logic as if it were a block
                    centerPos = calculateSmartPlacement(blockHit.getBlockPos(), blockHit.getDirection());
                } else {
                    // Player is looking at a solid block - smart placement!
                    centerPos = calculateSmartPlacement(blockHit.getBlockPos(), blockHit.getDirection());
                }
            } else {
                // Air placement - place prison 5 blocks in front of player
                Vec3 targetPos = eyePos.add(lookVec.scale(5.0));
                centerPos = new BlockPos((int) Math.floor(targetPos.x), (int) Math.floor(targetPos.y), (int) Math.floor(targetPos.z));
            }
        }
        
        // Create the prison
        return createPrison(serverLevel, player, centerPos, targetEntity);
    }
    
    /**
     * Calculate smart prison placement based on clicked block and face.
     * The prison's center face on the clicked side will touch the clicked block.
     * 
     * Prison is 3x3x3 centered at centerPos, extending:
     * X: center-1 to center+1
     * Y: center to center+2 (bottom, middle, top layers)
     * Z: center-1 to center+1
     */
    private BlockPos calculateSmartPlacement(BlockPos clickedBlock, Direction clickedFace) {
        // Offset the prison center so the face touching the clicked block aligns properly
        // Prison extends 1 block in each horizontal direction from center, 
        // and 0-2 blocks up from center Y
        
        return switch (clickedFace) {
            case UP -> 
                // Clicked top of block - prison spawns above, bottom layer at clicked.y + 1
                clickedBlock.above();
            case DOWN -> 
                // Clicked bottom of block - prison spawns below, top layer at clicked.y - 1
                clickedBlock.below(3); // center.y + 2 = clicked.y - 1, so center = clicked - 3
            case NORTH -> 
                // Clicked north face (facing -Z) - prison spawns to the north
                // Prison's south edge (center.z + 1) should be at clickedBlock.z - 1
                clickedBlock.north(2);
            case SOUTH -> 
                // Clicked south face (facing +Z) - prison spawns to the south
                // Prison's north edge (center.z - 1) should be at clickedBlock.z + 1
                clickedBlock.south(2);
            case WEST -> 
                // Clicked west face (facing -X) - prison spawns to the west
                clickedBlock.west(2);
            case EAST -> 
                // Clicked east face (facing +X) - prison spawns to the east
                clickedBlock.east(2);
        };
    }
    
    private boolean createPrison(ServerLevel level, ServerPlayer caster, BlockPos center, LivingEntity initialTarget) {
        Set<BlockPos> prisonBlocks = new HashSet<>();
        Map<BlockPos, BlockState> originalBlocks = new HashMap<>();
        Set<BlockPos> positionsNeedingVanillaWater = new HashSet<>(); // Positions that should use vanilla water
        
        // First pass: Identify all positions and check which ones are in/adjacent to existing water
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    BlockState currentState = level.getBlockState(pos);
                    
                    // Check if this position or any horizontally adjacent block contains water
                    // Only check same Y level to prevent water below from causing flow issues
                    boolean adjacentToWater = false;
                    if (currentState.getFluidState().is(FluidTags.WATER)) {
                        adjacentToWater = true;
                    } else {
                        // Check only horizontal neighbors (same Y level) for water
                        for (Direction dir : Direction.Plane.HORIZONTAL) {
                            BlockPos adjacent = pos.relative(dir);
                            if (level.getBlockState(adjacent).getFluidState().is(FluidTags.WATER)) {
                                adjacentToWater = true;
                                break;
                            }
                        }
                    }
                    
                    if (adjacentToWater) {
                        positionsNeedingVanillaWater.add(pos);
                    }
                    
                    // Skip existing water blocks - don't replace them!
                    if (currentState.getFluidState().is(FluidTags.WATER)) {
                        // Still track this position for entity containment
                        prisonBlocks.add(pos);
                        // Don't store original state since we're not changing it
                        continue;
                    }
                    
                    // Skip solid terrain blocks - leave terrain as is!
                    // Only replace air and non-solid blocks (plants, snow, etc)
                    if (currentState.isSolidRender(level, pos)) {
                        // Still track for entity containment but don't modify
                        prisonBlocks.add(pos);
                        continue;
                    }
                    
                    // Store original state for restoration
                    originalBlocks.put(pos, currentState);
                    prisonBlocks.add(pos);
                }
            }
        }
        
        if (prisonBlocks.isEmpty()) {
            return false;
        }
        
        // Track if ANY position uses vanilla water (for tick/cleanup logic)
        boolean anyVanillaWater = false;
        
        // Place water - use vanilla water for positions adjacent to existing water,
        // static water for positions in air (to prevent flowing messes)
        BlockState staticWater = ModFluidBlocks.STATIC_WATER_BLOCK.get().defaultBlockState();
        BlockState vanillaWater = Blocks.WATER.defaultBlockState();
        
        for (BlockPos pos : prisonBlocks) {
            // Only place water if we stored the original (meaning it wasn't already water or solid)
            if (originalBlocks.containsKey(pos)) {
                if (positionsNeedingVanillaWater.contains(pos)) {
                    // Adjacent to existing water - use vanilla to blend in
                    level.setBlock(pos, vanillaWater, Block.UPDATE_CLIENTS);
                    anyVanillaWater = true;
                } else {
                    // In air/not adjacent to water - use static water to prevent flow
                    level.setBlock(pos, staticWater, Block.UPDATE_CLIENTS);
                }
            }
        }
        
        // Play water sound
        level.playSound(null, center, SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.0f, 0.8f);
        
        Vec3 prisonCenter = Vec3.atCenterOf(center.above()); // Center of middle layer
        
        // If we have an initial target, pull them toward center (not teleport)
        if (initialTarget != null) {
            TRAPPED_ENTITIES.add(initialTarget.getUUID());
        }
        
        // Prison bounds - the actual 3x3x3 water area
        AABB prisonBounds = new AABB(
            center.getX() - 1.5, center.getY() - 0.5, center.getZ() - 1.5,
            center.getX() + 1.5, center.getY() + 2.5, center.getZ() + 1.5
        );
        
        // Pull zone - entities in this area get slowly pulled toward center (0.25 blocks outside prison)
        AABB pullZone = prisonBounds.inflate(0.25);
        
        PrisonData prison = new PrisonData(
            level,
            center, 
            prisonBlocks,
            originalBlocks, 
            caster.getUUID(),
            prisonCenter,
            prisonBounds,
            pullZone,
            PRISON_DURATION_TICKS,
            anyVanillaWater  // Track if any positions use vanilla water
        );
        
        // Add to player's prison list
        List<PrisonData> playerPrisons = PLAYER_PRISONS.computeIfAbsent(caster.getUUID(), k -> new ArrayList<>());
        playerPrisons.add(prison);
        
        // Add to active queue for processing
        ACTIVE_PRISONS_QUEUE.add(prison);
        
        return true;
    }
    
    private static void tickPrison(PrisonData prison) {
        if (!prison.isActive || prison.level == null) {
            return;
        }
        
        ServerLevel level = prison.level;
        BlockPos center = prison.center;
        
        // Only maintain water blocks if using static water (not in/near vanilla water)
        if (!prison.usingVanillaWater) {
            BlockState staticWater = ModFluidBlocks.STATIC_WATER_BLOCK.get().defaultBlockState();
            
            // Maintain static water blocks inside prison - only replace if destroyed
            for (BlockPos pos : prison.waterBlocks) {
                // Only maintain blocks we actually placed (stored in originalBlocks)
                if (!prison.originalBlocks.containsKey(pos)) {
                    continue;
                }
                BlockState current = level.getBlockState(pos);
                if (!current.is(ModFluidBlocks.STATIC_WATER_BLOCK.get())) {
                    level.setBlock(pos, staticWater, Block.UPDATE_CLIENTS);
                }
            }
            
            // AGGRESSIVE FLOW CLEANUP - Remove ANY water that escaped the prison bounds
            // Check 3-block radius around prison for any flowing water
            for (int x = -4; x <= 4; x++) {
                for (int y = -2; y <= 5; y++) {
                    for (int z = -4; z <= 4; z++) {
                        BlockPos checkPos = center.offset(x, y, z);
                        
                        // Skip positions that are part of the prison itself
                        if (prison.waterBlocks.contains(checkPos)) {
                            continue;
                        }
                        
                        // If there's vanilla water here that we didn't place, it might have escaped
                        // Only clean up if we have an original block stored for this position
                        BlockState state = level.getBlockState(checkPos);
                        if (state.is(Blocks.WATER) && prison.originalBlocks.containsKey(checkPos)) {
                            BlockState original = prison.originalBlocks.get(checkPos);
                            level.setBlock(checkPos, original, Block.UPDATE_CLIENTS);
                        }
                    }
                }
            }
        }
        // If using vanilla water, we don't maintain blocks - they flow naturally
        
        // Entity handling - PULL zone and complete immobilization inside
        Vec3 prisonCenter = prison.prisonCenter;
        
        // Check pull zone for entities to slowly pull in
        List<LivingEntity> entitiesInPullZone = level.getEntitiesOfClass(LivingEntity.class, prison.pullZone);
        
        for (LivingEntity entity : entitiesInPullZone) {
            // Skip the caster - they are immune to their own prison
            if (entity.getUUID().equals(prison.casterUUID)) {
                continue;
            }
            
            boolean insidePrison = prison.prisonBounds.contains(entity.position());
            boolean wasTrapped = TRAPPED_ENTITIES.contains(entity.getUUID());
            
            if (insidePrison) {
                // INSIDE PRISON: Complete immobilization - no movement, no swim, no jump
                TRAPPED_ENTITIES.add(entity.getUUID());
                
                // Calculate direction to center
                double dx = prisonCenter.x - entity.getX();
                double dy = prisonCenter.y - entity.getY();
                double dz = prisonCenter.z - entity.getZ();
                double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                
                // FORCE position toward center - override any floating/swimming
                if (distance > 0.3) {
                    // Strong pull toward center - override natural buoyancy
                    entity.setDeltaMovement(dx * 0.15, dy * 0.15, dz * 0.15);
                } else {
                    // At center - COMPLETE FREEZE by directly setting position
                    entity.setDeltaMovement(Vec3.ZERO);
                    // Snap to center position to prevent any drift
                    entity.setPos(prisonCenter.x, prisonCenter.y - 0.5, prisonCenter.z);
                }
                
                // Force position sync to client
                entity.hurtMarked = true;
                
                // Prevent ALL movement - override any player input and buoyancy
                entity.setNoGravity(true);
                entity.setSwimming(false);
                entity.resetFallDistance();
                
                // Apply drowning damage periodically
                long gameTime = level.getGameTime();
                if (gameTime % SUFFOCATION_INTERVAL == 0) {
                    entity.hurt(level.damageSources().drown(), SUFFOCATION_DAMAGE);
                }
            } else if (wasTrapped) {
                // Entity was trapped but somehow escaped - strong pull back
                double dx = prisonCenter.x - entity.getX();
                double dy = prisonCenter.y - entity.getY();
                double dz = prisonCenter.z - entity.getZ();
                double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                
                // Strong pull force to drag them back
                if (distance > 0) {
                    entity.setDeltaMovement(dx * 0.3, dy * 0.3, dz * 0.3);
                    entity.hurtMarked = true;
                }
            } else {
                // Entity is in PULL ZONE but not inside yet - slow pull toward prison
                double dx = prisonCenter.x - entity.getX();
                double dy = prisonCenter.y - entity.getY();
                double dz = prisonCenter.z - entity.getZ();
                double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                
                // Slow pull force - gets stronger as they get closer
                if (distance > 0.5) {
                    double pullStrength = 0.08; // Gentle pull
                    Vec3 pullVelocity = new Vec3(dx / distance * pullStrength, dy / distance * pullStrength, dz / distance * pullStrength);
                    
                    // Add to existing velocity (don't override completely)
                    Vec3 currentVel = entity.getDeltaMovement();
                    entity.setDeltaMovement(currentVel.add(pullVelocity));
                    entity.hurtMarked = true;
                }
            }
        }
        
        // Re-enable gravity for entities that left the area completely
        AABB checkArea = prison.pullZone.inflate(2.0);
        for (UUID trappedUUID : new HashSet<>(TRAPPED_ENTITIES)) {
            // Find if entity is still in the area
            List<LivingEntity> allEntities = level.getEntitiesOfClass(LivingEntity.class, checkArea, 
                e -> e.getUUID().equals(trappedUUID));
            if (allEntities.isEmpty()) {
                // Entity no longer in area - clean up gravity flag
                // (We can't restore directly without entity reference, they'll fix on their own)
                TRAPPED_ENTITIES.remove(trappedUUID);
            }
        }
    }
    
    private static void removePrison(PrisonData prison) {
        prison.isActive = false;
        
        if (prison.level == null) {
            return;
        }
        
        ServerLevel level = prison.level;
        
        // Restore all original blocks (only the ones we actually changed)
        for (Map.Entry<BlockPos, BlockState> entry : prison.originalBlocks.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState originalState = entry.getValue();
            BlockState currentState = level.getBlockState(pos);
            
            // Check what type of water we need to clean up
            if (prison.usingVanillaWater) {
                // If using vanilla water, only restore blocks we explicitly changed
                if (currentState.is(Blocks.WATER) && prison.waterBlocks.contains(pos)) {
                    level.setBlock(pos, originalState, 3);
                }
            } else {
                // Remove if it's still static water
                if (currentState.is(ModFluidBlocks.STATIC_WATER_BLOCK.get())) {
                    level.setBlock(pos, originalState, 3);
                }
            }
        }
        
        // Also cleanup any remaining static water in the prison area (if not using vanilla)
        if (!prison.usingVanillaWater) {
            for (BlockPos pos : prison.waterBlocks) {
                if (!prison.originalBlocks.containsKey(pos)) {
                    continue; // Don't touch blocks we didn't change
                }
                BlockState currentState = level.getBlockState(pos);
                if (currentState.is(ModFluidBlocks.STATIC_WATER_BLOCK.get())) {
                    BlockState original = prison.originalBlocks.getOrDefault(pos, Blocks.AIR.defaultBlockState());
                    level.setBlock(pos, original, 3);
                }
            }
        }
        
        // Release trapped entities in this prison area and restore gravity
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, prison.prisonBounds.inflate(1.0));
        for (LivingEntity entity : entities) {
            TRAPPED_ENTITIES.remove(entity.getUUID());
            entity.setNoGravity(false); // Restore gravity
        }
        
        // Remove from player's prison list
        List<PrisonData> playerPrisons = PLAYER_PRISONS.get(prison.casterUUID);
        if (playerPrisons != null) {
            playerPrisons.remove(prison);
            if (playerPrisons.isEmpty()) {
                PLAYER_PRISONS.remove(prison.casterUUID);
            }
        }
        
        // Play water dissipate sound
        level.playSound(null, prison.center, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.5f, 1.2f);
    }
    
    private EntityHitResult findEntityOnPath(Level level, Player player, Vec3 start, Vec3 end, AABB searchBox) {
        List<Entity> entities = level.getEntities(player, searchBox, e -> e instanceof LivingEntity && e != player && e.isAlive());
        
        Entity closest = null;
        double closestDist = Double.MAX_VALUE;
        Vec3 hitPos = null;
        
        for (Entity entity : entities) {
            AABB entityBB = entity.getBoundingBox().inflate(0.3);
            Optional<Vec3> hit = entityBB.clip(start, end);
            
            if (hit.isPresent()) {
                double dist = start.distanceToSqr(hit.get());
                if (dist < closestDist) {
                    closest = entity;
                    closestDist = dist;
                    hitPos = hit.get();
                }
            }
        }
        
        return closest != null ? new EntityHitResult(closest, hitPos) : null;
    }
    
    /**
     * Check if an entity is currently trapped in a water prison.
     * This can be used by other systems to prevent movement.
     */
    public static boolean isEntityTrapped(UUID entityUUID) {
        return TRAPPED_ENTITIES.contains(entityUUID);
    }
    
    /**
     * Data class to track an active water prison.
     */
    private static class PrisonData {
        final ServerLevel level;
        final BlockPos center;
        final Set<BlockPos> waterBlocks;
        final Map<BlockPos, BlockState> originalBlocks;
        final UUID casterUUID;
        final Vec3 prisonCenter;
        final AABB prisonBounds;
        final AABB pullZone;
        final boolean usingVanillaWater; // True if prison is in/near water, uses vanilla water
        int ticksRemaining;
        boolean isActive = true;
        
        PrisonData(ServerLevel level, BlockPos center, Set<BlockPos> waterBlocks,
                   Map<BlockPos, BlockState> originalBlocks, 
                   UUID casterUUID, Vec3 prisonCenter, AABB prisonBounds, AABB pullZone, 
                   int duration, boolean usingVanillaWater) {
            this.level = level;
            this.center = center;
            this.waterBlocks = waterBlocks;
            this.originalBlocks = originalBlocks;
            this.casterUUID = casterUUID;
            this.prisonCenter = prisonCenter;
            this.prisonBounds = prisonBounds;
            this.pullZone = pullZone;
            this.ticksRemaining = duration;
            this.usingVanillaWater = usingVanillaWater;
        }
    }
}
