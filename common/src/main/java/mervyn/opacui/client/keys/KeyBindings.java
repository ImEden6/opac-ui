package mervyn.opacui.client.keys;

import com.mojang.blaze3d.platform.InputConstants;
import mervyn.opacui.platform.Services;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class KeyBindings {

    public static final String CATEGORY = "key.categories.opacui";

    public static KeyMapping OPEN_PARTY_SCREEN;

    public static void register() {
        OPEN_PARTY_SCREEN = Services.PLATFORM.registerKeyBinding(new KeyMapping(
                "key.opacui.open_party_screen",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                CATEGORY
        ));
    }

    private KeyBindings() {}
}
