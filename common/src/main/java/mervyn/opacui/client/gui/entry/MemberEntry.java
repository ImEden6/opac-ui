package mervyn.opacui.client.gui.entry;

import mervyn.opacui.client.gui.PartyScreen;
import mervyn.opacui.client.util.AvatarCache;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.world.entity.player.PlayerSkin;
import mervyn.opacui.client.gui.list.AbstractPartyEntry;
import mervyn.opacui.client.gui.ConfirmActionScreen;
import mervyn.opacui.client.util.PartyCommands;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import xaero.pac.client.api.OpenPACClientAPI;
import xaero.pac.client.player.config.IPlayerConfigClientStorage;
import xaero.pac.client.player.config.api.IPlayerConfigClientStorageManagerAPI;
import xaero.pac.client.player.config.api.v2.IPlayerConfigStringableOptionClientStorageAPI;
import xaero.pac.common.parties.party.api.IPartyMemberDynamicInfoSyncableAPI;
import xaero.pac.common.server.player.config.api.v2.IPlayerConfigOptionSpecAPI;
import xaero.pac.common.server.player.config.api.v2.PlayerConfigOptions;
import xaero.pac.common.parties.party.member.PartyMemberRank;
import xaero.pac.common.parties.party.member.api.IPartyMemberAPI;

import java.util.List;
import java.util.function.Consumer;

/**
 * A single scrollable-list row representing one party member.
 * Renders: [★?] username (rank label) [▼Rank] [▲Rank] [Kick] [Transfer?] [Sharing, own row only]
 * Hovering an online member shows where they are, if OPAC has their location.
 *
 * Buttons are only shown if the local player has sufficient rank.
 */
public class MemberEntry extends AbstractPartyEntry {

    private static final int BTN_W = EntryButton.WIDTH;
    private static final int BTN_H = EntryButton.HEIGHT;
    private static final int BTN_GAP = EntryButton.GAP;

    private final IPartyMemberAPI member;

    // Action buttons — initialised in the constructor
    private final Button btnRankDown;
    private final Button btnRankUp;
    private final Button btnKick;
    private final Button btnTransfer;
    private final Button btnShare;
    private String shownLabel;
    private String shownTooltip;

    public static final class ShareState {
        private static ShareState current;

        private final int session;
        private Boolean pending;
        private long until;

        private ShareState(int session) {
            this.session = session;
        }

        // Kept by identity hash rather than a reference so a stale session can't hold the old connection alive.
        public static ShareState get() {
            int session = System.identityHashCode(Minecraft.getInstance().getConnection());
            if (current == null || current.session != session)
                current = new ShareState(session);
            return current;
        }

        public void tick(Consumer<Component> feedback) {
            if (pending == null || configSyncing()) return;
            boolean share = isOn(PlayerConfigOptions.SHARE_LOCATION_WITH_PARTY, myOption(PlayerConfigOptions.SHARE_LOCATION_WITH_PARTY));
            if (pending == share) {
                pending = null;
                feedback.accept(Component.translatable(share ? "screen.opacui.feedback.sharing_on" : "screen.opacui.feedback.sharing_off"));
            } else if (Util.getMillis() > until) {
                pending = null;
            }
        }
    }

    public MemberEntry(
            IPartyMemberAPI member,
            PartyMemberRank localRank,
            boolean localIsOwner,
            boolean isSelf,
            Consumer<Component> onAction) {
        this.member = member;

        Minecraft mc = Minecraft.getInstance();

        PartyMemberRank downRank = rankDown(member.getRank());
        PartyMemberRank upRank = rankUp(member.getRank());

        btnRankDown = EntryButton.create(Component.literal("▼"), EntryButton.WIDTH / 2 - 1, EntryButton.HEIGHT, b -> {
            if (downRank != null) {
                PartyCommands.setRank(mc, downRank.name(), member.getUsername());
                onAction.accept(Component.translatable("screen.opacui.feedback.demoted", member.getUsername(),
                        downRank.name()));
            }
        });
        if (downRank != null)
            btnRankDown.setTooltip(Tooltip.create(Component.translatable("screen.opacui.tooltip.demote", downRank.name())));

        btnRankUp = EntryButton.create(Component.literal("▲"), EntryButton.WIDTH / 2 - 1, EntryButton.HEIGHT, b -> {
            if (upRank != null) {
                PartyCommands.setRank(mc, upRank.name(), member.getUsername());
                onAction.accept(
                        Component.translatable("screen.opacui.feedback.promoted", member.getUsername(), upRank.name()));
            }
        });
        if (upRank != null)
            btnRankUp.setTooltip(Tooltip.create(Component.translatable("screen.opacui.tooltip.promote", upRank.name())));

        btnKick = EntryButton.create(Component.translatable("screen.opacui.kick"), b -> {
            Screen current = mc.screen;
            if (current != null) {
                mc.setScreen(new ConfirmActionScreen(
                        current,
                        Component.translatable("screen.opacui.confirm_title"),
                        Component.translatable("screen.opacui.confirm_kick", member.getUsername()),
                        () -> {
                            PartyCommands.kick(mc, member.getUsername());
                            onAction.accept(
                                    Component.translatable("screen.opacui.feedback.kicked", member.getUsername()));
                            mc.setScreen(current);
                        }));
            }
        });
        btnKick.setTooltip(Tooltip.create(Component.translatable("screen.opacui.tooltip.kick")));

        btnTransfer = EntryButton.create(Component.translatable("screen.opacui.transfer"), b -> {
            Screen current = mc.screen;
            if (current != null) {
                mc.setScreen(new ConfirmActionScreen(
                        current,
                        Component.translatable("screen.opacui.confirm_title"),
                        Component.translatable("screen.opacui.confirm_transfer", member.getUsername()),
                        () -> {
                            PartyCommands.transferOwnership(mc, member.getUsername());
                            onAction.accept(
                                    Component.translatable("screen.opacui.feedback.transferred", member.getUsername()));
                            mc.setScreen(current);
                        }));
            }
        });
        btnTransfer.setTooltip(Tooltip.create(Component.translatable("screen.opacui.tooltip.transfer")));

        btnShare = EntryButton.create(Component.empty(), b -> {
            ShareState state = ShareState.get();
            if (state.pending != null || configSyncing()) return;
            state.pending = !isOn(PlayerConfigOptions.SHARE_LOCATION_WITH_PARTY, myOption(PlayerConfigOptions.SHARE_LOCATION_WITH_PARTY));
            state.until = Util.getMillis() + 5000;
            PartyCommands.setShareLocation(mc, state.pending);
        });
        btnShare.visible = isSelf;

        // Visibility rules:
        // rank buttons: ADMIN+ only, not on self, not on owner, target rank below local rank
        // (unless owner, who bypasses the rank comparison server-side)
        boolean rankBelowLocal = localIsOwner || member.getRank().ordinal() < localRank.ordinal();
        boolean canRank = !isSelf && !member.isOwner() && localRank.ordinal() >= PartyMemberRank.ADMIN.ordinal() && rankBelowLocal;
        btnRankDown.active = canRank && member.getRank().ordinal() > PartyMemberRank.MEMBER.ordinal();
        btnRankUp.active = canRank && member.getRank().ordinal() < PartyMemberRank.ADMIN.ordinal()
                && (localIsOwner || upRank.ordinal() < localRank.ordinal());
        btnRankDown.visible = canRank;
        btnRankUp.visible = canRank;

        // kick: MODERATOR+ only, not self, not owner, target rank <= local rank (unless
        // owner)
        btnKick.visible = !isSelf && !member.isOwner() && localRank.ordinal() >= PartyMemberRank.MODERATOR.ordinal()
                && (localIsOwner || member.getRank().ordinal() <= localRank.ordinal());

        // transfer: owner only, not on self
        btnTransfer.visible = localIsOwner && !isSelf && !member.isOwner();
    }

    static void drawMember(GuiGraphicsExtractor g, IPartyMemberAPI member, boolean isOnline, Component suffix, int x, int y, int entryHeight) {
        Minecraft mc = Minecraft.getInstance();
        PlayerSkin skin = AvatarCache.getSkin(mc, member.getUUID(), member.getUsername());
        int headSize = 12;
        PlayerFaceExtractor.extractRenderState(g, skin, x + 4, y + (entryHeight - headSize) / 2, headSize);

        MutableComponent dot = Component.literal(isOnline ? "●" : "○")
                .withStyle(isOnline ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY);
        String star = member.isOwner() ? " ★" : "";
        ChatFormatting rankColor = member.getRank().getColor();
        MutableComponent label = Component.literal("")
                .append(dot)
                .append(Component.literal(star + " " + member.getUsername()).withStyle(s -> s.withColor(rankColor)))
                .append(suffix);
        g.text(mc.font, label, x + 22, y + (entryHeight - 8) / 2, 0xFFFFFFFF);
    }

    @Override
    public void extractContent(GuiGraphicsExtractor g, int mouseX, int mouseY, boolean isHovered, float delta) {
        int x = getContentX();
        int y = getContentY();
        int entryWidth = getContentWidth();
        int entryHeight = getContentHeight();

        MutableComponent rankLabel = Component.literal(" [" + member.getRank().name() + "]")
                .withStyle(ChatFormatting.DARK_GRAY);
        boolean online = isOnline(member);
        drawMember(g, member, online, rankLabel, x, y, entryHeight);

        // ── Position & render buttons from right edge ──────────────────────
        int rightEdge = x + entryWidth - 2;
        int btnY = y + (entryHeight - BTN_H) / 2;

        if (btnShare.visible) {
            IPlayerConfigStringableOptionClientStorageAPI<Boolean> option = myOption(PlayerConfigOptions.SHARE_LOCATION_WITH_PARTY);
            boolean syncing = configSyncing();
            boolean mutable = option.isMutable();
            String label = syncing ? "screen.opacui.sharing_loading"
                    : isOn(PlayerConfigOptions.SHARE_LOCATION_WITH_PARTY, option) ? "screen.opacui.sharing_on" : "screen.opacui.sharing_off";
            String tooltip = syncing ? "screen.opacui.tooltip.sharing_loading"
                    : mutable ? "screen.opacui.tooltip.sharing" : "screen.opacui.tooltip.sharing_locked";
            if (!label.equals(shownLabel) || !tooltip.equals(shownTooltip)) {
                shownLabel = label;
                shownTooltip = tooltip;
                btnShare.setMessage(Component.translatable(label));
                btnShare.setTooltip(Tooltip.create(Component.translatable(tooltip)));
            }
            btnShare.active = !syncing && mutable && ShareState.get().pending == null;
            btnShare.setX(rightEdge - BTN_W);
            btnShare.setY(btnY);
            btnShare.extractRenderState(g, mouseX, mouseY, delta);
            rightEdge -= BTN_W + BTN_GAP;
        }
        if (btnTransfer.visible) {
            btnTransfer.setX(rightEdge - BTN_W);
            btnTransfer.setY(btnY);
            btnTransfer.extractRenderState(g, mouseX, mouseY, delta);
            rightEdge -= BTN_W + BTN_GAP;
        }
        if (btnKick.visible) {
            btnKick.setX(rightEdge - BTN_W);
            btnKick.setY(btnY);
            btnKick.extractRenderState(g, mouseX, mouseY, delta);
            rightEdge -= BTN_W + BTN_GAP;
        }
        if (btnRankUp.visible) {
            int half = BTN_W / 2 - 1;
            btnRankUp.setX(rightEdge - half);
            btnRankUp.setY(btnY);
            btnRankUp.extractRenderState(g, mouseX, mouseY, delta);
            btnRankDown.setX(rightEdge - half - BTN_GAP - half);
            btnRankDown.setY(btnY);
            btnRankDown.extractRenderState(g, mouseX, mouseY, delta);
        }

        if (isHovered && online && !btnShare.isHovered() && !btnTransfer.isHovered() && !btnKick.isHovered() && !btnRankUp.isHovered() && !btnRankDown.isHovered()) {
            g.setComponentTooltipForNextFrame(Minecraft.getInstance().font, locationLines(), mouseX, mouseY);
        }
    }

    static boolean isOnline(IPartyMemberAPI member) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        return connection != null && connection.getPlayerInfo(member.getUUID()) != null;
    }

    // Until OPAC's login sync finishes, options read as OPAC's built-in defaults rather than the server's.
    private static boolean configSyncing() {
        IPlayerConfigClientStorageManagerAPI configs = OpenPACClientAPI.get().getPlayerConfigClientStorageManager();
        return configs.getMyPlayerConfig() instanceof IPlayerConfigClientStorage<?> mine && mine.isSyncInProgress()
                || configs.getDefaultPlayerConfig() instanceof IPlayerConfigClientStorage<?> defaults && defaults.isSyncInProgress();
    }

    private static IPlayerConfigStringableOptionClientStorageAPI<Boolean> myOption(IPlayerConfigOptionSpecAPI<Boolean> spec) {
        return OpenPACClientAPI.get().getPlayerConfigClientStorageManager().getMyPlayerConfig().getOption(spec);
    }

    private static boolean isOn(IPlayerConfigOptionSpecAPI<Boolean> spec, IPlayerConfigStringableOptionClientStorageAPI<Boolean> mine) {
        Boolean value = mine.getValue();
        return value != null ? value : serverDefault(spec);
    }

    private static boolean serverDefault(IPlayerConfigOptionSpecAPI<Boolean> spec) {
        Boolean value = OpenPACClientAPI.get().getPlayerConfigClientStorageManager().getDefaultPlayerConfig().getOption(spec).getValue();
        return value == null || value;
    }

    private List<Component> locationLines() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return List.of();
        Identifier dimension;
        double px, py, pz;
        if (member.getUUID().equals(mc.player.getUUID())) {
            dimension = mc.level.dimension().identifier();
            px = mc.player.getX();
            py = mc.player.getY();
            pz = mc.player.getZ();
        } else {
            IPartyMemberDynamicInfoSyncableAPI info = OpenPACClientAPI.get().getClientPartyStorage()
                    .getPartyMemberDynamicInfoSyncableStorage().getForPlayer(member.getUUID());
            if (info == null || info.getDimension() == null) {
                boolean receiving = configSyncing() || isOn(PlayerConfigOptions.RECEIVE_LOCATIONS_FROM_PARTY, myOption(PlayerConfigOptions.RECEIVE_LOCATIONS_FROM_PARTY));
                return List.of(Component.translatable(receiving ? "screen.opacui.members.location_hidden"
                        : "screen.opacui.members.location_off").withStyle(ChatFormatting.GRAY));
            }
            dimension = info.getDimension();
            px = info.getX();
            py = info.getY();
            pz = info.getZ();
        }
        Component where = Component.translatable("screen.opacui.members.location", PartyScreen.dimensionName(dimension),
                (int) Math.floor(px), (int) Math.floor(py), (int) Math.floor(pz));
        if (!dimension.equals(mc.level.dimension().identifier()) || member.getUUID().equals(mc.player.getUUID()))
            return List.of(where);
        int distance = (int) Math.sqrt(mc.player.distanceToSqr(px, py, pz));
        return List.of(where, Component.translatable("screen.opacui.members.distance", distance).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children() {
        return List.of(btnRankDown, btnRankUp, btnKick, btnTransfer, btnShare);
    }

    @Override
    public List<? extends net.minecraft.client.gui.narration.NarratableEntry> narratables() {
        return List.of(btnRankDown, btnRankUp, btnKick, btnTransfer, btnShare);
    }

    // ── Rank cycling helpers ──────────────────────────────────────────────

    private static PartyMemberRank rankUp(PartyMemberRank current) {
        int next = current.ordinal() + 1;
        PartyMemberRank[] values = PartyMemberRank.values();
        return next < values.length ? values[next] : null;
    }

    private static PartyMemberRank rankDown(PartyMemberRank current) {
        int prev = current.ordinal() - 1;
        return prev >= 0 ? PartyMemberRank.values()[prev] : null;
    }
}
