package mervyn.opacui.fabric.gametest;

import mervyn.opacui.client.gui.PartyScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

public class ClaimsTabClientTest implements FabricClientGameTest {

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getClientWorld().waitForChunksRender();
            TestServerContext server = world.getServer();

            // OPAC's party commands only accept a real player, so these go out from the client like the mod's own.
            playerCommand(context, "oparties create");
            playerCommand(context, "oclaims claim");
            playerCommand(context, "oclaims forceload");
            server.runCommand("execute as @p at @s run tp @s ~20 ~ ~20");
            context.waitTicks(10);
            playerCommand(context, "oclaims claim");
            server.runCommand("execute in minecraft:the_nether run tp @p 0 100 0");
            context.waitTicks(40);
            playerCommand(context, "oclaims claim");
            context.waitTicks(20);

            context.setScreen(() -> new PartyScreen(null));
            context.clickScreenButton("screen.opacui.tab_claims");
            context.waitTicks(5);
            context.takeScreenshot("claims-tab");

            // First row of the list, in window pixels: LIST_TOP plus about half a row, times the GUI scale.
            double[] firstRow = context.computeOnClient(mc -> {
                int scale = mc.getWindow().getGuiScale();
                return new double[]{mc.getWindow().getGuiScaledWidth() / 2.0 * scale, 70.0 * scale};
            });
            context.getInput().setCursorPos(firstRow[0], firstRow[1]);
            context.waitTicks(5);
            context.takeScreenshot("claims-tab-hover");
        }
    }

    private static void playerCommand(ClientGameTestContext context, String command) {
        context.runOnClient(mc -> mc.player.connection.sendCommand(command));
        context.waitTicks(10);
    }
}
