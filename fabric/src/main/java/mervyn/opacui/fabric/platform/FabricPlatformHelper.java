package mervyn.opacui.fabric.platform;

import mervyn.opacui.platform.IPlatformHelper;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;

public class FabricPlatformHelper implements IPlatformHelper {

    @Override
    public KeyMapping registerKeyBinding(KeyMapping keyMapping) {
        return KeyMappingHelper.registerKeyMapping(keyMapping);
    }
}
