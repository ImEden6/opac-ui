package mervyn.opacui;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpacUiMod implements ModInitializer {

    public static final String MOD_ID = "opacui";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[OPAC-UI] Initialized.");
    }
}
