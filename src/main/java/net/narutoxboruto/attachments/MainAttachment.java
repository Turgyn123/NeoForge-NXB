package net.narutoxboruto.attachments;

import net.narutoxboruto.attachments.info.*;
import net.narutoxboruto.attachments.jutsus.*;
import net.narutoxboruto.attachments.modes.ChakraControl;
import net.narutoxboruto.attachments.modes.NarutoRun;
import net.narutoxboruto.attachments.stats.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class MainAttachment {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, "narutoxboruto");

    public static final Supplier<AttachmentType<Chakra>> CHAKRA = ATTACHMENT_TYPES.register("chakra", () -> AttachmentType.<Chakra>builder(() -> new Chakra()).serialize(Chakra.CODEC).copyOnDeath().build());
    public static final Supplier<AttachmentType<MaxChakra>> MAX_CHAKRA = ATTACHMENT_TYPES.register("max_chakra", () -> AttachmentType.<MaxChakra>builder(() -> new MaxChakra()).serialize(MaxChakra.CODEC).copyOnDeath().build());
    public static final Supplier<AttachmentType<ShinobiPoints>> SHINOBI_POINTS = ATTACHMENT_TYPES.register("shinobi_points", () -> AttachmentType.<ShinobiPoints>builder(() -> new ShinobiPoints()).serialize(ShinobiPoints.CODEC).copyOnDeath().build());
    public static final Supplier<AttachmentType<Rank>> RANK = ATTACHMENT_TYPES.register("rank", () -> AttachmentType.<Rank>builder(() -> new Rank()).serialize(Rank.CODEC).copyOnDeath().build());
    public static final Supplier<AttachmentType<Clan>> CLAN = ATTACHMENT_TYPES.register("clan", () -> AttachmentType.<Clan>builder(() -> new Clan()).serialize(Clan.CODEC).copyOnDeath().build());
    public static final Supplier<AttachmentType<Affiliation>> AFFILIATION = ATTACHMENT_TYPES.register("affiliaton", () -> AttachmentType.<Affiliation>builder(() -> new Affiliation()).serialize(Affiliation.CODEC).copyOnDeath().build());
    public static final Supplier<AttachmentType<Genjutsu>> GENJUTSU = ATTACHMENT_TYPES.register("genjutsu", () -> AttachmentType.<Genjutsu>builder(() -> new Genjutsu()).serialize(Genjutsu.CODEC).copyOnDeath().build());
    public static final Supplier<AttachmentType<Kenjutsu>> KENJUTSU = ATTACHMENT_TYPES.register("kenjutsu", () -> AttachmentType.<Kenjutsu>builder(() -> new Kenjutsu()).serialize(Kenjutsu.CODEC).copyOnDeath().build());
    public static final Supplier<AttachmentType<Kinjutsu>> KINJUTSU = ATTACHMENT_TYPES.register("kinjutsu", () -> AttachmentType.<Kinjutsu>builder(() -> new Kinjutsu()).serialize(Kinjutsu.CODEC).copyOnDeath().build());
    public static final Supplier<AttachmentType<Medical>> MEDICAL = ATTACHMENT_TYPES.register("medical", () -> AttachmentType.<Medical>builder(() -> new Medical()).serialize(Medical .CODEC).copyOnDeath().build());
    public static final Supplier<AttachmentType<Ninjutsu>> NINJUTSU = ATTACHMENT_TYPES.register("ninjutsu", () -> AttachmentType.<Ninjutsu>builder(() -> new Ninjutsu()).serialize(Ninjutsu.CODEC).copyOnDeath().build());
    public static final Supplier<AttachmentType<Senjutsu>> SENJUTSU = ATTACHMENT_TYPES.register("senjutsu", () -> AttachmentType.<Senjutsu>builder(() -> new Senjutsu()).serialize(Senjutsu.CODEC).copyOnDeath().build());
    public static final Supplier<AttachmentType<Shurikenjutsu>> SHURIKENJUTSU = ATTACHMENT_TYPES.register("shurikenjutsu", () -> AttachmentType.<Shurikenjutsu>builder(() -> new Shurikenjutsu()).serialize(Shurikenjutsu.CODEC).copyOnDeath().build());
    public static final Supplier<AttachmentType<Speed>> SPEED = ATTACHMENT_TYPES.register("speed", () -> AttachmentType.<Speed>builder(() -> new Speed()).serialize(Speed.CODEC).copyOnDeath().build());
    public static final Supplier<AttachmentType<Summoning>> SUMMONING = ATTACHMENT_TYPES.register("summoning", () -> AttachmentType.<Summoning>builder(() -> new Summoning()).serialize(Summoning.CODEC).copyOnDeath().build());
    public static final Supplier<AttachmentType<Taijutsu>> TAIJUTSU = ATTACHMENT_TYPES.register("taijutsu", () -> AttachmentType.<Taijutsu>builder(() -> new Taijutsu()).serialize(Taijutsu.CODEC).copyOnDeath().build());
    public static final Supplier<AttachmentType<EarthList>> EARTHLIST = ATTACHMENT_TYPES.register("earth", () -> AttachmentType.<EarthList>builder(() -> new EarthList()).serialize(EarthList.CODEC).copyOnDeath().build());
    public static final Supplier<AttachmentType<FireList>> FIRELIST = ATTACHMENT_TYPES.register("firet", () -> AttachmentType.<FireList>builder(() -> new FireList()).serialize(FireList.CODEC).copyOnDeath().build());
    public static final Supplier<AttachmentType<LightningList>> LIGHTINGLIST = ATTACHMENT_TYPES.register("lightning", () -> AttachmentType.<LightningList>builder(() -> new LightningList()).serialize(LightningList.CODEC).copyOnDeath().build());
    public static final Supplier<AttachmentType<WaterList>> WATERLIST = ATTACHMENT_TYPES.register("water", () -> AttachmentType.<WaterList>builder(() -> new WaterList()).serialize(WaterList.CODEC).copyOnDeath().build());
    public static final Supplier<AttachmentType<WindList>> WINDLIST = ATTACHMENT_TYPES.register("wind", () -> AttachmentType.<WindList>builder(() -> new WindList()).serialize(WindList.CODEC).copyOnDeath().build());
    public static final Supplier<AttachmentType<YangList>> YANGLIST = ATTACHMENT_TYPES.register("yang", () -> AttachmentType.<YangList>builder(() -> new YangList()).serialize(YangList.CODEC).copyOnDeath().build());
    public static final Supplier<AttachmentType<YingList>> YINGLIST = ATTACHMENT_TYPES.register("ying", () -> AttachmentType.<YingList>builder(() -> new YingList()).serialize(YingList.CODEC).copyOnDeath().build());

    public static final Supplier<AttachmentType<ReleaseList>> RELEASE_LIST = ATTACHMENT_TYPES.register("release_list", () -> AttachmentType.<ReleaseList>builder(() -> new ReleaseList()).serialize(ReleaseList.CODEC).copyOnDeath().build());
    public static final Supplier<AttachmentType<ChakraControl>> CHAKRA_CONTROL = ATTACHMENT_TYPES.register("chakra_control", () -> AttachmentType.<ChakraControl>builder(() -> new ChakraControl()).serialize(ChakraControl.CODEC).copyOnDeath().build());
    public static final Supplier<AttachmentType<NarutoRun>> NARUTO_RUN = ATTACHMENT_TYPES.register("naruto_run", () -> AttachmentType.<NarutoRun>builder(() -> new NarutoRun()).serialize(NarutoRun.CODEC).copyOnDeath().build());
    public static final Supplier<AttachmentType<JutsuStorage>> JUTSU_STORAGE = ATTACHMENT_TYPES.register("jutsu_storage", () -> AttachmentType.<JutsuStorage>builder(() -> new JutsuStorage()).serialize(JutsuStorage.CODEC).copyOnDeath().build());

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}

