package mervyn.opacui.client.gui.entry;

import mervyn.opacui.client.util.AvatarCache;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.resources.ResourceLocation;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import mervyn.opacui.client.gui.ConfirmActionScreen;
import mervyn.opacui.client.util.PartyCommands;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import xaero.pac.common.parties.party.member.PartyMemberRank;
import xaero.pac.common.parties.party.member.api.IPartyMemberAPI;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * A single scrollable-list row representing one party member.
 * Renders: [★?] username (rank label) [▼Rank] [▲Rank] [Kick] [Transfer?]
 *
 * Buttons are only shown if the local player has sufficient rank.
 */
public class MemberEntry extends TooltipListEntry<Void> {

    private static final int ENTRY_HEIGHT = 24;
    private static final int BTN_W = EntryButton.WIDTH;
    private static final int BTN_H = EntryButton.HEIGHT;
    private static final int BTN_GAP = EntryButton.GAP;

    private final IPartyMemberAPI member;
    private final boolean isOnline;

    // Action buttons — initialised in the constructor
    private final Button btnRankDown;
    private final Button btnRankUp;
    private final Button btnKick;
    private final Button btnTransfer;

    @SuppressWarnings("deprecation")
    public MemberEntry(
            IPartyMemberAPI member,
            PartyMemberRank localRank,
            boolean localIsOwner,
            boolean isSelf,
            boolean isOnline,
            Consumer<Component> onAction) {
        super(Component.literal(member.getUsername()), null);
        this.member = member;
        this.isOnline = isOnline;

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

        // Visibility rules:
        // rank buttons: ADMIN+ only, not on self, not on owner
        boolean canRank = !isSelf && !member.isOwner() && localRank.ordinal() >= PartyMemberRank.ADMIN.ordinal();
        btnRankDown.active = canRank && member.getRank().ordinal() > PartyMemberRank.MEMBER.ordinal();
        btnRankUp.active = canRank && member.getRank().ordinal() < PartyMemberRank.ADMIN.ordinal();
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

    @Override
    public void render(GuiGraphics g, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY,
            boolean isHovered, float delta) {
        Minecraft mc = Minecraft.getInstance();

        // ── Player Face Avatar ────────────────────────────────────────────
        ResourceLocation skin = AvatarCache.getSkin(mc, member.getUUID(), member.getUsername());
        int headSize = 12;
        int headX = x + 4;
        int headY = y + (entryHeight - headSize) / 2;
        PlayerFaceRenderer.draw(g, skin, headX, headY, headSize);

        // ── Text label ────────────────────────────────────────────────────
        MutableComponent dot = Component.literal(isOnline ? "●" : "○")
                .withStyle(isOnline ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY);
        String star = member.isOwner() ? " ★" : "";
        ChatFormatting rankColor = member.getRank().getColor();
        MutableComponent label = Component.literal("")
                .append(dot)
                .append(Component.literal(star + " " + member.getUsername()).withStyle(s -> s.withColor(rankColor)));
        MutableComponent rankLabel = Component.literal(" [" + member.getRank().name() + "]")
                .withStyle(ChatFormatting.DARK_GRAY);
        g.drawString(mc.font, label.append(rankLabel), x + 22, y + (entryHeight - 8) / 2, 0xFFFFFFFF, false);

        // ── Position & render buttons from right edge ──────────────────────
        int rightEdge = x + entryWidth - 2;
        int btnY = y + (entryHeight - BTN_H) / 2;

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
        return List.of(btnRankDown, btnRankUp, btnKick, btnTransfer);
    }

    @Override
    public List<? extends net.minecraft.client.gui.narration.NarratableEntry> narratables() {
        return List.of(btnRankDown, btnRankUp, btnKick, btnTransfer);
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
