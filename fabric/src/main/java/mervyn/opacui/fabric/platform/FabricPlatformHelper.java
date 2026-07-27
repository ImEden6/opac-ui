package mervyn.opacui.fabric.platform;

import mervyn.opacui.platform.IPlatformHelper;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;

public class FabricPlatformHelper implements IPlatformHelper {

    @Override
    public KeyMapping registerKeyBinding(KeyMapping keyMapping) {
        return KeyBindingHelper.registerKeyBinding(keyMapping);
    }
}
