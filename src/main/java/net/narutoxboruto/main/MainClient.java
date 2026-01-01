package net.narutoxboruto.main;

import net.minecraft.client.renderer.entity.EntityRenderers;
import net.narutoxboruto.client.gui.JutsuStorageScreen;
import net.narutoxboruto.client.model.FireBallModel;
import net.narutoxboruto.client.renderer.entity.*;
import net.narutoxboruto.client.renderer.item.KibaClientExtension;
import net.narutoxboruto.client.renderer.shinobi.AbstractShinobiRender;
import net.narutoxboruto.entities.ModEntities;
import net.narutoxboruto.fluids.ModFluidBlocks;
import net.narutoxboruto.fluids.ModFluids;
import net.narutoxboruto.fluids.StaticWaterClientExtension;
import net.narutoxboruto.items.ModItems;
import net.narutoxboruto.menu.ModMenuTypes;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = Main.MOD_ID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = Main.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class MainClient {
    public MainClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
    
    @SubscribeEvent
    static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(FireBallModel.LAYER_LOCATION, FireBallModel::createBodyLayer);
    }

    @SubscribeEvent
    static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.JUTSU_STORAGE.get(), JutsuStorageScreen::new);
    }
    
    @SubscribeEvent
    static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        // Register static water fluid rendering with water textures
        event.registerFluidType(StaticWaterClientExtension.INSTANCE, ModFluids.STATIC_WATER_TYPE.get());
        
        // Register custom renderer for Kiba sword (lightning effects)
        event.registerItem(KibaClientExtension.INSTANCE, ModItems.KIBA.get());
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code
        event.enqueueWork(() -> {
            // Register static water block as translucent for semi-transparent rendering like vanilla water
            ItemBlockRenderTypes.setRenderLayer(ModFluids.STATIC_WATER.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModFluidBlocks.STATIC_WATER_BLOCK.get(), RenderType.translucent());
            
            // your client, only entity renderers, packet receivers, etc.

            EntityRenderers.register(ModEntities.SHURIKEN.get(), ShurikenRenderer::new);
            EntityRenderers.register(ModEntities.KUNAI.get(), ThrowableWeaponRenderer::new);
            EntityRenderers.register(ModEntities.EXPLOSIVE_KUNAI.get(), ThrowableWeaponRenderer::new);
            EntityRenderers.register(ModEntities.POISON_SENBON.get(), PoisonSenbonRenderer::new);
            EntityRenderers.register(ModEntities.SENBON.get(), SenbonRenderer::new);
            EntityRenderers.register(ModEntities.FUMA_SHURIKEN.get(), FumaShurikenRenderer::new);
            EntityRenderers.register(ModEntities.FIRE_BALL.get(), FireBallRenderer::new);
            EntityRenderers.register(ModEntities.LIGHTNING_ARC.get(), LightningArcRenderer::new);

            EntityRenderers.register(ModEntities.JINPACHI_MUNASHI.get(),
                    (ctx) -> new AbstractShinobiRender(ctx, "jinpachi_munashi", true));
            EntityRenderers.register(ModEntities.KISAME_HOSHIGAKI.get(),
                    (ctx) -> new AbstractShinobiRender(ctx, "kisame_hoshigaki"));
            EntityRenderers.register(ModEntities.ZABUZA_MOMOCHI.get(),
                    (ctx) -> new AbstractShinobiRender(ctx, "zabuza_momochi"));
        });
    }
}
