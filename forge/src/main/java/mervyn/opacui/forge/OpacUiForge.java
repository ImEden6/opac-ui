package mervyn.opacui.forge;

import mervyn.opacui.OpacUiMod;
import mervyn.opacui.forge.client.OpacUiForgeClient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;

@Mod(OpacUiMod.MOD_ID)
public class OpacUiForge {

    public OpacUiForge() {
        OpacUiMod.init();
        if (FMLLoader.getDist() == Dist.CLIENT) {
            OpacUiForgeClient.init();
        }
    }
}
