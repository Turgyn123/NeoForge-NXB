package net.narutoxboruto.main;

import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.levelgen.Heightmap;
import net.narutoxboruto.attachments.MainAttachment;
import net.narutoxboruto.blocks.ModBlocks;
import net.narutoxboruto.blocks.entity.ModBlockEntities;
import net.narutoxboruto.client.overlay.ModHudOverlays;
import net.narutoxboruto.client.renderer.entity.*;
import net.narutoxboruto.client.renderer.shinobi.AbstractShinobiRender;
import net.narutoxboruto.command.argument.*;
import net.narutoxboruto.effect.ModEffects;
import net.narutoxboruto.entities.ModEntities;
import net.narutoxboruto.entities.shinobis.JinpachiMunashi;
import net.narutoxboruto.entities.shinobis.KizameHoshigaki;
import net.narutoxboruto.entities.shinobis.ZabuzaMomochi;
import net.narutoxboruto.events.*;
import net.narutoxboruto.fluids.ModFluidBlocks;
import net.narutoxboruto.fluids.ModFluids;
import net.narutoxboruto.items.ModItems;
import net.narutoxboruto.items.ModTab;
import net.narutoxboruto.menu.ModMenuTypes;
import net.narutoxboruto.networking.ModPacketHandler;
import net.narutoxboruto.particles.ModParticles;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import static net.narutoxboruto.entities.ModEntities.*;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(Main.MOD_ID)
public class Main {
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "narutoxboruto";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    public Main(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.register(AttachmentEvents.class);
        NeoForge.EVENT_BUS.register(Events.class);
        NeoForge.EVENT_BUS.register(StatEvents.class);
        NeoForge.EVENT_BUS.register(CommandEvents.class);
        NeoForge.EVENT_BUS.register(JutsuItemEvents.class);
        modEventBus.register(SpawnEvents.class);
        modEventBus.addListener(this::attributes);

        ModBlockEntities.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModEffects.register(modEventBus);
        ModFluids.register(modEventBus);
        ModFluidBlocks.register(modEventBus);
        ModTab.register(modEventBus);
        ModItems.register(modEventBus);
        ModEntities.register(modEventBus);
        MainAttachment.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModParticles.register(modEventBus);
        modEventBus.register(ModPacketHandler.class);
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, RenderGuiEvent.Post.class, this::onRenderGui);
        NeoForge.EVENT_BUS.register(this);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
    private void onRenderGui(RenderGuiEvent.Post event) {
        // Copy the render code here or call static method
        ModHudOverlays.onRenderGui(event);
    }
    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> { });
        ArgumentTypeInfos.registerByClass(AffiliationArgument.class,
                SingletonArgumentInfo.contextFree(AffiliationArgument::affiliation));
        ArgumentTypeInfos.registerByClass(ClanArgument.class, SingletonArgumentInfo.contextFree(ClanArgument::clan));
        ArgumentTypeInfos.registerByClass(RankArgument.class, SingletonArgumentInfo.contextFree(RankArgument::rank));
        ArgumentTypeInfos.registerByClass(ShinobiStatArgument.class,
                SingletonArgumentInfo.contextFree(ShinobiStatArgument::shinobiStat));
        ArgumentTypeInfos.registerByClass(InfoArgument.class, SingletonArgumentInfo.contextFree(InfoArgument::info));
    }

    private void attributes(EntityAttributeCreationEvent event) {
       // event.put(AMEYURI_RINGO.get(), AmeyuriRingo.setAttribute());
       // event.put(CHOJURO.get(), Chojuro.setAttribute());
       // event.put(JININ_AKEBINO.get(), JininAkebino.setAttribute());
        event.put(JINPACHI_MUNASHI.get(), JinpachiMunashi.setAttribute());
        event.put(KISAME_HOSHIGAKI.get(), KizameHoshigaki.setAttribute());
       // event.put(KURIARARE_KUSHIMARU.get(), KuriarareKushimaru.setAttribute());
        event.put(ZABUZA_MOMOCHI.get(), ZabuzaMomochi.setAttribute());
       // event.put(SHUKAKU.get(), Shukaku.setAttribute());
       // event.put(MATABI.get(), Matabi.setAttribute());
       // event.put(ISOBU.get(), Isobu.setAttribute());
       // event.put(SON_GOKU.get(), SonGoku.setAttribute());
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }
}
