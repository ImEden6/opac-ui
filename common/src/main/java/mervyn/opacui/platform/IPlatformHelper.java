package mervyn.opacui.platform;

import net.minecraft.client.KeyMapping;

public interface IPlatformHelper {
    /**
     * Register a key mapping on the active platform.
     */
    KeyMapping registerKeyBinding(KeyMapping keyMapping);
}
