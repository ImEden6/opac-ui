package mervyn.opacui.client.gui;

import net.minecraft.client.gui.components.Tooltip;
import xaero.pac.client.gui.ConfigMenu;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.gui.AbstractConfigScreen;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import mervyn.opacui.client.gui.entry.AllyEntry;
import mervyn.opacui.client.gui.entry.InviteEntry;
import mervyn.opacui.client.gui.entry.MemberEntry;
import mervyn.opacui.client.util.PartyCommands;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import mervyn.opacui.client.util.AvatarCache;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.resources.ResourceLocation;
import xaero.pac.client.parties.party.api.IClientPartyAPI;
import xaero.pac.client.parties.party.api.IClientPartyStorageAPI;
import xaero.pac.client.world.capability.api.ClientWorldCapabilityTypes;
import xaero.pac.client.api.OpenPACClientAPI;
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
    private static final int CLOTH_BOTTOM_MARGIN = 48;
    private static final int BAR_Y_OFFSET = 44;
    private static final int BOTTOM_Y_OFFSET = 20;

    private final Screen parent;

    // ── Widgets for State B header & actions ─────────────────────────────
    private EditBox inviteBox;
    private Button btnSendInvite;

    /**
     * Cloth Config sub-screen that holds the tabbed member/invite/ally lists.
     */
    private Screen clothScreen;

    // ── Feedback ──────────────────────────────────────────────────────────
    private Component feedbackText;
    private int feedbackTimer;

    // ── Invite auto-complete ──────────────────────────────────────────────
    private List<String> suggestions = List.of();
    private int selectedSuggestion = -1;
    private boolean showSuggestions;

    // ── State tracking for auto-refresh ─────────────────────────────
    private boolean lastPartyPresent;
    private int lastMemberCount;
    private int lastInviteCount;
    private int lastAllyCount;
    private int actionRefreshTicks;

    public PartyScreen(Screen parent) {
        super(Component.translatable("screen.opacui.party_manager"));
        this.parent = parent;
    }

    public void populateInputBox(String text) {
        if (inviteBox != null) {
            inviteBox.setValue(text);
            inviteBox.moveCursorToEnd();
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Init
    // ─────────────────────────────────────────────────────────────────────

    private int savedTabIndex;

    @Override
    protected void init() {
        // Save current tab before rebuilding
        if (clothScreen instanceof AbstractConfigScreen acs) {
            savedTabIndex = acs.selectedCategoryIndex;
        }
        super.init();
        clearWidgets();
        clothScreen = null;

        // ── Option A: OPAC Config Gear Button in top-right corner ───────────
        addRenderableWidget(Button.builder(
                Component.literal("⚙"),
                b -> minecraft.setScreen(new ConfigMenu(parent, this))).bounds(width - 24, 6, 18, 18)
                .tooltip(Tooltip.create(Component.literal("OPAC Configuration")))
                .build());

        IClientPartyAPI party = getParty();

        lastPartyPresent = party != null;
        lastMemberCount = party != null ? party.getMemberCount() : 0;
        lastInviteCount = party != null ? party.getInviteCount() : 0;
        lastAllyCount = party != null ? party.getAllyCount() : 0;

        if (party == null) {
            initNoParty();
        } else {
            initInParty(party);
        }
        updateActionBarForTab();
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

    private EditBox partyNameBox;

    /** State B — in a party. Build the Cloth Config tabbed screen. */
    private void initInParty(IClientPartyAPI party) {
        Minecraft mc = Minecraft.getInstance();
        UUID localUUID = mc.player == null ? null : mc.player.getUUID();

        // Determine local player's rank
        IPartyMemberAPI localMember = localUUID != null ? party.getMemberInfo(localUUID) : null;
        PartyMemberRank localRank = localMember != null ? localMember.getRank() : PartyMemberRank.MEMBER;
        boolean localIsOwner = localMember != null && localMember.isOwner();
        boolean canModeratorPlus = localRank.ordinal() >= PartyMemberRank.MODERATOR.ordinal();

        if (localIsOwner) {
            String partyName = OpenPACClientAPI.get().getClientPartyStorage().getPartyName();
            partyNameBox = new EditBox(font, width / 2 - 100, 6, 140, 16, Component.literal("Party Name"));
            partyNameBox.setValue(partyName != null ? partyName : "");
            addRenderableWidget(partyNameBox);

            addRenderableWidget(Button.builder(
                    Component.literal("Rename"),
                    b -> {
                        String newName = partyNameBox.getValue().trim();
                        if (!newName.isEmpty()) {
                            PartyCommands.renameParty(mc, newName);
                            showFeedback(Component.literal("Renamed party to " + newName));
                            scheduleActionRefresh();
                        }
                    }).bounds(width / 2 + 45, 6, 50, 16).build());
        }

        // ── Build Cloth Config screen ─────────────────────────────────────
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent) // cloth closes back into parent screen
                .setTitle(localIsOwner ? Component.empty() : Component.translatable("screen.opacui.party_manager"))
                .setSavingRunnable(() -> {
                }) // no config to save; all actions are immediate
                .setDoesConfirmSave(false);

        // Reserve bottom space for our own action bar
        // (We render cloth in the upper portion; our buttons sit at the bottom)

        // ── Members tab ───────────────────────────────────────────────────
        ConfigCategory membersCategory = builder.getOrCreateCategory(
                Component.translatable("screen.opacui.tab_members"));

        Set<UUID> onlinePlayers = minecraft.getConnection() != null
                ? minecraft.getConnection().getOnlinePlayers().stream()
                        .map(pi -> pi.getProfile().getId())
                        .collect(Collectors.toSet())
                : Set.of();

        party.getMemberInfoStream()
                .sorted((a, b) -> {
                    if (a.isOwner())
                        return -1;
                    if (b.isOwner())
                        return 1;
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

        // ── Invites tab ───────────────────────────────────────────────────
        ConfigCategory invitesCategory = builder.getOrCreateCategory(
                Component.translatable("screen.opacui.tab_invites"));

        if (party.getInviteCount() == 0) {
            invitesCategory.addEntry(new PlaceholderEntry(Component.translatable("screen.opacui.empty_invites")));
        } else {
            party.getInvitedPlayersStream()
                    .forEach(invite -> invitesCategory.addEntry(new InviteEntry(invite, msg -> {
                        showFeedback(msg);
                        scheduleActionRefresh();
                    })));
        }

        // ── Allies tab ────────────────────────────────────────────────────
        ConfigCategory alliesCategory = builder.getOrCreateCategory(
                Component.translatable("screen.opacui.tab_allies"));

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

        // ── Invite / Add Ally bar ──────────────────────────────────────────
        int barY = height - BAR_Y_OFFSET;
        inviteBox = new EditBox(font, width / 2 - 150, barY, 198, 20,
                Component.translatable("screen.opacui.invite_placeholder"));
        inviteBox.setMaxLength(32);
        inviteBox.setHint(Component.translatable("screen.opacui.invite_placeholder"));
        inviteBox.setResponder(this::updateSuggestions);
        addRenderableWidget(inviteBox);

        btnSendInvite = addRenderableWidget(Button.builder(
                savedTabIndex == 2
                        ? Component.literal("Add Ally")
                        : Component.translatable("screen.opacui.invite"),
                b -> {
                    String name = inviteBox.getValue().trim();
                    if (!name.isEmpty()) {
                        boolean isOnline = mc.getConnection() != null && mc.getConnection().getOnlinePlayers().stream()
                                .anyMatch(pi -> pi.getProfile().getName().equalsIgnoreCase(name));
                        if (!isOnline) {
                            showFeedback(Component.translatable("screen.opacui.feedback.player_not_online", name));
                            return;
                        }

                        boolean isAlreadyMember = party.getMemberInfoStream()
                                .anyMatch(m -> m.getUsername().equalsIgnoreCase(name));
                        if (isAlreadyMember) {
                            showFeedback(Component.translatable("screen.opacui.feedback.already_in_party", name));
                            return;
                        }

                        if (savedTabIndex == 2) {
                            PartyCommands.addAlly(mc, name);
                            showFeedback(Component.translatable("screen.opacui.feedback.ally_added", name));
                        } else {
                            PartyCommands.invite(mc, name);
                            showFeedback(Component.translatable("screen.opacui.feedback.invited", name));
                        }
                        inviteBox.setValue("");
                        scheduleActionRefresh();
                    }
                }).bounds(width / 2 + 50, barY, 60, 20).build());
        btnSendInvite.active = canModeratorPlus;

        // ── Leave / Disband ───────────────────────────────────────────────
        int bottomY = height - BOTTOM_Y_OFFSET;

        addRenderableWidget(Button.builder(
                localIsOwner
                        ? Component.translatable("screen.opacui.disband_party")
                        : Component.translatable("screen.opacui.leave_party"),
                b -> {
                    if (localIsOwner) {
                        // Owner must disband instead of leaving
                        minecraft.setScreen(new ConfirmActionScreen(
                                this,
                                Component.translatable("screen.opacui.confirm_title"),
                                Component.translatable("screen.opacui.confirm_disband"),
                                () -> {
                                    PartyCommands.destroyParty(mc);
                                    minecraft.setScreen(parent);
                                }));
                    } else {
                        PartyCommands.leaveParty(mc);
                        minecraft.setScreen(parent);
                    }
                }).bounds(width / 2 - 155, bottomY, 100, 18).build());

        // Close button
        addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                b -> minecraft.setScreen(parent)).bounds(width / 2 + 55, bottomY, 100, 18).build());

        setFocused(inviteBox);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Render
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        renderBackground(g);

        if (clothScreen != null) {
            // Cloth Config renders the tabbed list (and its own header/title)
            clothScreen.render(g, mouseX, mouseY, delta);
        } else {
            // State A: draw title and no-party message
            g.drawCenteredString(font, title, width / 2, 16, 0xFFFFFFFF);
            g.drawCenteredString(font,
                    Component.translatable("screen.opacui.no_party"),
                    width / 2, height / 2 - 20, 0xFFAAAAAA);
        }

        // Feedback message (auto-fades)
        if (feedbackTimer > 0 && feedbackText != null) {
            g.drawCenteredString(font, feedbackText, width / 2, 28, 0xFFFFAA00);
        }

        // Our own widgets (invite bar, leave/close buttons) on top
        super.render(g, mouseX, mouseY, delta);

        // Suggestion dropdown
        if (showSuggestions && !suggestions.isEmpty()) {
            int boxX = width / 2 - 150;
            int boxY = height - BAR_Y_OFFSET;
            int itemH = font.lineHeight + 4;
            int visible = Math.min(suggestions.size(), 5);
            int ddH = visible * itemH + 4;
            int ddY = boxY - ddH - 2;

            g.fill(boxX, ddY, boxX + 198, ddY + ddH, 0xCC000000);
            for (int i = 0; i < visible; i++) {
                int itemY = ddY + 2 + i * itemH;
                if (i == selectedSuggestion) {
                    g.fill(boxX, itemY, boxX + 198, itemY + itemH, 0x55555555);
                }
                String sName = suggestions.get(i);
                ResourceLocation skin = AvatarCache.getSkin(minecraft, null, sName);
                PlayerFaceRenderer.draw(g, skin, boxX + 4, itemY + 1, 8);
                g.drawString(font, sName, boxX + 16, itemY + 1, 0xFFFFFFFF, false);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Feedback & Tick
    // ─────────────────────────────────────────────────────────────────────

    private void showFeedback(Component text) {
        feedbackText = text;
        feedbackTimer = 60;
    }

    private void scheduleActionRefresh() {
        actionRefreshTicks = 6;
        reinit();
    }

    private void updateActionBarForTab() {
        if (btnSendInvite == null || inviteBox == null)
            return;
        if (savedTabIndex == 2) {
            btnSendInvite.setMessage(Component.literal("Add Ally"));
            inviteBox.setHint(Component.literal("Enter player name to ally..."));
        } else {
            btnSendInvite.setMessage(Component.translatable("screen.opacui.invite"));
            inviteBox.setHint(Component.translatable("screen.opacui.invite_placeholder"));
        }
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
                updateActionBarForTab();
            }
        }

        IClientPartyAPI party = getParty();
        boolean isPresent = party != null;
        int memberCount = party != null ? party.getMemberCount() : 0;
        int inviteCount = party != null ? party.getInviteCount() : 0;
        int allyCount = party != null ? party.getAllyCount() : 0;

        if (isPresent != lastPartyPresent || memberCount != lastMemberCount || inviteCount != lastInviteCount
                || allyCount != lastAllyCount) {
            lastPartyPresent = isPresent;
            lastMemberCount = memberCount;
            lastInviteCount = inviteCount;
            lastAllyCount = allyCount;
            reinit();
            return;
        }

        if (actionRefreshTicks > 0) {
            actionRefreshTicks--;
            if (actionRefreshTicks == 0) {
                reinit();
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Input delegation to cloth sub-screen
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double x, double y, int btn) {
        // Handle suggestion dropdown clicks
        if (showSuggestions && !suggestions.isEmpty()) {
            int boxX = width / 2 - 150;
            int boxY = height - BAR_Y_OFFSET;
            int itemH = font.lineHeight + 4;
            int visible = Math.min(suggestions.size(), 5);
            int ddH = visible * itemH + 4;
            int ddY = boxY - ddH - 2;

            if (x >= boxX && x < boxX + 198 && y >= ddY && y < ddY + ddH) {
                int idx = (int) ((y - ddY - 2) / itemH);
                if (idx >= 0 && idx < suggestions.size()) {
                    selectSuggestion(idx);
                }
                return true;
            }
            showSuggestions = false;
        }

        if (clothScreen != null && y < height - CLOTH_BOTTOM_MARGIN) {
            return clothScreen.mouseClicked(x, y, btn) || super.mouseClicked(x, y, btn);
        }
        return super.mouseClicked(x, y, btn);
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
        if (showSuggestions && !suggestions.isEmpty()) {
            if (keyCode == GLFW.GLFW_KEY_UP) {
                selectedSuggestion = Math.max(0, selectedSuggestion - 1);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_DOWN) {
                selectedSuggestion = Math.min(suggestions.size() - 1, selectedSuggestion + 1);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER && selectedSuggestion >= 0) {
                selectSuggestion(selectedSuggestion);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                showSuggestions = false;
                return true;
            }
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER) {
            if (partyNameBox != null && partyNameBox.isFocused()) {
                String newName = partyNameBox.getValue().trim();
                if (!newName.isEmpty()) {
                    PartyCommands.renameParty(minecraft, newName);
                    showFeedback(Component.literal("Renamed party to " + newName));
                    scheduleActionRefresh();
                    return true;
                }
            }
            if (inviteBox != null && inviteBox.isFocused()) {
                String name = inviteBox.getValue().trim();
                if (!name.isEmpty() && btnSendInvite != null && btnSendInvite.active) {
                    btnSendInvite.onPress();
                    return true;
                }
            }
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (clothScreen != null) {
            return clothScreen.keyPressed(keyCode, scanCode, modifiers)
                    || super.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (clothScreen != null) {
            return clothScreen.charTyped(chr, modifiers) || super.charTyped(chr, modifiers);
        }
        return super.charTyped(chr, modifiers);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────

    /** Re-initialises the screen so the party list refreshes after an action. */
    private void reinit() {
        init(minecraft, width, height);
    }

    private void updateSuggestions(String text) {
        if (text.isEmpty() || minecraft == null || minecraft.getConnection() == null) {
            suggestions = List.of();
            showSuggestions = false;
            return;
        }
        String lower = text.toLowerCase();
        IClientPartyAPI party = getParty();
        Set<String> memberNames = party != null
                ? party.getMemberInfoStream().map(m -> m.getUsername().toLowerCase()).collect(Collectors.toSet())
                : Set.of();

        suggestions = minecraft.getConnection().getOnlinePlayers().stream()
                .map(pi -> pi.getProfile().getName())
                .filter(name -> !memberNames.contains(name.toLowerCase()))
                .filter(name -> name.toLowerCase().startsWith(lower))
                .sorted()
                .limit(5)
                .toList();
        selectedSuggestion = -1;
        showSuggestions = !suggestions.isEmpty();
    }

    private void selectSuggestion(int index) {
        inviteBox.setValue(suggestions.get(index));
        inviteBox.moveCursorToEnd();
        showSuggestions = false;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    /** Safely fetches the local client's party; returns null if not in one. */
    private static IClientPartyAPI getParty() {
        try {
            OpenPACClientAPI api = OpenPACClientAPI.get();
            if (api == null)
                return null;
            IClientPartyStorageAPI storage = api.getClientPartyStorage();
            if (storage == null || storage.isLoading())
                return null;
            return storage.getParty();
        } catch (Exception e) {
            LOGGER.warn("Failed to get party", e);
            return null;
        }
    }

    /** Returns true only if the server has the mod and parties are enabled. */
    public static boolean isAvailable(Minecraft mc) {
        if (mc.level == null || mc.player == null)
            return false;
        try {
            var cap = OpenPACClientAPI.get()
                    .getCapabilityHelper()
                    .getCapability(mc.level, ClientWorldCapabilityTypes.MAIN_CAP);
            if (cap == null)
                return false;
            var worldData = cap.getClientWorldData();
            return worldData.serverHasMod() && worldData.serverHasPartiesEnabled();
        } catch (Exception e) {
            LOGGER.warn("Failed to check party availability", e);
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Placeholder entry for empty tab states
    // ─────────────────────────────────────────────────────────────────────

    private static class PlaceholderEntry extends TooltipListEntry<Void> {
        private static final int ENTRY_HEIGHT = 20;
        private final Component text;

        @SuppressWarnings("deprecation")
        public PlaceholderEntry(Component text) {
            super(Component.empty(), null);
            this.text = text;
        }

        @Override
        public void render(GuiGraphics g, int index, int y, int x, int entryWidth, int entryHeight, int mouseX,
                int mouseY, boolean isHovered, float delta) {
            g.drawCenteredString(Minecraft.getInstance().font, text, x + entryWidth / 2, y + (entryHeight - 8) / 2,
                    0xFF888888);
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
            return List.of();
        }

        @Override
        public List<? extends net.minecraft.client.gui.narration.NarratableEntry> narratables() {
            return List.of();
        }

        @Override
        public Optional<Component[]> getTooltip() {
            return Optional.empty();
        }
    }
}
