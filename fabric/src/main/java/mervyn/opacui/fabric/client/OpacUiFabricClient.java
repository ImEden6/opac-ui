package mervyn.opacui.fabric.client;

import mervyn.opacui.client.gui.PartyScreen;
import mervyn.opacui.client.keys.KeyBindings;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

@Environment(EnvType.CLIENT)
public class OpacUiFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        KeyBindings.register();

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (KeyBindings.OPEN_PARTY_SCREEN.consumeClick()) {
                if (mc.level == null || mc.player == null || !PartyScreen.isAvailable(mc)) {
                    continue;
                }
                mc.gui.setScreen(new PartyScreen(mc.gui.screen()));
            }
        });
    }
}
