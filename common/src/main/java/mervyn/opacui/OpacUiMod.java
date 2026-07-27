package mervyn.opacui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpacUiMod {

    public static final String MOD_ID = "opacui";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static void init() {
        LOGGER.info("[OPAC-UI] Initialized.");
    }
}
