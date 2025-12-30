package net.narutoxboruto.items;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static net.narutoxboruto.blocks.ModBlocks.PAPER_BOMB;
import static net.narutoxboruto.items.ModItems.*;
import static net.narutoxboruto.main.Main.MOD_ID;

public class ModTab {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

   public static final DeferredHolder<CreativeModeTab, CreativeModeTab> THORWABLE = CREATIVE_MODE_TAB.register("throwable", () -> CreativeModeTab.builder()
           .title(Component.translatable("itemGroup.throwable")) //The language key for the title of your CreativeModeTab
           .withTabsBefore(CreativeModeTabs.COMBAT)
           .icon(() -> SHURIKEN.get().getDefaultInstance())
           .displayItems((parameters, output) -> {
               output.accept(SHURIKEN.get()); // Add the example item to the tab. For your own tabs, this method is preferred over the event
               output.accept(KUNAI.get());
               output.accept(ModItems.EXPLOSIVE_KUNAI.get());
               output.accept(ModItems.SENBON.get());
               output.accept(ModItems.POISON_SENBON.get());
               output.accept(ModItems.FUMA_SHURIKEN.get());
           }).build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SWORDS = CREATIVE_MODE_TAB.register("swords", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.swords")) //The language key for the title of your CreativeModeTab
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> SAMEHADA.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(SAMEHADA.get());
                output.accept(KUBIKIRIBOCHO.get());
                output.accept(SHIBUKI.get());
                output.accept(KIBA.get());
                output.accept(KABUTOWARI.get());
                //output.accept(NUIBARI.get());
            }).build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> NXB_TAB = CREATIVE_MODE_TAB.register("nxb_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.nxb_tab")) //The language key for the title of your CreativeModeTab
            .icon(() -> CHAKRA_PAPER.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(CHAKRA_PAPER.get());
                output.accept(CLAN_REROLL.get());
                output.accept(PAPER_BOMB.get());
            }).build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> NXB_DNA = CREATIVE_MODE_TAB.register("nxb_dna", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.dna_tab")) //The language key for the title of your CreativeModeTab
            .icon(() -> RANDOM_DNA.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(RANDOM_DNA.get());
                output.accept(EARTH_DNA.get());
                output.accept(FIRE_DNA.get());
                output.accept(WATER_DNA.get());
                output.accept(WIND_DNA.get());
                output.accept(LIGHTNING_DNA.get());
                output.accept(YIN_DNA.get());
                output.accept(YANG_DNA.get());
            }).build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> NXB_BOSSES = CREATIVE_MODE_TAB.register("nxb_bosses", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.bosses_tab")) //The language key for the title of your CreativeModeTab
            .icon(() -> ZABUZA_SPAWN_EGG.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(KISAME_SPAWN_EGG.get());
                output.accept(JINPACHI_SPAWN_EGG.get());
                output.accept(ZABUZA_SPAWN_EGG.get());
            }).build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> NXB_SCROLLS = CREATIVE_MODE_TAB.register("nxb_scroll", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.scroll")) //The language key for the title of your CreativeModeTab
            .icon(() -> TAIJUTSU_SCROLL.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(TAIJUTSU_SCROLL.get());
                output.accept(GENJUTSU_SCROLL.get());
                output.accept(KENJUTSU_SCROLL.get());
                output.accept(KINJUTSU_SCROLL.get());
                output.accept(MEDICAL_SCROLL.get());
                output.accept(NINJUTSU_SCROLL.get());
                output.accept(SENJUTSU_SCROLL.get());
                output.accept(SHURIKENJUTSU_SCROLL.get());
                output.accept(SPEED_SCROLL.get());
                output.accept(SUMMONING_SCROLL.get());
            }).build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> NXB_JUTSUS = CREATIVE_MODE_TAB.register("nxb_jutsus", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.jutsus_tab"))
            .icon(() -> FIRE_BALL_JUTSU.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(FIRE_BALL_JUTSU.get());
                output.accept(EARTH_WALL_JUTSU.get());
                output.accept(EARTH_WAVE_JUTSU.get());
                output.accept(WATER_PRISON_JUTSU.get());
                output.accept(LIGHTNING_CHAKRA_MODE.get());
            }).build());

    public static void register(IEventBus eventBus) {CREATIVE_MODE_TAB.register(eventBus);}
}
