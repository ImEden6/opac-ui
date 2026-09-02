package mervyn.opacui.neoforge.client;

import mervyn.opacui.client.gui.PartyScreen;
import mervyn.opacui.client.keys.KeyBindings;
import mervyn.opacui.neoforge.platform.NeoForgePlatformHelper;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;

public class OpacUiNeoForgeClient {

    public static void init(IEventBus modEventBus) {
        KeyBindings.register();
        modEventBus.addListener(OpacUiNeoForgeClient::onRegisterKeyMappings);
        NeoForge.EVENT_BUS.addListener(OpacUiNeoForgeClient::onClientTick);
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        for (var key : NeoForgePlatformHelper.KEY_MAPPINGS) {
            event.register(key);
        }
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (KeyBindings.OPEN_PARTY_SCREEN.consumeClick()) {
            if (mc.level == null || mc.player == null || !PartyScreen.isAvailable(mc)) {
                continue;
            }
            mc.gui.setScreen(new PartyScreen(mc.gui.screen()));
        }
    }
}
