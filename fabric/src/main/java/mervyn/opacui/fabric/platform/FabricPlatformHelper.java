package mervyn.opacui.fabric.platform;

import mervyn.opacui.platform.IPlatformHelper;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;

public class FabricPlatformHelper implements IPlatformHelper {

    @Override
    public KeyMapping registerKeyBinding(KeyMapping keyMapping) {
        return KeyBindingHelper.registerKeyBinding(keyMapping);
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }
}
