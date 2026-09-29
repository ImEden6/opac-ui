package mervyn.opacui.client.util;

import net.minecraft.client.Minecraft;

/**
 * Helper that fires OPAC's party commands.
 * All commands are under the stable prefix "oparties".
 */
public final class PartyCommands {

    private static final String PREFIX = "oparties";

    // ── Party lifecycle ───────────────────────────────────────────────────

    public static void createParty(Minecraft mc) {
        send(mc, PREFIX + " create");
    }

    public static void leaveParty(Minecraft mc) {
        send(mc, PREFIX + " leave");
    }

    /** Sends the two-step destroy+confirm in one call. */
    public static void destroyParty(Minecraft mc) {
        send(mc, PREFIX + " destroy confirm");
    }

    /** Renames the party by setting the parties.name config option. */
    public static void renameParty(Minecraft mc, String newName) {
        if (newName == null) return;
        String sanitized = newName.replaceAll("[\\r\\n\\t]", "");
        String escaped = sanitized.replace("\\", "\\\\").replace("\"", "\\\"");
        send(mc, "opac player-config set parties.name \"" + escaped + "\"");
    }

    // ── Member management ─────────────────────────────────────────────────

    /** Sends a party invite to the given player name. Requires MODERATOR+. */
    public static void invite(Minecraft mc, String playerName) {
        send(mc, PREFIX + " member invite " + sanitizeName(playerName));
    }

    /**
     * Kicks a member (or revokes a pending invite) by username.
     * Requires MODERATOR+.
     */
    public static void kick(Minecraft mc, String username) {
        send(mc, PREFIX + " member kick " + sanitizeName(username));
    }

    /**
     * Sets a member's rank. rank must be one of the PartyMemberRank enum names
     * (MEMBER, CLAIMER, MODERATOR, ADMIN). Requires ADMIN+.
     */
    public static void setRank(Minecraft mc, String rank, String username) {
        send(mc, PREFIX + " member rank " + sanitizeName(rank) + " " + sanitizeName(username));
    }

    /**
     * Transfers party ownership to another member (immediately confirmed).
     * Only the current owner can call this.
     */
    public static void transferOwnership(Minecraft mc, String username) {
        send(mc, PREFIX + " transfer " + sanitizeName(username) + " confirm");
    }

    // ── Allies ────────────────────────────────────────────────────────────

    /**
     * Sends an ally request to the player (by name) so their party becomes
     * allied. Requires MODERATOR+.
     */
    public static void addAlly(Minecraft mc, String playerName) {
        send(mc, PREFIX + " ally add " + sanitizeName(playerName));
    }

    /**
     * Removes an ally relationship by the owner's username.
     * Requires MODERATOR+.
     */
    public static void removeAlly(Minecraft mc, String ownerName) {
        send(mc, PREFIX + " ally remove " + sanitizeName(ownerName));
    }

    // ── Internal ──────────────────────────────────────────────────────────

    private static String sanitizeName(String input) {
        if (input == null) return "";
        return input.replaceAll("[\\r\\n\\t\\s]", "");
    }

    private static void send(Minecraft mc, String command) {
        if (mc.player == null || mc.player.connection == null) return;
        mc.player.connection.sendCommand(command);
    }

    private PartyCommands() {}
}
