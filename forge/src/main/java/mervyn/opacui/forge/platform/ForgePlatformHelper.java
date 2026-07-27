package mervyn.opacui.forge.platform;

import mervyn.opacui.platform.IPlatformHelper;
import net.minecraft.client.KeyMapping;
import java.util.ArrayList;
import java.util.List;

public class ForgePlatformHelper implements IPlatformHelper {

    public static final List<KeyMapping> KEY_MAPPINGS = new ArrayList<>();

    @Override
    public KeyMapping registerKeyBinding(KeyMapping keyMapping) {
        KEY_MAPPINGS.add(keyMapping);
        return keyMapping;
    }
}
