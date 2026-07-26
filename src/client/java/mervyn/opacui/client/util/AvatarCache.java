package mervyn.opacui.client.util;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches player skin ResourceLocations for GUI head rendering with TTL eviction.
 * Expired entries (older than 5 minutes) are re-fetched via Minecraft's SkinManager
 * so skin updates by players are automatically picked up.
 */
public final class AvatarCache {

    private static final long CACHE_TTL_MS = 5 * 60 * 1000L; // 5 minutes TTL

    private record CachedSkin(ResourceLocation location, long timestamp) {}

    private static final Map<UUID, CachedSkin> CACHE_BY_UUID = new ConcurrentHashMap<>();
    private static final Map<String, CachedSkin> CACHE_BY_NAME = new ConcurrentHashMap<>();
    private static final Map<Object, Long> REQUESTED_TIMESTAMPS = new ConcurrentHashMap<>();

    private AvatarCache() {}

    /** Clears all cached avatar textures, forcing skin re-fetches. */
    public static void clear() {
        CACHE_BY_UUID.clear();
        CACHE_BY_NAME.clear();
        REQUESTED_TIMESTAMPS.clear();
    }

    public static ResourceLocation getSkin(Minecraft mc, UUID uuid, String username) {
        if (mc == null) return DefaultPlayerSkin.getDefaultSkin();

        long now = System.currentTimeMillis();

        // 1. Check cached UUID (validate TTL)
        if (uuid != null) {
            CachedSkin cached = CACHE_BY_UUID.get(uuid);
            if (cached != null) {
                if (now - cached.timestamp < CACHE_TTL_MS) {
                    return cached.location;
                } else {
                    CACHE_BY_UUID.remove(uuid);
                    REQUESTED_TIMESTAMPS.remove(uuid);
                }
            }
        }

        // 2. Check cached Name (validate TTL)
        if (username != null && !username.isEmpty()) {
            String lower = username.toLowerCase();
            CachedSkin cachedName = CACHE_BY_NAME.get(lower);
            if (cachedName != null) {
                if (now - cachedName.timestamp < CACHE_TTL_MS) {
                    return cachedName.location;
                } else {
                    CACHE_BY_NAME.remove(lower);
                    REQUESTED_TIMESTAMPS.remove(lower);
                }
            }
        }

        // 3. Request asynchronously via SkinManager if not requested recently
        SkinManager skinManager = mc.getSkinManager();
        Object key = uuid != null ? uuid : (username != null && !username.isEmpty() ? username.toLowerCase() : null);

        if (key != null) {
            Long lastReq = REQUESTED_TIMESTAMPS.get(key);
            if (lastReq == null || now - lastReq > CACHE_TTL_MS) {
                REQUESTED_TIMESTAMPS.put(key, now);
                GameProfile profile = new GameProfile(uuid, username);
                skinManager.registerSkins(profile, (type, location, profileTexture) -> {
                    if (type == MinecraftProfileTexture.Type.SKIN) {
                        CachedSkin entry = new CachedSkin(location, System.currentTimeMillis());
                        if (uuid != null) CACHE_BY_UUID.put(uuid, entry);
                        if (username != null && !username.isEmpty()) CACHE_BY_NAME.put(username.toLowerCase(), entry);
                    }
                }, true);
            }
        }

        if (uuid == null && (username == null || username.isEmpty())) {
            return DefaultPlayerSkin.getDefaultSkin();
        }

        GameProfile profile = new GameProfile(uuid, username);
        return skinManager.getInsecureSkinLocation(profile);
    }
}
