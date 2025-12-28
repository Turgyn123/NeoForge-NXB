package net.narutoxboruto.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.narutoxboruto.main.Main;

/**
 * 3D model for the FireBall entity based on Blockbench export.
 */
public class FireBallModel extends Model {
    
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        ResourceLocation.fromNamespaceAndPath(Main.MOD_ID, "fire_ball"), "main"
    );
    
    private final ModelPart root;
    
    public FireBallModel(ModelPart root) {
        super(RenderType::entityTranslucentEmissive);
        this.root = root;
    }
    
    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        
        // Main cube - center of the fireball (16x16x16)
        partdefinition.addOrReplaceChild("cube0", CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-8.0F, -8.0F, -8.0F, 16.0F, 16.0F, 16.0F, new CubeDeformation(0.0F)),
            PartPose.offset(0.0F, 0.0F, 0.0F));
        
        // Extended cube along Z axis (12x12x20)
        partdefinition.addOrReplaceChild("cube1", CubeListBuilder.create()
            .texOffs(0, 32)
            .addBox(-6.0F, -6.0F, -10.0F, 12.0F, 12.0F, 20.0F, new CubeDeformation(0.0F)),
            PartPose.offset(0.0F, 0.0F, 0.0F));
        
        // Extended cube along X axis (20x12x12)
        partdefinition.addOrReplaceChild("cube2", CubeListBuilder.create()
            .texOffs(0, 64)
            .addBox(-10.0F, -6.0F, -6.0F, 20.0F, 12.0F, 12.0F, new CubeDeformation(0.0F)),
            PartPose.offset(0.0F, 0.0F, 0.0F));
        
        // Extended cube along Y axis (12x20x12)
        partdefinition.addOrReplaceChild("cube3", CubeListBuilder.create()
            .texOffs(64, 0)
            .addBox(-6.0F, -10.0F, -6.0F, 12.0F, 20.0F, 12.0F, new CubeDeformation(0.0F)),
            PartPose.offset(0.0F, 0.0F, 0.0F));
        
        // Inner core along Z (8x8x22)
        partdefinition.addOrReplaceChild("cube4", CubeListBuilder.create()
            .texOffs(64, 32)
            .addBox(-4.0F, -4.0F, -11.0F, 8.0F, 8.0F, 22.0F, new CubeDeformation(0.0F)),
            PartPose.offset(0.0F, 0.0F, 0.0F));
        
        // Inner core along X (22x8x8)
        partdefinition.addOrReplaceChild("cube5", CubeListBuilder.create()
            .texOffs(64, 62)
            .addBox(-11.0F, -4.0F, -4.0F, 22.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)),
            PartPose.offset(0.0F, 0.0F, 0.0F));
        
        // Inner core along Y (8x22x8)
        partdefinition.addOrReplaceChild("cube6", CubeListBuilder.create()
            .texOffs(0, 96)
            .addBox(-4.0F, -11.0F, -4.0F, 8.0F, 22.0F, 8.0F, new CubeDeformation(0.0F)),
            PartPose.offset(0.0F, 0.0F, 0.0F));
        
        return LayerDefinition.create(meshdefinition, 128, 128);
    }
    
    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, 
                               int packedOverlay, int color) {
        root.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
    
    public ModelPart getRoot() {
        return root;
    }
}
