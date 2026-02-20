package net.narutoxboruto.client.model;

import net.minecraft.resources.ResourceLocation;
import net.narutoxboruto.entities.jutsus.SharkBombEntity;
import net.narutoxboruto.main.Main;
import software.bernie.geckolib.model.GeoModel;

public class SharkBombModel extends GeoModel<SharkBombEntity> {
    
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(
            Main.MOD_ID, "geo/shark_bomb.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Main.MOD_ID, "textures/entity/shark_bomb.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(
            Main.MOD_ID, "animations/shark_bomb.animation.json");
    
    @Override
    public ResourceLocation getModelResource(SharkBombEntity entity) {
        return MODEL;
    }
    
    @Override
    public ResourceLocation getTextureResource(SharkBombEntity entity) {
        return TEXTURE;
    }
    
    @Override
    public ResourceLocation getAnimationResource(SharkBombEntity entity) {
        return ANIMATION;
    }
}
