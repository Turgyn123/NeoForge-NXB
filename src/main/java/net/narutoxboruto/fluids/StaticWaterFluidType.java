package net.narutoxboruto.fluids;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidType;

/**
 * Fluid type for static water - looks and behaves like water but doesn't flow.
 * Uses water textures, water sounds, and water physics (drowning, swimming).
 */
public class StaticWaterFluidType extends FluidType {
    
    // Use vanilla water textures
    public static final ResourceLocation STILL_TEXTURE = ResourceLocation.withDefaultNamespace("block/water_still");
    public static final ResourceLocation FLOWING_TEXTURE = ResourceLocation.withDefaultNamespace("block/water_flow");
    public static final ResourceLocation OVERLAY_TEXTURE = ResourceLocation.withDefaultNamespace("block/water_overlay");
    
    public StaticWaterFluidType() {
        super(Properties.create()
                .descriptionId("fluid.narutoxboruto.static_water")
                .canSwim(true)           // Allows swimming
                .canDrown(true)          // Causes drowning
                .canPushEntity(false)    // No current to push entities
                .canHydrate(true)        // Can hydrate farmland
                .supportsBoating(true)   // Boats work on it
                .canExtinguish(true)     // Puts out fire
                .density(1000)           // Same as water
                .viscosity(1000)         // Same as water
                .temperature(300)        // Room temperature
                .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)
        );
    }
}
