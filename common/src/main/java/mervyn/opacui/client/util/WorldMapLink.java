package mervyn.opacui.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.dimension.DimensionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

// Xaero's World Map is optional and its GuiMap extends xaerolib, which isn't published anywhere to compile against,
// so it's looked up by name. GuiMap has no public way to pick where the camera starts, so its private camera fields are
// set directly.
public final class WorldMapLink {

    private static final Logger LOGGER = LoggerFactory.getLogger("opacui");

    private WorldMapLink() {}

    public static void open(Minecraft mc, ChunkPos chunk) {
        if (mc.level == null || mc.player == null) return;
        Object processor;
        Screen map;
        try {
            Object session = Class.forName("xaero.map.WorldMapSession").getMethod("getCurrentSession").invoke(null);
            if (session == null) return;
            processor = session.getClass().getMethod("getMapProcessor").invoke(session);
            map = (Screen) Class.forName("xaero.map.gui.GuiMap")
                    .getConstructor(Screen.class, Screen.class, Class.forName("xaero.map.MapProcessor"), Entity.class)
                    .newInstance(mc.screen, mc.screen, processor, mc.player);
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.warn("Couldn't open Xaero's World Map", e);
            return;
        }
        try {
            centre(mc, map, processor, chunk);
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.warn("Couldn't move Xaero's World Map to the claim", e);
        }
        mc.setScreen(map);
    }

    private static void centre(Minecraft mc, Screen map, Object processor, ChunkPos chunk) throws ReflectiveOperationException {
        Class<?> guiMap = map.getClass();
        Field resetCamera = field(guiMap, "shouldResetCameraPos");
        Field attachedCamera = field(guiMap, "attachedCamera");
        Field cameraX = field(guiMap, "cameraX");
        Field cameraZ = field(guiMap, "cameraZ");
        Field prevDimDiv = field(guiMap, "prevPlayerDimDiv");
        Object mapWorld = processor.getClass().getMethod("getMapWorld").invoke(processor);
        Method followPlayer = mapWorld.getClass().getMethod("setCustomDimensionId", ResourceKey.class);
        Method worldUpdate = processor.getClass().getMethod("checkForWorldUpdate");
        double dimDiv = dimDiv(mc, processor, mapWorld);
        // Same as picking the player's dimension in World Map's dimension switcher.
        followPlayer.invoke(mapWorld, (Object) null);
        worldUpdate.invoke(processor);

        // The map may still be showing a dimension picked in World Map. Switching back to the player's happens a few
        // frames later, and when it does GuiMap keeps the camera's offset from the player rather than its position,
        // so the camera starts at the claim's offset from the player in the dimension it's switching from.
        double x = mc.player.getX();
        double z = mc.player.getZ();
        cameraX.setDouble(map, chunk.getMiddleBlockX() + 0.5 - x + x / dimDiv);
        cameraZ.setDouble(map, chunk.getMiddleBlockZ() + 0.5 - z + z / dimDiv);
        prevDimDiv.setDouble(map, dimDiv);
        // Without this the first frame moves the camera back onto the player.
        resetCamera.setBoolean(map, false);
        // Same as World Map's own go-to-coordinates, which also turns off its follow-the-player camera.
        attachedCamera.setBoolean(null, false);
    }

    private static double dimDiv(Minecraft mc, Object processor, Object mapWorld) throws ReflectiveOperationException {
        Object dimension = mapWorld.getClass().getMethod("getCurrentDimension").invoke(mapWorld);
        Object registry = processor.getClass().getMethod("getWorldDimensionTypeRegistry").invoke(processor);
        if (dimension == null || registry == null) return 1;
        return (double) dimension.getClass().getMethod("calculateDimDiv", Registry.class, DimensionType.class)
                .invoke(dimension, registry, mc.player.level().dimensionType());
    }

    private static Field field(Class<?> type, String name) throws NoSuchFieldException {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
