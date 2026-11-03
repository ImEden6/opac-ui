package mervyn.opacui.client.gui.entry;

import me.shedaniel.math.Point;
import mervyn.opacui.client.gui.PartyScreen;
import mervyn.opacui.client.util.AvatarCache;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.resources.ResourceLocation;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import mervyn.opacui.client.gui.ConfirmActionScreen;
import mervyn.opacui.client.util.PartyCommands;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import xaero.pac.client.api.OpenPACClientAPI;
import xaero.pac.client.player.config.api.v2.IPlayerConfigStringableOptionClientStorageAPI;
import xaero.pac.common.server.player.config.api.v2.IPlayerConfigOptionSpecAPI;
import xaero.pac.common.server.player.config.api.v2.PlayerConfigOptions;
import xaero.pac.common.parties.party.api.IPartyMemberDynamicInfoSyncableAPI;
import xaero.pac.common.parties.party.member.PartyMemberRank;
import xaero.pac.common.parties.party.member.api.IPartyMemberAPI;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * A single scrollable-list row representing one party member.
 * Renders: [★?] username (rank label) [▼Rank] [▲Rank] [Kick] [Transfer?] [Sharing, own row only]
 * Hovering an online member shows where they are, if OPAC has their location.
 *
 * Buttons are only shown if the local player has sufficient rank.
 */
public class MemberEntry extends TooltipListEntry<Void> {

    private static final int ENTRY_HEIGHT = 24;
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
    private Boolean shownShare;
    private boolean shownMutable;
    private final ShareState shareState;

    public static final class ShareState {
        private final Consumer<Component> feedback;
        private Boolean pending;
        private long until;

        public ShareState(Consumer<Component> feedback) {
            this.feedback = feedback;
        }

        public void tick() {
            if (pending == null) return;
            boolean share = isOn(PlayerConfigOptions.SHARE_LOCATION_WITH_PARTY, myOption(PlayerConfigOptions.SHARE_LOCATION_WITH_PARTY));
            if (pending == share) {
                pending = null;
                feedback.accept(Component.translatable(share ? "screen.opacui.feedback.sharing_on" : "screen.opacui.feedback.sharing_off"));
            } else if (Util.getMillis() > until) {
                pending = null;
            }
        }
    }

    @SuppressWarnings("deprecation")
    public MemberEntry(
            IPartyMemberAPI member,
            PartyMemberRank localRank,
            boolean localIsOwner,
            boolean isSelf,
            ShareState shareState,
            Consumer<Component> onAction) {
        super(Component.literal(member.getUsername()), null);
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

        this.shareState = shareState;
        btnShare = EntryButton.create(Component.empty(), b -> {
            if (shareState.pending != null) return;
            shareState.pending = !isOn(PlayerConfigOptions.SHARE_LOCATION_WITH_PARTY, myOption(PlayerConfigOptions.SHARE_LOCATION_WITH_PARTY));
            shareState.until = Util.getMillis() + 5000;
            PartyCommands.setShareLocation(mc, shareState.pending);
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button);
    }

    static void drawMember(GuiGraphics g, IPartyMemberAPI member, boolean isOnline, Component suffix, int x, int y, int entryHeight) {
        Minecraft mc = Minecraft.getInstance();
        ResourceLocation skin = AvatarCache.getSkin(mc, member.getUUID(), member.getUsername());
        int headSize = 12;
        PlayerFaceRenderer.draw(g, skin, x + 4, y + (entryHeight - headSize) / 2, headSize);

        MutableComponent dot = Component.literal(isOnline ? "●" : "○")
                .withStyle(isOnline ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY);
        String star = member.isOwner() ? " ★" : "";
        ChatFormatting rankColor = member.getRank().getColor();
        MutableComponent label = Component.literal("")
                .append(dot)
                .append(Component.literal(star + " " + member.getUsername()).withStyle(s -> s.withColor(rankColor)))
                .append(suffix);
        g.drawString(mc.font, label, x + 22, y + (entryHeight - 8) / 2, 0xFFFFFFFF, false);
    }

    @Override
    public void render(GuiGraphics g, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY,
            boolean isHovered, float delta) {
        MutableComponent rankLabel = Component.literal(" [" + member.getRank().name() + "]")
                .withStyle(ChatFormatting.DARK_GRAY);
        boolean online = isOnline(member);
        drawMember(g, member, online, rankLabel, x, y, entryHeight);

        // ── Position & render buttons from right edge ──────────────────────
        int rightEdge = x + entryWidth - 2;
        int btnY = y + (entryHeight - BTN_H) / 2;

        if (btnShare.visible) {
            IPlayerConfigStringableOptionClientStorageAPI<Boolean> option = myOption(PlayerConfigOptions.SHARE_LOCATION_WITH_PARTY);
            boolean share = isOn(PlayerConfigOptions.SHARE_LOCATION_WITH_PARTY, option);
            boolean mutable = option.isMutable();
            if (shownShare == null || shownShare != share || shownMutable != mutable) {
                shownShare = share;
                shownMutable = mutable;
                btnShare.setMessage(Component.translatable(share ? "screen.opacui.sharing_on" : "screen.opacui.sharing_off"));
                btnShare.setTooltip(Tooltip.create(Component.translatable(mutable
                        ? "screen.opacui.tooltip.sharing" : "screen.opacui.tooltip.sharing_locked")));
            }
            btnShare.active = mutable && shareState.pending == null;
            btnShare.setX(rightEdge - BTN_W);
            btnShare.setY(btnY);
            btnShare.render(g, mouseX, mouseY, delta);
            rightEdge -= BTN_W + BTN_GAP;
        }
        if (btnTransfer.visible) {
            btnTransfer.setX(rightEdge - BTN_W);
            btnTransfer.setY(btnY);
            btnTransfer.render(g, mouseX, mouseY, delta);
            rightEdge -= BTN_W + BTN_GAP;
        }
        if (btnKick.visible) {
            btnKick.setX(rightEdge - BTN_W);
            btnKick.setY(btnY);
            btnKick.render(g, mouseX, mouseY, delta);
            rightEdge -= BTN_W + BTN_GAP;
        }
        if (btnRankUp.visible) {
            int half = BTN_W / 2 - 1;
            btnRankUp.setX(rightEdge - half);
            btnRankUp.setY(btnY);
            btnRankUp.render(g, mouseX, mouseY, delta);
            btnRankDown.setX(rightEdge - half - BTN_GAP - half);
            btnRankDown.setY(btnY);
            btnRankDown.render(g, mouseX, mouseY, delta);
        }

        if (isHovered && online && !btnShare.isHovered() && !btnTransfer.isHovered() && !btnKick.isHovered() && !btnRankUp.isHovered() && !btnRankDown.isHovered()) {
            addTooltip(me.shedaniel.clothconfig2.api.Tooltip.of(new Point(mouseX, mouseY), locationLines()));
        }
    }

    static boolean isOnline(IPartyMemberAPI member) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        return connection != null && connection.getPlayerInfo(member.getUUID()) != null;
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

    private Component[] locationLines() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return new Component[0];
        ResourceLocation dimension;
        double px, py, pz;
        if (member.getUUID().equals(mc.player.getUUID())) {
            dimension = mc.level.dimension().location();
            px = mc.player.getX();
            py = mc.player.getY();
            pz = mc.player.getZ();
        } else {
            IPartyMemberDynamicInfoSyncableAPI info = OpenPACClientAPI.get().getClientPartyStorage()
                    .getPartyMemberDynamicInfoSyncableStorage().getForPlayer(member.getUUID());
            if (info == null || info.getDimension() == null) {
                boolean receiving = isOn(PlayerConfigOptions.RECEIVE_LOCATIONS_FROM_PARTY, myOption(PlayerConfigOptions.RECEIVE_LOCATIONS_FROM_PARTY));
                return new Component[] { Component.translatable(receiving ? "screen.opacui.members.location_hidden"
                        : "screen.opacui.members.location_off").withStyle(ChatFormatting.GRAY) };
            }
            dimension = info.getDimension();
            px = info.getX();
            py = info.getY();
            pz = info.getZ();
        }
        Component where = Component.translatable("screen.opacui.members.location", PartyScreen.dimensionName(dimension),
                (int) Math.floor(px), (int) Math.floor(py), (int) Math.floor(pz));
        if (!dimension.equals(mc.level.dimension().location()) || member.getUUID().equals(mc.player.getUUID()))
            return new Component[] { where };
        int distance = (int) Math.sqrt(mc.player.distanceToSqr(px, py, pz));
        return new Component[] { where, Component.translatable("screen.opacui.members.distance", distance).withStyle(ChatFormatting.GRAY) };
    }

    @Override
    public int getItemHeight() {
        return ENTRY_HEIGHT;
    }

    @Override
    public Void getValue() {
        return null;
    }

    @Override
    public Optional<Void> getDefaultValue() {
        return Optional.empty();
    }

    @Override
    public boolean isEdited() {
        return false;
    }

    @Override
    public void save() {
    }

    @Override
    public List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children() {
        return List.of(btnRankDown, btnRankUp, btnKick, btnTransfer, btnShare);
    }

    @Override
    public List<? extends net.minecraft.client.gui.narration.NarratableEntry> narratables() {
        return List.of(btnRankDown, btnRankUp, btnKick, btnTransfer, btnShare);
    }

    @Override
    public Optional<Component[]> getTooltip() {
        return Optional.empty();
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
