package net.narutoxboruto.fluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidType;

import java.util.Optional;

/**
 * Static water fluid - water that doesn't flow or spread.
 * Has all the properties of water (drowning, swimming, water overlay)
 * but stays perfectly still.
 */
public class StaticWaterFluid extends Fluid {
    
    @Override
    public FluidType getFluidType() {
        return ModFluids.STATIC_WATER_TYPE.get();
    }
    
    @Override
    public Item getBucket() {
        // No bucket for this - it's a jutsu-only fluid
        return Items.AIR;
    }
    
    @Override
    protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, 
                                         Fluid fluid, Direction direction) {
        // Don't allow replacement by other fluids
        return false;
    }
    
    @Override
    protected net.minecraft.world.phys.Vec3 getFlow(BlockGetter level, BlockPos pos, FluidState state) {
        // No flow - completely still
        return net.minecraft.world.phys.Vec3.ZERO;
    }
    
    @Override
    public int getTickDelay(LevelReader level) {
        // No tick delay needed since we don't flow
        return Integer.MAX_VALUE;
    }
    
    @Override
    protected float getExplosionResistance() {
        // Same as water
        return 100.0F;
    }
    
    @Override
    public float getHeight(FluidState state, BlockGetter level, BlockPos pos) {
        // Full block height
        return 1.0F;
    }
    
    @Override
    public float getOwnHeight(FluidState state) {
        return 1.0F;
    }
    
    @Override
    protected BlockState createLegacyBlock(FluidState state) {
        // This fluid uses its own block
        return ModFluidBlocks.STATIC_WATER_BLOCK.get().defaultBlockState();
    }
    
    @Override
    public boolean isSource(FluidState state) {
        // Always a source - no flowing variant
        return true;
    }
    
    @Override
    public int getAmount(FluidState state) {
        // Full level (8 is max for water)
        return 8;
    }
    
    @Override
    public boolean isSame(Fluid fluid) {
        return fluid == this || fluid == ModFluids.STATIC_WATER.get();
    }
    
    @Override
    public VoxelShape getShape(FluidState state, BlockGetter level, BlockPos pos) {
        // Full block shape since we're always full source water
        return Shapes.block();
    }
    
    @Override
    public Optional<SoundEvent> getPickupSound() {
        return Optional.of(SoundEvents.BUCKET_FILL);
    }
}
