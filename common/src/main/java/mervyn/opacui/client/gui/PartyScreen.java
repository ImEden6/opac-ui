package mervyn.opacui.client.gui;

import mervyn.opacui.client.gui.entry.AllyEntry;
import mervyn.opacui.client.gui.entry.InviteEntry;
import mervyn.opacui.client.gui.entry.MemberEntry;
import mervyn.opacui.client.gui.list.AbstractPartyEntry;
import mervyn.opacui.client.gui.list.PartyEntryList;
import mervyn.opacui.client.gui.widget.PartyActionBarWidget;
import mervyn.opacui.client.gui.widget.PartyHeaderWidget;
import mervyn.opacui.client.gui.widget.SuggestionDropdown;
import mervyn.opacui.client.util.PartyCommands;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xaero.pac.client.api.OpenPACClientAPI;
import xaero.pac.client.parties.party.api.IClientPartyAPI;
import xaero.pac.client.parties.party.api.IClientPartyStorageAPI;
import xaero.pac.client.world.capability.api.ClientWorldCapabilityTypes;
import xaero.pac.common.parties.party.member.PartyMemberRank;
import xaero.pac.common.parties.party.member.api.IPartyMemberAPI;

import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Top-level party management screen.
 */
public class PartyScreen extends Screen {

    private static final Logger LOGGER = LoggerFactory.getLogger("opacui");
    private static final int LIST_BOTTOM_MARGIN = 72;
    private static final int BAR_Y_OFFSET = 68;
    private static final int BOTTOM_Y_OFFSET = 44;

    private static final int TAB_BAR_Y = 30;
    private static final int TAB_BTN_WIDTH = 90;
    private static final int TAB_BTN_HEIGHT = 20;
    private static final int TAB_BTN_GAP = 4;
    private static final int LIST_TOP = TAB_BAR_Y + TAB_BTN_HEIGHT + 4;

    private final Screen parent;

    // ── Extracted Widgets ────────────────────────────────────────────────
    private final PartyHeaderWidget headerWidget = new PartyHeaderWidget();
    private final PartyActionBarWidget actionBarWidget = new PartyActionBarWidget();
    private final SuggestionDropdown suggestionDropdown = new SuggestionDropdown();

    /** Tab lists — rebuilt fresh whenever the underlying party data changes. */
    private PartyEntryList membersList;
    private PartyEntryList invitesList;
    private PartyEntryList alliesList;
    private PartyEntryList activeListWidget;

    private Button tabMembersBtn;
    private Button tabInvitesBtn;
    private Button tabAlliesBtn;

    // ── Feedback ──────────────────────────────────────────────────────────
    private Component feedbackText;
    private int feedbackTimer;

    // ── Tab & State Tracking ─────────────────────────────────────────────
    private int savedTabIndex;
    private boolean lastPartyPresent;
    private int lastMemberCount;
    private int lastInviteCount;
    private int lastAllyCount;
    private boolean lastIsOwner;
    private PartyMemberRank lastLocalRank;
    private Set<UUID> lastOnlinePlayerIds = Set.of();
    private int actionRefreshTicks;

    public PartyScreen(Screen parent) {
        super(Component.translatable("screen.opacui.party_manager"));
        this.parent = parent;
    }

    public void populateInputBox(String text) {
        actionBarWidget.populateInputBox(text);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Init
    // ─────────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        super.init();
        clearWidgets();
        membersList = null;
        invitesList = null;
        alliesList = null;
        activeListWidget = null;

        IClientPartyAPI party = getParty();
        Minecraft mc = Minecraft.getInstance();
        UUID localUUID = mc.player == null ? null : mc.player.getUUID();
        IPartyMemberAPI localMember = (party != null && localUUID != null) ? party.getMemberInfo(localUUID) : null;
        boolean localIsOwner = localMember != null && localMember.isOwner();

        lastPartyPresent = party != null;
        lastMemberCount = party != null ? party.getMemberCount() : 0;
        lastInviteCount = party != null ? party.getInviteCount() : 0;
        lastAllyCount = party != null ? party.getAllyCount() : 0;
        lastIsOwner = localIsOwner;
        lastLocalRank = localMember != null ? localMember.getRank() : PartyMemberRank.MEMBER;

        if (party == null) {
            initNoParty();
        } else {
            initInParty(party);
        }
    }

    /** State A — not in a party. */
    private void initNoParty() {
        addRenderableWidget(Button.builder(
                Component.translatable("screen.opacui.create_party"),
                b -> {
                    PartyCommands.createParty(minecraft);
                    scheduleActionRefresh();
                }).bounds(width / 2 - 75, height / 2, 150, 20).build());

        addRenderableWidget(Button.builder(
                Component.translatable("gui.cancel"),
                b -> minecraft.setScreen(parent)).bounds(width / 2 - 50, height / 2 + 28, 100, 20).build());
    }

    /** State B — in a party. Build tab bar, lists & widgets. */
    private void initInParty(IClientPartyAPI party) {
        Minecraft mc = Minecraft.getInstance();
        UUID localUUID = mc.player == null ? null : mc.player.getUUID();

        IPartyMemberAPI localMember = localUUID != null ? party.getMemberInfo(localUUID) : null;
        PartyMemberRank localRank = localMember != null ? localMember.getRank() : PartyMemberRank.MEMBER;
        boolean localIsOwner = localMember != null && localMember.isOwner();
        boolean canModeratorPlus = localRank.ordinal() >= PartyMemberRank.MODERATOR.ordinal();

        // 1. Header Widget
        String partyName = OpenPACClientAPI.get().getClientPartyStorage().getPartyName();
        headerWidget.init(this, font, width, localIsOwner, partyName, this::showFeedback, this::scheduleActionRefresh);
        if (headerWidget.getBtnConfigGear() != null) addRenderableWidget(headerWidget.getBtnConfigGear());
        if (headerWidget.getPartyNameBox() != null) addRenderableWidget(headerWidget.getPartyNameBox());
        if (headerWidget.getBtnRename() != null) addRenderableWidget(headerWidget.getBtnRename());

        // 2. Tab bar + lists
        buildLists(party, localUUID, localRank, localIsOwner, canModeratorPlus);
        addTabBar();
        showTab(savedTabIndex);

        // 3. Action Bar Widget
        actionBarWidget.init(
                this, font, width, height, BAR_Y_OFFSET, BOTTOM_Y_OFFSET,
                localIsOwner, canModeratorPlus, savedTabIndex, party,
                this::showFeedback, this::scheduleActionRefresh, this::updateSuggestions
        );
        if (actionBarWidget.getInviteBox() != null) addRenderableWidget(actionBarWidget.getInviteBox());
        if (actionBarWidget.getBtnSendInvite() != null) addRenderableWidget(actionBarWidget.getBtnSendInvite());
        if (actionBarWidget.getBtnLeaveDisband() != null) addRenderableWidget(actionBarWidget.getBtnLeaveDisband());
        if (actionBarWidget.getBtnDone() != null) addRenderableWidget(actionBarWidget.getBtnDone());

        if (actionBarWidget.getInviteBox() != null) {
            setFocused(actionBarWidget.getInviteBox());
        }
    }

    private void addTabBar() {
        int totalWidth = 3 * TAB_BTN_WIDTH + 2 * TAB_BTN_GAP;
        int startX = width / 2 - totalWidth / 2;

        tabMembersBtn = Button.builder(Component.translatable("screen.opacui.tab_members"), b -> showTab(0))
                .bounds(startX, TAB_BAR_Y, TAB_BTN_WIDTH, TAB_BTN_HEIGHT).build();
        tabInvitesBtn = Button.builder(Component.translatable("screen.opacui.tab_invites"), b -> showTab(1))
                .bounds(startX + (TAB_BTN_WIDTH + TAB_BTN_GAP), TAB_BAR_Y, TAB_BTN_WIDTH, TAB_BTN_HEIGHT).build();
        tabAlliesBtn = Button.builder(Component.translatable("screen.opacui.tab_allies"), b -> showTab(2))
                .bounds(startX + 2 * (TAB_BTN_WIDTH + TAB_BTN_GAP), TAB_BAR_Y, TAB_BTN_WIDTH, TAB_BTN_HEIGHT).build();

        addRenderableWidget(tabMembersBtn);
        addRenderableWidget(tabInvitesBtn);
        addRenderableWidget(tabAlliesBtn);
    }

    /** Switches the visible tab: swaps which list is a registered widget, updates button/action-bar state. */
    private void showTab(int index) {
        savedTabIndex = index;
        if (activeListWidget != null) {
            removeWidget(activeListWidget);
        }
        activeListWidget = switch (index) {
            case 1 -> invitesList;
            case 2 -> alliesList;
            default -> membersList;
        };
        if (activeListWidget != null) {
            addRenderableWidget(activeListWidget);
        }
        if (tabMembersBtn != null) tabMembersBtn.active = index != 0;
        if (tabInvitesBtn != null) tabInvitesBtn.active = index != 1;
        if (tabAlliesBtn != null) tabAlliesBtn.active = index != 2;
        actionBarWidget.updateForTab(index);
    }

    private void buildLists(IClientPartyAPI party, UUID localUUID, PartyMemberRank localRank, boolean localIsOwner, boolean canModeratorPlus) {
        Minecraft mc = Minecraft.getInstance();
        int listHeight = (height - LIST_BOTTOM_MARGIN) - LIST_TOP;

        // Members Tab
        membersList = new PartyEntryList(mc, width, listHeight, LIST_TOP);
        Set<UUID> onlinePlayers = minecraft.getConnection() != null
                ? minecraft.getConnection().getOnlinePlayers().stream().map(pi -> pi.getProfile().id()).collect(Collectors.toSet())
                : Set.of();

        party.getMemberInfoStream()
                .sorted((a, b) -> {
                    if (a.isOwner()) return -1;
                    if (b.isOwner()) return 1;
                    return b.getRank().ordinal() - a.getRank().ordinal();
                })
                .forEach(member -> {
                    boolean isSelf = Objects.equals(member.getUUID(), localUUID);
                    boolean isOnline = onlinePlayers.contains(member.getUUID());
                    membersList.addRow(new MemberEntry(member, localRank, localIsOwner, isSelf, isOnline, msg -> {
                        showFeedback(msg);
                        scheduleActionRefresh();
                    }));
                });

        // Invites Tab
        invitesList = new PartyEntryList(mc, width, listHeight, LIST_TOP);
        if (party.getInviteCount() == 0) {
            invitesList.addRow(new PlaceholderEntry(Component.translatable("screen.opacui.empty_invites")));
        } else {
            party.getInvitedPlayersStream().forEach(invite -> invitesList.addRow(new InviteEntry(invite, msg -> {
                showFeedback(msg);
                scheduleActionRefresh();
            })));
        }

        // Allies Tab
        alliesList = new PartyEntryList(mc, width, listHeight, LIST_TOP);
        if (party.getAllyCount() == 0) {
            alliesList.addRow(new PlaceholderEntry(Component.translatable("screen.opacui.empty_allies")));
        } else {
            IClientPartyStorageAPI storage = OpenPACClientAPI.get().getClientPartyStorage();
            party.getAllyPartiesStream().forEach(ally -> {
                var allyInfo = storage.getAllyInfoStorage().get(ally.getPartyId());
                if (allyInfo != null) {
                    alliesList.addRow(new AllyEntry(allyInfo, canModeratorPlus, msg -> {
                        showFeedback(msg);
                        scheduleActionRefresh();
                    }));
                }
            });
        }
    }

    /** Dynamic in-place list update without full screen tear-down. */
    private void refreshPartyLists() {
        IClientPartyAPI party = getParty();
        if (party == null) {
            if (membersList != null) init();
            return;
        }
        if (membersList == null) {
            init();
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        UUID localUUID = mc.player == null ? null : mc.player.getUUID();
        IPartyMemberAPI localMember = localUUID != null ? party.getMemberInfo(localUUID) : null;
        PartyMemberRank localRank = localMember != null ? localMember.getRank() : PartyMemberRank.MEMBER;
        boolean localIsOwner = localMember != null && localMember.isOwner();
        boolean canModeratorPlus = localRank.ordinal() >= PartyMemberRank.MODERATOR.ordinal();

        if (localIsOwner != lastIsOwner) {
            lastIsOwner = localIsOwner;
            init();
            return;
        }

        buildLists(party, localUUID, localRank, localIsOwner, canModeratorPlus);
        showTab(savedTabIndex);
        actionBarWidget.updateState(localIsOwner, canModeratorPlus, savedTabIndex, party);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Render & Tick
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        super.extractBackground(g, mouseX, mouseY, partial);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        if (membersList != null) {
            if (!lastIsOwner) {
                g.centeredText(font, title, width / 2, 18, -1);
            }
        } else {
            g.centeredText(font, title, width / 2, 16, 0xFFFFFFFF);
            g.centeredText(font, Component.translatable("screen.opacui.no_party"), width / 2, height / 2 - 20, 0xFFAAAAAA);
        }

        if (feedbackTimer > 0 && feedbackText != null) {
            g.centeredText(font, feedbackText, width / 2, height - BAR_Y_OFFSET - 14, 0xFFFFAA00);
        }

        super.extractRenderState(g, mouseX, mouseY, partial);
        suggestionDropdown.extractContent(g, font, minecraft, width / 2 - 132, height - BAR_Y_OFFSET, 198);
    }

    @Override
    public void tick() {
        if (feedbackTimer > 0 && --feedbackTimer == 0) {
            feedbackText = null;
        }

        IClientPartyAPI party = getParty();
        Minecraft mc = Minecraft.getInstance();
        UUID localUUID = mc.player == null ? null : mc.player.getUUID();
        IPartyMemberAPI localMember = (party != null && localUUID != null) ? party.getMemberInfo(localUUID) : null;
        boolean localIsOwner = localMember != null && localMember.isOwner();
        PartyMemberRank localRank = localMember != null ? localMember.getRank() : PartyMemberRank.MEMBER;

        boolean isPresent = party != null;
        int memberCount = party != null ? party.getMemberCount() : 0;
        int inviteCount = party != null ? party.getInviteCount() : 0;
        int allyCount = party != null ? party.getAllyCount() : 0;

        if (isPresent != lastPartyPresent || memberCount != lastMemberCount || inviteCount != lastInviteCount || allyCount != lastAllyCount || localIsOwner != lastIsOwner || localRank != lastLocalRank) {
            lastPartyPresent = isPresent;
            lastMemberCount = memberCount;
            lastInviteCount = inviteCount;
            lastAllyCount = allyCount;
            lastIsOwner = localIsOwner;
            lastLocalRank = localRank;
            init();
            return;
        }

        Set<UUID> onlinePlayerIds = party != null && mc.getConnection() != null
                ? mc.getConnection().getOnlinePlayers().stream().map(pi -> pi.getProfile().id()).collect(Collectors.toSet())
                : Set.of();
        if (!onlinePlayerIds.equals(lastOnlinePlayerIds)) {
            lastOnlinePlayerIds = onlinePlayerIds;
            refreshPartyLists();
        }

        if (actionRefreshTicks > 0) {
            actionRefreshTicks--;
            if (actionRefreshTicks == 0) {
                refreshPartyLists();
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Input Event Handling
    // ─────────────────────────────────────────────────────────────────────

    private boolean isTextBoxFocused() {
        return headerWidget.isFocused() || actionBarWidget.isFocused();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (suggestionDropdown.mouseClicked(event.x(), event.y(), width / 2 - 132, height - BAR_Y_OFFSET, 198, font, this::populateInputBox)) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (suggestionDropdown.keyPressed(event.key(), this::populateInputBox)) {
            return true;
        }

        if (event.key() == GLFW.GLFW_KEY_ENTER) {
            if (headerWidget.handleEnterKey(minecraft, this::showFeedback, this::scheduleActionRefresh)) {
                return true;
            }
            if (actionBarWidget.handleEnterKey(event)) {
                return true;
            }
        }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }

        if (isTextBoxFocused()) {
            if (super.keyPressed(event)) {
                return true;
            }
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (isTextBoxFocused()) {
            if (super.charTyped(event)) {
                return true;
            }
        }
        return super.charTyped(event);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────

    private void showFeedback(Component text) {
        feedbackText = text;
        feedbackTimer = 60;
    }

    private void scheduleActionRefresh() {
        actionRefreshTicks = 6;
        refreshPartyLists();
    }

    private void updateSuggestions(String text) {
        IClientPartyAPI party = getParty();
        Set<String> memberNames = party != null
                ? party.getMemberInfoStream().map(IPartyMemberAPI::getUsername).collect(Collectors.toSet())
                : Set.of();
        suggestionDropdown.update(text, minecraft, memberNames);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    private static IClientPartyAPI getParty() {
        try {
            OpenPACClientAPI api = OpenPACClientAPI.get();
            if (api == null) return null;
            IClientPartyStorageAPI storage = api.getClientPartyStorage();
            if (storage == null || storage.isLoading()) return null;
            return storage.getParty();
        } catch (Exception e) {
            LOGGER.warn("Failed to get party", e);
            return null;
        }
    }

    public static boolean isAvailable(Minecraft mc) {
        if (mc.level == null || mc.player == null) return false;
        try {
            var cap = OpenPACClientAPI.get().getCapabilityHelper().getCapability(mc.level, ClientWorldCapabilityTypes.MAIN_CAP);
            if (cap == null) return false;
            var worldData = cap.getClientWorldData();
            return worldData.serverHasMod() && worldData.serverHasPartiesEnabled();
        } catch (Exception e) {
            LOGGER.warn("Failed to check party availability", e);
            return false;
        }
    }

    private static class PlaceholderEntry extends AbstractPartyEntry {
        private final Component text;

        public PlaceholderEntry(Component text) {
            this.text = text;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor g, int mouseX, int mouseY, boolean isHovered, float delta) {
            g.centeredText(Minecraft.getInstance().font, text, getContentX() + getContentWidth() / 2, getContentY() + (getContentHeight() - 8) / 2, 0xFF888888);
        }

        @Override
        public List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children() { return List.of(); }

        @Override
        public List<? extends net.minecraft.client.gui.narration.NarratableEntry> narratables() { return List.of(); }
    }
}
