package net.narutoxboruto.fluids;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.Fluid;
import net.narutoxboruto.main.Main;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ModFluids {
    
    public static final DeferredRegister<Fluid> FLUIDS = 
            DeferredRegister.create(Registries.FLUID, Main.MOD_ID);
    
    public static final DeferredRegister<FluidType> FLUID_TYPES = 
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Main.MOD_ID);
    
    // Static Water - water that doesn't flow
    public static final Supplier<FluidType> STATIC_WATER_TYPE = FLUID_TYPES.register("static_water",
            () -> new StaticWaterFluidType());
    
    public static final Supplier<StaticWaterFluid> STATIC_WATER = FLUIDS.register("static_water",
            () -> new StaticWaterFluid());
    
    public static void register(IEventBus eventBus) {
        FLUIDS.register(eventBus);
        FLUID_TYPES.register(eventBus);
    }
}
