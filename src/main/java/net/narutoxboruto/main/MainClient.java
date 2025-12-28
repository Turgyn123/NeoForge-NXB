package net.narutoxboruto.main;

import net.minecraft.client.renderer.entity.EntityRenderers;
import net.narutoxboruto.client.model.FireBallModel;
import net.narutoxboruto.client.renderer.entity.*;
import net.narutoxboruto.client.renderer.shinobi.AbstractShinobiRender;
import net.narutoxboruto.entities.ModEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
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
    static void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code
        event.enqueueWork(() -> {
            // your client, only entity renderers, packet receivers, etc.

            EntityRenderers.register(ModEntities.SHURIKEN.get(), ShurikenRenderer::new);
            EntityRenderers.register(ModEntities.KUNAI.get(), ThrowableWeaponRenderer::new);
            EntityRenderers.register(ModEntities.EXPLOSIVE_KUNAI.get(), ThrowableWeaponRenderer::new);
            EntityRenderers.register(ModEntities.POISON_SENBON.get(), PoisonSenbonRenderer::new);
            EntityRenderers.register(ModEntities.SENBON.get(), SenbonRenderer::new);
            EntityRenderers.register(ModEntities.FUMA_SHURIKEN.get(), FumaShurikenRenderer::new);
            EntityRenderers.register(ModEntities.FIRE_BALL.get(), FireBallRenderer::new);

            EntityRenderers.register(ModEntities.JINPACHI_MUNASHI.get(),
                    (ctx) -> new AbstractShinobiRender(ctx, "jinpachi_munashi", true));
            EntityRenderers.register(ModEntities.KISAME_HOSHIGAKI.get(),
                    (ctx) -> new AbstractShinobiRender(ctx, "kisame_hoshigaki"));
            EntityRenderers.register(ModEntities.ZABUZA_MOMOCHI.get(),
                    (ctx) -> new AbstractShinobiRender(ctx, "zabuza_momochi"));
        });
    }
}
