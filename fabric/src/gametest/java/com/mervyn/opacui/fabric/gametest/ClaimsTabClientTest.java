package com.mervyn.opacui.fabric.gametest;

import mervyn.opacui.client.gui.PartyScreen;
import mervyn.opacui.client.gui.entry.ClaimEntry;
import mervyn.opacui.client.gui.list.AbstractPartyEntry;
import mervyn.opacui.client.gui.list.PartyEntryList;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import xaero.pac.client.api.OpenPACClientAPI;
import xaero.pac.client.claims.api.IClientClaimsManagerAPI;
import xaero.pac.client.claims.player.api.IClientPlayerClaimInfoAPI;

public class ClaimsTabClientTest implements FabricClientGameTest {

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            TestServerContext server = world.getServer();
            ChunkPos home = context.computeOnClient(mc -> mc.player.chunkPosition());

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

            context.runOnClient(mc -> {
                IClientClaimsManagerAPI claims = OpenPACClientAPI.get().getClaimsManager();
                check(claims.getClaimCount() == 3, "expected 3 claims, got " + claims.getClaimCount());
                check(claims.getForceloadCount() == 1, "expected 1 forceload, got " + claims.getForceloadCount());

                IClientPlayerClaimInfoAPI own = claims.getPlayerInfoStream()
                        .filter(info -> info.getPlayerId().equals(mc.player.getUUID()))
                        .findFirst().orElseThrow(() -> new AssertionError("no claim info for the player"));
                Identifier overworld = Level.OVERWORLD.identifier();
                check(home.equals(PartyScreen.nearestClaim(own, overworld, home)), "nearest Overworld claim isn't the home chunk");
                check("The Nether".equals(PartyScreen.otherClaimDimension(own, overworld)), "expected the Nether as the other dimension");
                check("Overworld".equals(PartyScreen.otherClaimDimension(own, Level.NETHER.identifier())), "expected the Overworld as the other dimension");
            });

            context.setScreen(() -> new PartyScreen(null));
            context.clickScreenButton("screen.opacui.tab_claims");
            context.waitTicks(5);
            context.takeScreenshot("claims-tab");

            double[] firstRow = firstRow(context);
            context.getInput().setCursorPos(firstRow[0], firstRow[1]);
            context.waitTicks(5);
            context.takeScreenshot("claims-tab-hover");
        }
    }

    private static double[] firstRow(ClientGameTestContext context) {
        return context.computeOnClient(mc -> {
            PartyEntryList list = mc.gui.screen().children().stream()
                    .filter(PartyEntryList.class::isInstance).map(PartyEntryList.class::cast)
                    .findFirst().orElseThrow(() -> new AssertionError("Claims tab has no list"));
            check(!list.children().isEmpty() && list.children().get(0) instanceof ClaimEntry, "Claims tab's first row isn't a ClaimEntry");
            AbstractPartyEntry row = list.children().get(0);
            int scale = mc.getWindow().getGuiScale();
            return new double[]{(row.getContentX() + row.getContentWidth() / 2.0) * scale, (row.getContentY() + row.getContentHeight() / 2.0) * scale};
        });
    }

    private static void playerCommand(ClientGameTestContext context, String command) {
        context.runOnClient(mc -> mc.player.connection.sendCommand(command));
        context.waitTicks(10);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
