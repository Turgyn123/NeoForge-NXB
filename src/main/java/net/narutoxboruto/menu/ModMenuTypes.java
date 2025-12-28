package net.narutoxboruto.menu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static net.narutoxboruto.main.Main.MOD_ID;

/**
 * Registry for all menu types in the mod.
 */
public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<JutsuStorageMenu>> JUTSU_STORAGE =
            MENUS.register("jutsu_storage", () -> IMenuTypeExtension.create(JutsuStorageMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
