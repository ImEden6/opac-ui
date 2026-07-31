package mervyn.opacui.neoforge;

import mervyn.opacui.OpacUiMod;
import mervyn.opacui.neoforge.client.OpacUiNeoForgeClient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@Mod(OpacUiMod.MOD_ID)
public class OpacUiNeoForge {

    public OpacUiNeoForge(IEventBus modEventBus) {
        OpacUiMod.init();
        modEventBus.addListener((FMLClientSetupEvent event) -> OpacUiNeoForgeClient.init(modEventBus));
    }
}
