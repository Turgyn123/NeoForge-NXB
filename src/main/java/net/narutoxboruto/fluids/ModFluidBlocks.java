package net.narutoxboruto.fluids;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.narutoxboruto.main.Main;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModFluidBlocks {
    
    public static final DeferredRegister<Block> BLOCKS = 
            DeferredRegister.create(net.minecraft.core.registries.Registries.BLOCK, Main.MOD_ID);
    
    public static final Supplier<StaticWaterBlock> STATIC_WATER_BLOCK = BLOCKS.register("static_water_block",
            () -> new StaticWaterBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.WATER)
                            .replaceable()
                            .noCollission()
                            .strength(100.0F)
                            .pushReaction(PushReaction.DESTROY)
                            .noLootTable()
                            .liquid()
            ));
    
    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
