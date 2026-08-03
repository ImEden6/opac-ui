package mervyn.opacui.client.gui;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.gui.AbstractConfigScreen;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import mervyn.opacui.client.gui.entry.AllyEntry;
import mervyn.opacui.client.gui.entry.InviteEntry;
import mervyn.opacui.client.gui.entry.MemberEntry;
import mervyn.opacui.client.gui.widget.PartyActionBarWidget;
import mervyn.opacui.client.gui.widget.PartyHeaderWidget;
import mervyn.opacui.client.gui.widget.SuggestionDropdown;
import mervyn.opacui.client.util.PartyCommands;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Top-level party management screen.
 */
public class PartyScreen extends Screen {

    private static final Logger LOGGER = LoggerFactory.getLogger("opacui");
    private static final int CLOTH_BOTTOM_MARGIN = 72;
    private static final int BAR_Y_OFFSET = 68;
    private static final int BOTTOM_Y_OFFSET = 44;

    private final Screen parent;

    // ── Extracted Widgets ────────────────────────────────────────────────
    private final PartyHeaderWidget headerWidget = new PartyHeaderWidget();
    private final PartyActionBarWidget actionBarWidget = new PartyActionBarWidget();
    private final SuggestionDropdown suggestionDropdown = new SuggestionDropdown();

    /** Cloth Config sub-screen that holds tabbed lists. */
    private Screen clothScreen;

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
        if (clothScreen instanceof AbstractConfigScreen acs) {
            savedTabIndex = acs.selectedCategoryIndex;
        }
        super.init();
        clearWidgets();
        clothScreen = null;

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

    /** State B — in a party. Build Cloth Config tabbed screen & widgets. */
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

        // 2. Cloth Config Screen
        buildClothScreen(party, localUUID, localRank, localIsOwner, canModeratorPlus);

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

    private void buildClothScreen(IClientPartyAPI party, UUID localUUID, PartyMemberRank localRank, boolean localIsOwner, boolean canModeratorPlus) {
        Minecraft mc = Minecraft.getInstance();
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(localIsOwner ? Component.empty() : Component.translatable("screen.opacui.party_manager"))
                .setSavingRunnable(() -> {})
                .setDoesConfirmSave(false)
                .setTransparentBackground(true)
                .setAfterInitConsumer(screen -> screen.children().stream()
                        .filter(net.minecraft.client.gui.components.AbstractWidget.class::isInstance)
                        .map(net.minecraft.client.gui.components.AbstractWidget.class::cast)
                        .filter(w -> w.getY() >= height - CLOTH_BOTTOM_MARGIN - 30)
                        .forEach(w -> {
                            w.visible = false;
                            w.active = false;
                        }));

        // Members Tab
        ConfigCategory membersCategory = builder.getOrCreateCategory(Component.translatable("screen.opacui.tab_members"));
        Set<UUID> onlinePlayers = minecraft.getConnection() != null
                ? minecraft.getConnection().getOnlinePlayers().stream().map(pi -> pi.getProfile().getId()).collect(Collectors.toSet())
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
                    membersCategory.addEntry(new MemberEntry(member, localRank, localIsOwner, isSelf, isOnline, msg -> {
                        showFeedback(msg);
                        scheduleActionRefresh();
                    }));
                });

        // Invites Tab
        ConfigCategory invitesCategory = builder.getOrCreateCategory(Component.translatable("screen.opacui.tab_invites"));
        if (party.getInviteCount() == 0) {
            invitesCategory.addEntry(new PlaceholderEntry(Component.translatable("screen.opacui.empty_invites")));
        } else {
            party.getInvitedPlayersStream().forEach(invite -> invitesCategory.addEntry(new InviteEntry(invite, msg -> {
                showFeedback(msg);
                scheduleActionRefresh();
            })));
        }

        // Allies Tab
        ConfigCategory alliesCategory = builder.getOrCreateCategory(Component.translatable("screen.opacui.tab_allies"));
        if (party.getAllyCount() == 0) {
            alliesCategory.addEntry(new PlaceholderEntry(Component.translatable("screen.opacui.empty_allies")));
        } else {
            IClientPartyStorageAPI storage = OpenPACClientAPI.get().getClientPartyStorage();
            party.getAllyPartiesStream().forEach(ally -> {
                var allyInfo = storage.getAllyInfoStorage().get(ally.getPartyId());
                if (allyInfo != null) {
                    alliesCategory.addEntry(new AllyEntry(allyInfo, canModeratorPlus, msg -> {
                        showFeedback(msg);
                        scheduleActionRefresh();
                    }));
                }
            });
        }

        clothScreen = builder.build();
        if (savedTabIndex > 0 && clothScreen instanceof AbstractConfigScreen acs) {
            acs.selectedCategoryIndex = savedTabIndex;
        }
        clothScreen.init(mc, width, height - CLOTH_BOTTOM_MARGIN);
    }

    /** Dynamic in-place list update without full screen tear-down. */
    private void refreshPartyLists() {
        IClientPartyAPI party = getParty();
        if (party == null) {
            if (clothScreen != null) init();
            return;
        }
        if (clothScreen == null) {
            init();
            return;
        }

        if (clothScreen instanceof AbstractConfigScreen acs) {
            savedTabIndex = acs.selectedCategoryIndex;
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

        buildClothScreen(party, localUUID, localRank, localIsOwner, canModeratorPlus);
        actionBarWidget.updateState(localIsOwner, canModeratorPlus, savedTabIndex, party);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Render & Tick
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, 0x90101010);

        if (clothScreen != null) {
            clothScreen.render(g, mouseX, mouseY, delta);
        } else {
            g.drawCenteredString(font, title, width / 2, 16, 0xFFFFFFFF);
            g.drawCenteredString(font, Component.translatable("screen.opacui.no_party"), width / 2, height / 2 - 20, 0xFFAAAAAA);
        }

        if (feedbackTimer > 0 && feedbackText != null) {
            g.drawCenteredString(font, feedbackText, width / 2, height - BAR_Y_OFFSET - 14, 0xFFFFAA00);
        }

        super.render(g, mouseX, mouseY, delta);
        suggestionDropdown.render(g, font, minecraft, width / 2 - 132, height - BAR_Y_OFFSET, 198);
    }

    @Override
    public void tick() {
        if (feedbackTimer > 0 && --feedbackTimer == 0) {
            feedbackText = null;
        }

        if (clothScreen != null) {
            clothScreen.tick();
        }

        if (clothScreen instanceof AbstractConfigScreen acs) {
            int currentTab = acs.selectedCategoryIndex;
            if (currentTab != savedTabIndex) {
                savedTabIndex = currentTab;
                actionBarWidget.updateForTab(savedTabIndex);
            }
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
                ? mc.getConnection().getOnlinePlayers().stream().map(pi -> pi.getProfile().getId()).collect(Collectors.toSet())
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
    public boolean mouseClicked(double x, double y, int btn) {
        if (suggestionDropdown.mouseClicked(x, y, width / 2 - 132, height - BAR_Y_OFFSET, 198, font, this::populateInputBox)) {
            return true;
        }

        if (clothScreen != null && y < height - CLOTH_BOTTOM_MARGIN) {
            boolean handled = clothScreen.mouseClicked(x, y, btn);
            if (handled) {
                releaseOuterTextFocus();
            }
            return handled || super.mouseClicked(x, y, btn);
        }
        return super.mouseClicked(x, y, btn);
    }

    /** Releases focus from PartyScreen's own text boxes so keystrokes route into clothScreen. */
    private void releaseOuterTextFocus() {
        setFocused(null);
        if (headerWidget.getPartyNameBox() != null) headerWidget.getPartyNameBox().setFocused(false);
        if (actionBarWidget.getInviteBox() != null) actionBarWidget.getInviteBox().setFocused(false);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double delta) {
        if (clothScreen != null && y < height - CLOTH_BOTTOM_MARGIN) {
            return clothScreen.mouseScrolled(x, y, delta);
        }
        return super.mouseScrolled(x, y, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (suggestionDropdown.keyPressed(keyCode, this::populateInputBox)) {
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ENTER) {
            if (headerWidget.handleEnterKey(minecraft, this::showFeedback, this::scheduleActionRefresh)) {
                return true;
            }
            if (actionBarWidget.handleEnterKey()) {
                return true;
            }
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }

        if (isTextBoxFocused()) {
            if (super.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }

        if (clothScreen != null) {
            return clothScreen.keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (isTextBoxFocused()) {
            if (super.charTyped(chr, modifiers)) {
                return true;
            }
        }
        if (clothScreen != null) {
            return clothScreen.charTyped(chr, modifiers) || super.charTyped(chr, modifiers);
        }
        return super.charTyped(chr, modifiers);
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

    private static class PlaceholderEntry extends TooltipListEntry<Void> {
        private static final int ENTRY_HEIGHT = 20;
        private final Component text;

        @SuppressWarnings("deprecation")
        public PlaceholderEntry(Component text) {
            super(Component.empty(), null);
            this.text = text;
        }

        @Override
        public void render(GuiGraphics g, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
            g.drawCenteredString(Minecraft.getInstance().font, text, x + entryWidth / 2, y + (entryHeight - 8) / 2, 0xFF888888);
        }

        @Override
        public int getItemHeight() { return ENTRY_HEIGHT; }

        @Override
        public Void getValue() { return null; }

        @Override
        public Optional<Void> getDefaultValue() { return Optional.empty(); }

        @Override
        public boolean isEdited() { return false; }

        @Override
        public void save() {}

        @Override
        public List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children() { return List.of(); }

        @Override
        public List<? extends net.minecraft.client.gui.narration.NarratableEntry> narratables() { return List.of(); }

        @Override
        public Optional<Component[]> getTooltip() { return Optional.empty(); }
    }
}
