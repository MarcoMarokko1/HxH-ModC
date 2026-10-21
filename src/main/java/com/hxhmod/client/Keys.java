package com.hxhmod.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class Keys {
    private static final String CAT = "key.categories.hxhmod";

    private static KeyMapping key(String name, int code) {
        return new KeyMapping("key.hxhmod." + name, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, code, CAT);
    }

    public static final KeyMapping USE = key("use", GLFW.GLFW_KEY_G);
    public static final KeyMapping NEXT = key("next", GLFW.GLFW_KEY_V);
    public static final KeyMapping PREV = key("prev", GLFW.GLFW_KEY_B);
    public static final KeyMapping TEN = key("ten", GLFW.GLFW_KEY_R);
    public static final KeyMapping ZETSU = key("zetsu", GLFW.GLFW_KEY_X);
    public static final KeyMapping REN = key("ren", GLFW.GLFW_KEY_C);
    public static final KeyMapping GYO = key("gyo", GLFW.GLFW_KEY_H);
    public static final KeyMapping EN = key("en", GLFW.GLFW_KEY_J);

    private Keys() {}
}
