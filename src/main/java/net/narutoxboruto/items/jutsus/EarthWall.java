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
public class EarthWall extends AbstractNatureReleaseItem {

    // Wall dimensions
    private static final int WALL_WIDTH = 7;   // 7 blocks wide
    private static final int WALL_HEIGHT = 3;  // 3 blocks tall
    private static final int WALL_DEPTH = 2;   // 2 blocks deep

    public EarthWall(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public String getSelectedJutsu(ServerPlayer serverPlayer) {
        return "earth_wall";
    }

    @Override
    protected String getRequiredRelease() {
        return "earth";
    }

    @Override
    protected int getJutsuChakraCost(ServerPlayer serverPlayer) {
        return 15;
    }

    @Override
    protected int getCooldownTicks() {
        return 100; // 5 seconds cooldown
    }

    @Override
    protected boolean castJutsu(ServerPlayer serverPlayer, Level level) {
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
     */
    private void buildWall(Level level, BlockPos origin, Direction facing) {
        int halfWidth = WALL_WIDTH / 2;

        for (int height = 1; height <= WALL_HEIGHT; height++) {
            for (int width = -halfWidth; width <= halfWidth; width++) {
                for (int depth = 0; depth < WALL_DEPTH; depth++) {
                    BlockPos wallPos = calculateWallBlockPos(origin, facing, width, height, depth);

                    if (canPlaceWallBlock(level, wallPos, height)) {
                        BlockState blockToPlace = getWallBlockState(level, origin);
                        level.setBlock(wallPos, blockToPlace, 3);
                    }
                }
            }
        }
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
     */
    private boolean canPlaceWallBlock(Level level, BlockPos pos, int height) {
        // Must be air or replaceable
        if (!isAir(level, pos) && !level.getBlockState(pos).canBeReplaced()) {
            return false;
        }
        // Don't place if there's a block entity
        if (isBlockEntity(level, pos)) {
            return false;
        }
        return true;
    }

    /**
     * Get the block state to use for the wall (copies from ground or uses cobblestone).
     */
    private BlockState getWallBlockState(Level level, BlockPos origin) {
        BlockState groundState = level.getBlockState(origin);

        // If it's an ore or special block, use cobblestone instead
        if (isOre(level, origin) || groundState.getDestroySpeed(level, origin) < 0) {
            return Blocks.COBBLESTONE.defaultBlockState();
        }

        // Use the ground block type
        return groundState;
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
