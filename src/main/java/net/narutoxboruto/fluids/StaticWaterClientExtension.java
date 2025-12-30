package net.narutoxboruto.fluids;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

/**
 * Client-side rendering properties for static water fluid.
 * Uses vanilla water textures for seamless appearance.
 */
public class StaticWaterClientExtension implements IClientFluidTypeExtensions {
    
    public static final StaticWaterClientExtension INSTANCE = new StaticWaterClientExtension();
    
    // Vanilla water textures
    private static final ResourceLocation STILL_TEXTURE = ResourceLocation.withDefaultNamespace("block/water_still");
    private static final ResourceLocation FLOWING_TEXTURE = ResourceLocation.withDefaultNamespace("block/water_flow");
    private static final ResourceLocation OVERLAY_TEXTURE = ResourceLocation.withDefaultNamespace("block/water_overlay");
    
    // Water tint color with alpha for semi-transparency (ARGB format)
    // This is the default biome water color with some transparency
    private static final int WATER_TINT = 0xFF3F76E4; // Full alpha, same blue as vanilla water
    
    @Override
    public @NotNull ResourceLocation getStillTexture() {
        return STILL_TEXTURE;
    }
    
    @Override
    public @NotNull ResourceLocation getFlowingTexture() {
        // Use still texture since we don't flow, but provide flowing for compatibility
        return FLOWING_TEXTURE;
    }
    
    @Override
    public ResourceLocation getOverlayTexture() {
        return OVERLAY_TEXTURE;
    }
    
    @Override
    public int getTintColor() {
        // Return the water blue tint - this creates the semi-transparent blue look
        return WATER_TINT;
    }
    
    @Override
    public @NotNull Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level,
                                            int renderDistance, float darkenWorldAmount, Vector3f fluidFogColor) {
        // Water-like blue fog when submerged - same as vanilla water
        return new Vector3f(0.02f, 0.02f, 0.2f);
    }
    
    @Override
    public void modifyFogRender(Camera camera, FogRenderer.FogMode mode, float renderDistance,
                                 float partialTick, float nearDistance, float farDistance, FogShape shape) {
        // Apply water-like fog density
        RenderSystem.setShaderFogStart(-8.0f);
        RenderSystem.setShaderFogEnd(96.0f);
    }
}
