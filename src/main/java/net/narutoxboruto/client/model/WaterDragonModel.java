package net.narutoxboruto.client.model;

import net.minecraft.resources.ResourceLocation;
import net.narutoxboruto.entities.jutsus.WaterDragonEntity;
import net.narutoxboruto.main.Main;
import software.bernie.geckolib.model.GeoModel;

public class WaterDragonModel extends GeoModel<WaterDragonEntity> {
    
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(
            Main.MOD_ID, "geo/water_dragon.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Main.MOD_ID, "textures/entity/water_dragon.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(
            Main.MOD_ID, "animations/water_dragon.animation.json");
    
    @Override
    public ResourceLocation getModelResource(WaterDragonEntity entity) {
        return MODEL;
    }
    
    @Override
    public ResourceLocation getTextureResource(WaterDragonEntity entity) {
        return TEXTURE;
    }
    
    @Override
    public ResourceLocation getAnimationResource(WaterDragonEntity entity) {
        return ANIMATION;
    }
}
