package mervyn.opacui.forge.client;

import mervyn.opacui.client.gui.PartyScreen;
import mervyn.opacui.client.keys.KeyBindings;
import mervyn.opacui.forge.platform.ForgePlatformHelper;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;

public class OpacUiForgeClient {

    public static void init() {
        KeyBindings.register();
        RegisterKeyMappingsEvent.BUS.addListener(OpacUiForgeClient::onRegisterKeyMappings);
        MinecraftForge.EVENT_BUS.register(new ForgeForgeEvents());
    }

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        for (var key : ForgePlatformHelper.KEY_MAPPINGS) {
            event.register(key);
        }
    }

    public static class ForgeForgeEvents {
        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            while (KeyBindings.OPEN_PARTY_SCREEN.consumeClick()) {
                if (mc.level == null || mc.player == null || !PartyScreen.isAvailable(mc)) {
                    continue;
                }
                mc.setScreen(new PartyScreen(mc.screen));
            }
        }
    }
}
