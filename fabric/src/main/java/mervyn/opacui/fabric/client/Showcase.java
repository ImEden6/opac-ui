package mervyn.opacui.fabric.client;

import mervyn.opacui.client.gui.ConfirmActionScreen;
import mervyn.opacui.client.gui.PartyScreen;
import mervyn.opacui.client.util.PartyCommands;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;

// Dev only, switched on by the opacui.showcase property that runClientShowcase sets. Drops into
// run/saves/New World, opens the party screens with blur on and off, saves each to
// run/screenshots/showcase_*.png and quits.
final class Showcase {
    static final boolean ENABLED = Boolean.getBoolean("opacui.showcase");

    private static int tick;

    private Showcase() {}

    static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null)
            return;
        tick++;
        switch (tick) {
            case 40 -> {
                mc.options.pauseOnLostFocus = false;
                mc.setScreen(null);
                PartyCommands.destroyParty(mc);
            }
            case 80 -> {
                blur(mc, 5);
                mc.setScreen(new PartyScreen(null));
            }
            case 110 -> shot(mc, "no_party_blur");
            case 120 -> {
                mc.setScreen(null);
                PartyCommands.createParty(mc);
            }
            case 180 -> mc.setScreen(new PartyScreen(null));
            case 210 -> shot(mc, "party_blur");
            case 220 -> blur(mc, 0);
            case 240 -> shot(mc, "party_no_blur");
            case 250 -> {
                blur(mc, 5);
                mc.setScreen(new ConfirmActionScreen(mc.screen, Component.literal("Leave party?"),
                        Component.literal("Showcase dialog, nothing happens if you confirm."), () -> {}));
            }
            case 280 -> shot(mc, "confirm_blur");
            case 290 -> {
                mc.setScreen(null);
                PartyCommands.destroyParty(mc);
            }
            case 320 -> mc.stop();
            default -> {}
        }
    }

    private static void blur(Minecraft mc, int value) {
        mc.options.menuBackgroundBlurriness().set(value);
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, "showcase_" + name + ".png", mc.getMainRenderTarget(), message -> {});
    }
}
