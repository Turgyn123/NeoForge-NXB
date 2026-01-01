package net.narutoxboruto.client.renderer.item;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * Client extensions for Kiba sword to use custom BEWLR for lightning effects.
 */
public class KibaClientExtension implements IClientItemExtensions {
    
    public static final KibaClientExtension INSTANCE = new KibaClientExtension();
    
    private KibaClientExtension() {}
    
    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        return KibaSwordRenderer.INSTANCE;
    }
}
