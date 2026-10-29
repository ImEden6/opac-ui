package mervyn.opacui.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.level.ChunkPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Xaero's World Map is optional and its GuiMap extends xaerolib, which isn't published anywhere to compile against,
// so its public constructor is looked up by name.
public final class WorldMapLink {

    private static final Logger LOGGER = LoggerFactory.getLogger("opacui");

    private WorldMapLink() {}

    public static void open(Minecraft mc, ChunkPos chunk) {
        if (mc.level == null || mc.player == null) return;
        try {
            Object session = Class.forName("xaero.map.WorldMapSession").getMethod("getCurrentSession").invoke(null);
            if (session == null) return;
            Object processor = session.getClass().getMethod("getMapProcessor").invoke(session);
            // GuiMap centres its camera on the entity it's given, so a marker that never joins the world points it at the claim.
            Marker marker = new Marker(EntityType.MARKER, mc.level);
            marker.snapTo(chunk.getMiddleBlockX() + 0.5, mc.player.getY(), chunk.getMiddleBlockZ() + 0.5);
            Screen map = (Screen) Class.forName("xaero.map.gui.GuiMap")
                    .getConstructor(Screen.class, Screen.class, Class.forName("xaero.map.MapProcessor"), Entity.class)
                    .newInstance(mc.gui.screen(), mc.gui.screen(), processor, marker);
            mc.gui.setScreen(map);
        } catch (ReflectiveOperationException | ClassCastException e) {
            LOGGER.warn("Couldn't open Xaero's World Map", e);
        }
    }
}
