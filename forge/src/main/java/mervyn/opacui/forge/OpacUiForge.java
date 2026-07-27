package mervyn.opacui.forge;

import mervyn.opacui.OpacUiMod;
import mervyn.opacui.forge.client.OpacUiForgeClient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

@Mod(OpacUiMod.MOD_ID)
public class OpacUiForge {

    public OpacUiForge() {
        OpacUiMod.init();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> OpacUiForgeClient::init);
    }
}
