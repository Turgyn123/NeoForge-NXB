package net.narutoxboruto.items.jutsus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import static net.narutoxboruto.util.BlockUtils.*;

/**
 * Earth Wall Jutsu - Creates a 7x3x2 wall of earth blocks in front of the player.
 * Requires Earth release affinity.
 */
public class EarthWall extends AbstractJutsuItem {

    // Wall dimensions
    private static final int WALL_WIDTH = 7;   // 7 blocks wide
    private static final int WALL_HEIGHT = 3;  // 3 blocks tall
    private static final int WALL_DEPTH = 2;   // 2 blocks deep

    public EarthWall(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public String getJutsuName() {
        return "Earth Wall";
    }

    @Override
    public String getRequiredRelease() {
        return "earth";
    }

    @Override
    public int getChakraCost() {
        return 5;
    }

    @Override
    public int getCooldownTicks() {
        return 100; // 5 seconds cooldown
    }

    @Override
    protected boolean executeJutsu(ServerPlayer serverPlayer, Level level) {
        // Get the block the player is looking at
        BlockPos targetBlock = getBlockPlayerIsLookingAt(serverPlayer, 10);

        if (targetBlock == null) {
            return false; // Not looking at any block
        }

        // Check if player is on solid ground (not in air or water)
        if (playerIsOnAir(level, serverPlayer) || serverPlayer.isUnderWater()) {
            return false;
        }

        // Check if target is solid ground
        BlockState targetState = level.getBlockState(targetBlock);
        if (!targetState.isSolidRender(level, targetBlock)) {
            return false;
        }

        // Build the wall
        Direction facing = serverPlayer.getDirection();
        buildWall(level, targetBlock, facing);

        // Play sound effect (placeholder)
        level.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                SoundEvents.STONE_PLACE, SoundSource.PLAYERS, 1.5F, 0.5F);

        // Spawn particles (placeholder)
        if (level instanceof ServerLevel serverLevel) {
            spawnWallParticles(serverLevel, targetBlock, facing);
        }

        return true;
    }

    /**
     * Build the earth wall based on player facing direction.
     * Uses blocks from underground - matching the block type beneath each wall position.
     */
    private void buildWall(Level level, BlockPos origin, Direction facing) {
        int halfWidth = WALL_WIDTH / 2;

        for (int height = 1; height <= WALL_HEIGHT; height++) {
            for (int width = -halfWidth; width <= halfWidth; width++) {
                for (int depth = 0; depth < WALL_DEPTH; depth++) {
                    BlockPos wallPos = calculateWallBlockPos(origin, facing, width, height, depth);

                    if (canPlaceWallBlock(level, wallPos)) {
                        // Get the ground position directly below this wall block
                        BlockPos groundPos = getGroundBelowWallPos(wallPos, origin, height);
                        BlockState blockToPlace = getWallBlockState(level, groundPos, origin);
                        level.setBlock(wallPos, blockToPlace, 3);
                    }
                }
            }
        }
    }

    /**
     * Get the ground position below where the wall block will be placed.
     */
    private BlockPos getGroundBelowWallPos(BlockPos wallPos, BlockPos origin, int height) {
        // The ground is at the wall's X/Z but at the origin's Y level (or below)
        return new BlockPos(wallPos.getX(), origin.getY(), wallPos.getZ());
    }

    /**
     * Calculate the position of a wall block based on direction.
     */
    private BlockPos calculateWallBlockPos(BlockPos origin, Direction facing, int width, int height, int depth) {
        return switch (facing) {
            case NORTH -> new BlockPos(origin.getX() + width, origin.getY() + height, origin.getZ() - depth);
            case SOUTH -> new BlockPos(origin.getX() - width, origin.getY() + height, origin.getZ() + depth);
            case EAST -> new BlockPos(origin.getX() + depth, origin.getY() + height, origin.getZ() + width);
            case WEST -> new BlockPos(origin.getX() - depth, origin.getY() + height, origin.getZ() - width);
            default -> origin.above(height);
        };
    }

    /**
     * Check if we can place a wall block at this position.
     * Allows replacing air, grass, plants, and other replaceable blocks.
     */
    private boolean canPlaceWallBlock(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        // Allow placing if air
        if (state.isAir()) {
            return true;
        }
        // Allow replacing grass, tall grass, flowers, etc.
        if (isReplaceable(state)) {
            return true;
        }
        // Don't place if there's a block entity
        if (isBlockEntity(level, pos)) {
            return false;
        }
        // Allow if the block is replaceable
        return state.canBeReplaced();
    }

    /**
     * Check if a block is a plant/grass type that should be replaced.
     */
    private boolean isReplaceable(BlockState state) {
        return state.is(Blocks.SHORT_GRASS) ||
               state.is(Blocks.TALL_GRASS) ||
               state.is(Blocks.FERN) ||
               state.is(Blocks.LARGE_FERN) ||
               state.is(Blocks.DEAD_BUSH) ||
               state.is(Blocks.SEAGRASS) ||
               state.is(Blocks.TALL_SEAGRASS) ||
               state.is(Blocks.VINE) ||
               state.is(Blocks.SNOW) ||
               state.is(Blocks.DANDELION) ||
               state.is(Blocks.POPPY) ||
               state.is(Blocks.BLUE_ORCHID) ||
               state.is(Blocks.ALLIUM) ||
               state.is(Blocks.AZURE_BLUET) ||
               state.is(Blocks.OXEYE_DAISY) ||
               state.is(Blocks.CORNFLOWER) ||
               state.is(Blocks.LILY_OF_THE_VALLEY);
    }

    /**
     * Get the block state to use for the wall.
     * Uses the block from underground at the given position, but ONLY if it's an earth/stone type.
     * If underground is air or non-earth block (wood, leaves, etc.), uses dirt.
     */
    private BlockState getWallBlockState(Level level, BlockPos groundPos, BlockPos fallbackOrigin) {
        BlockState groundState = level.getBlockState(groundPos);

        // If ground is air, search downward for a solid earth block
        if (groundState.isAir()) {
            BlockPos searchPos = groundPos.below();
            for (int i = 0; i < 5; i++) {
                BlockState belowState = level.getBlockState(searchPos);
                if (!belowState.isAir() && isSolidEarthBlock(belowState)) {
                    groundState = belowState;
                    break;
                }
                searchPos = searchPos.below();
            }
            // If still air, check the fallback origin
            if (groundState.isAir()) {
                BlockState fallbackState = level.getBlockState(fallbackOrigin);
                if (isSolidEarthBlock(fallbackState)) {
                    groundState = fallbackState;
                } else {
                    // Fallback is not an earth block, use dirt
                    return Blocks.DIRT.defaultBlockState();
                }
            }
        }

        // If it's grass block, use dirt instead (grass won't grow in a wall)
        if (groundState.is(Blocks.GRASS_BLOCK) || groundState.is(Blocks.PODZOL) || groundState.is(Blocks.MYCELIUM)) {
            return Blocks.DIRT.defaultBlockState();
        }

        // If it's an ore, use cobblestone instead
        if (isOre(level, groundPos)) {
            return Blocks.COBBLESTONE.defaultBlockState();
        }

        // If it's NOT an earth-type block (wood, leaves, etc.), use dirt
        if (!isSolidEarthBlock(groundState)) {
            return Blocks.DIRT.defaultBlockState();
        }

        // Use the ground block type (it's a valid earth block)
        return groundState;
    }

    /**
     * Check if the block is a solid earth-type block suitable for walls.
     */
    private boolean isSolidEarthBlock(BlockState state) {
        return state.is(Blocks.DIRT) ||
               state.is(Blocks.GRASS_BLOCK) ||
               state.is(Blocks.COARSE_DIRT) ||
               state.is(Blocks.ROOTED_DIRT) ||
               state.is(Blocks.PODZOL) ||
               state.is(Blocks.MYCELIUM) ||
               state.is(Blocks.STONE) ||
               state.is(Blocks.COBBLESTONE) ||
               state.is(Blocks.MOSSY_COBBLESTONE) ||
               state.is(Blocks.GRANITE) ||
               state.is(Blocks.DIORITE) ||
               state.is(Blocks.ANDESITE) ||
               state.is(Blocks.DEEPSLATE) ||
               state.is(Blocks.COBBLED_DEEPSLATE) ||
               state.is(Blocks.TUFF) ||
               state.is(Blocks.CALCITE) ||
               state.is(Blocks.DRIPSTONE_BLOCK) ||
               state.is(Blocks.SAND) ||
               state.is(Blocks.RED_SAND) ||
               state.is(Blocks.GRAVEL) ||
               state.is(Blocks.CLAY) ||
               state.is(Blocks.TERRACOTTA) ||
               state.is(Blocks.SANDSTONE) ||
               state.is(Blocks.RED_SANDSTONE) ||
               state.is(Blocks.MUD) ||
               state.is(Blocks.PACKED_MUD) ||
               state.is(Blocks.NETHERRACK) ||
               state.is(Blocks.BASALT) ||
               state.is(Blocks.BLACKSTONE) ||
               state.is(Blocks.END_STONE);
    }

    /**
     * Spawn particle effects for the wall creation.
     */
    private void spawnWallParticles(ServerLevel level, BlockPos origin, Direction facing) {
        int halfWidth = WALL_WIDTH / 2;

        for (int width = -halfWidth; width <= halfWidth; width++) {
            for (int height = 1; height <= WALL_HEIGHT; height++) {
                BlockPos particlePos = calculateWallBlockPos(origin, facing, width, height, 0);
                level.sendParticles(ParticleTypes.CLOUD,
                        particlePos.getX() + 0.5, particlePos.getY() + 0.5, particlePos.getZ() + 0.5,
                        3, 0.3, 0.3, 0.3, 0.02);
            }
        }
    }

    /**
     * Get the block the player is looking at within range.
     */
    private BlockPos getBlockPlayerIsLookingAt(ServerPlayer serverPlayer, int range) {
        Vec3 from = serverPlayer.getEyePosition(1.0F);
        Vec3 lookVec = serverPlayer.getViewVector(1.0F);
        Vec3 rayPath = lookVec.scale(range);
        Vec3 to = from.add(rayPath);
        ClipContext rayContext = new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, CollisionContext.empty());
        BlockHitResult rayHit = serverPlayer.level().clip(rayContext);
        if (rayHit.getType() == HitResult.Type.BLOCK) {
            return rayHit.getBlockPos();
        }
        return null;
    }

    /**
     * Check if player is standing on air.
     */
    private boolean playerIsOnAir(Level level, ServerPlayer serverPlayer) {
        return level.getBlockState(serverPlayer.getOnPos()).isAir();
    }
}
