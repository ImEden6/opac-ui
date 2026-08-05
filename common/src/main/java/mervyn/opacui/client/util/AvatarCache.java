package mervyn.opacui.client.util;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.UUID;

/**
 * Resolves player skins for GUI head rendering.
 *
 * Prefers {@link PlayerInfo#getSkin()} for currently-connected players — it
 * already holds the real, signed texture properties from the network layer.
 * {@code SkinManager#get} resolves textures from whatever {@link GameProfile}
 * it's handed, and a synthetic profile built from just a UUID/name has no
 * texture properties, so falling back to it for online players showed the
 * wrong (or default) skin. Only offline members fall back to that lookup.
 */
public final class AvatarCache {

    private AvatarCache() {}

    public static PlayerSkin getSkin(Minecraft mc, UUID uuid, String username) {
        if (mc == null || (uuid == null && (username == null || username.isEmpty()))) {
            return DefaultPlayerSkin.get(uuid != null ? uuid : UUID.randomUUID());
        }

        if (mc.getConnection() != null) {
            PlayerInfo info = uuid != null ? mc.getConnection().getPlayerInfo(uuid)
                    : mc.getConnection().getPlayerInfo(username);
            if (info != null) {
                return info.getSkin();
            }
        }

        GameProfile profile = new GameProfile(uuid, username);
        return mc.getSkinManager().createLookup(profile, false).get();
    }
}
