package net.narutoxboruto.fluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Static water block - a block that looks and acts like water but never flows.
 * Used for the Water Prison jutsu to contain water without it spreading.
 * 
 * This extends Block directly (not LiquidBlock) to avoid needing a FlowingFluid.
 */
public class StaticWaterBlock extends Block {
    
    public StaticWaterBlock(Properties properties) {
        super(properties);
    }
    
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // No collision - entities can pass through like water
        return Shapes.empty();
    }
    
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // Full block outline
        return Shapes.block();
    }
    
    @Override
    protected FluidState getFluidState(BlockState state) {
        // Return our static water fluid state
        return ModFluids.STATIC_WATER.get().defaultFluidState();
    }
    
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        // Render as invisible - the fluid handles rendering
        return RenderShape.INVISIBLE;
    }
    
    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true; // Light passes through like water
    }
    
    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        // Allow pathfinding through water
        return type == PathComputationType.WATER;
    }
    
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Do nothing - don't spread or update
    }
    
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Do nothing - no random updates
    }
    
    @Override
    protected BlockState updateShape(BlockState state, Direction facing, BlockState facingState,
                                      LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
        // Don't update based on neighbors - stay exactly as placed
        return state;
    }
    
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, 
                                    Block block, BlockPos fromPos, boolean isMoving) {
        // Ignore neighbor changes - don't flow or react
    }
    
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        // Don't schedule ticks or trigger updates
    }
    
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        // Standard removal
        super.onRemove(state, level, pos, newState, isMoving);
    }
    
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        // Mark entity as in water for proper effects (drowning, etc)
        entity.resetFallDistance();
        // WaterPrison tick handles the actual movement control
    }
}
