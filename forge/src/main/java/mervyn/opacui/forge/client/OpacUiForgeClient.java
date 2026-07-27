package mervyn.opacui.forge.client;

import mervyn.opacui.OpacUiMod;
import mervyn.opacui.client.gui.PartyScreen;
import mervyn.opacui.client.keys.KeyBindings;
import mervyn.opacui.forge.platform.ForgePlatformHelper;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OpacUiMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class OpacUiForgeClient {

    public static void init() {
        KeyBindings.register();
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        for (var key : ForgePlatformHelper.KEY_MAPPINGS) {
            event.register(key);
        }
    }

    @Mod.EventBusSubscriber(modid = OpacUiMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeForgeEvents {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
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
