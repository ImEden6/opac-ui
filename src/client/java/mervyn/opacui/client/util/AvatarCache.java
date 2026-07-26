package mervyn.opacui.client.util;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches player skin ResourceLocations for GUI head rendering.
 */
public final class AvatarCache {

    private static final Map<UUID, ResourceLocation> CACHE_BY_UUID = new ConcurrentHashMap<>();
    private static final Map<String, ResourceLocation> CACHE_BY_NAME = new ConcurrentHashMap<>();
    private static final Set<UUID> REQUESTED_UUIDS = ConcurrentHashMap.newKeySet();
    private static final Set<String> REQUESTED_NAMES = ConcurrentHashMap.newKeySet();

    private AvatarCache() {}

    public static ResourceLocation getSkin(Minecraft mc, UUID uuid, String username) {
        if (mc == null) return DefaultPlayerSkin.getDefaultSkin();

        // 1. Check cached UUID
        if (uuid != null) {
            ResourceLocation cached = CACHE_BY_UUID.get(uuid);
            if (cached != null) return cached;
        }

        // 2. Check cached Name
        if (username != null && !username.isEmpty()) {
            ResourceLocation cachedName = CACHE_BY_NAME.get(username.toLowerCase());
            if (cachedName != null) return cachedName;
        }

        // 3. Request asynchronously via SkinManager
        SkinManager skinManager = mc.getSkinManager();
        if (uuid != null && REQUESTED_UUIDS.add(uuid)) {
            GameProfile profile = new GameProfile(uuid, username);
            skinManager.registerSkins(profile, (type, location, profileTexture) -> {
                if (type == MinecraftProfileTexture.Type.SKIN) {
                    CACHE_BY_UUID.put(uuid, location);
                    if (username != null) CACHE_BY_NAME.put(username.toLowerCase(), location);
                }
            }, false);
            return skinManager.getInsecureSkinLocation(profile);
        } else if (username != null && !username.isEmpty()) {
            String lower = username.toLowerCase();
            if (REQUESTED_NAMES.add(lower)) {
                GameProfile profile = new GameProfile(null, username);
                skinManager.registerSkins(profile, (type, location, profileTexture) -> {
                    if (type == MinecraftProfileTexture.Type.SKIN) {
                        CACHE_BY_NAME.put(lower, location);
                        if (uuid != null) CACHE_BY_UUID.put(uuid, location);
                    }
                }, false);
                return skinManager.getInsecureSkinLocation(profile);
            }
        }

        GameProfile profile = new GameProfile(uuid, username);
        return skinManager.getInsecureSkinLocation(profile);
    }
}
