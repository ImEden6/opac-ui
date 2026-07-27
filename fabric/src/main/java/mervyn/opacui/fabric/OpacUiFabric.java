package mervyn.opacui.fabric;

import mervyn.opacui.OpacUiMod;
import net.fabricmc.api.ModInitializer;

public class OpacUiFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        OpacUiMod.init();
    }
}
