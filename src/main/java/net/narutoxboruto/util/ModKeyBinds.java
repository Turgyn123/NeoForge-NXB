package net.narutoxboruto.util;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class ModKeyBinds {
    public static final String KEY_CATEGORY = "key.category.narutoxboruto";
    public static final String KEY_OPEN_GUI = "key.narutoxboruto.open_gui";
    public static final String KEY_CHAKRA_CONTROL = "key.narutoxboruto.chakra_control";
    public static final String KEY_SPECIAL_ACTION = "key.narutoxboruto.special_action";
    public static final String KEY_CHAKRA_RECHARGE = "key.narutoxboruto.chakra_recharge";
    public static final String KEY_JUTSU_STORAGE = "key.narutoxboruto.jutsu_storage";


    public static final KeyMapping OPEN_GUI = new KeyMapping(KEY_OPEN_GUI, KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, KEY_CATEGORY);

    public static final KeyMapping SPECIAL_ACTION = new KeyMapping(KEY_SPECIAL_ACTION, KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, KEY_CATEGORY);

    public static final KeyMapping CHAKRA_RECHARGE = new KeyMapping(KEY_CHAKRA_RECHARGE, KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, KEY_CATEGORY);

    public static final KeyMapping CHAKRA_CONTROL = new KeyMapping(KEY_CHAKRA_CONTROL, KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, KEY_CATEGORY);
    
    public static final KeyMapping JUTSU_STORAGE = new KeyMapping(KEY_JUTSU_STORAGE, KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, KEY_CATEGORY);
}
