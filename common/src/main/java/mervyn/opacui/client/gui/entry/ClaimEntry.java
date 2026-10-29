package mervyn.opacui.client.gui.entry;

import me.shedaniel.clothconfig2.api.Tooltip;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import me.shedaniel.math.Point;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import xaero.pac.common.parties.party.member.api.IPartyMemberAPI;

import java.util.List;
import java.util.Optional;

public class ClaimEntry extends TooltipListEntry<Void> {

    private static final int ENTRY_HEIGHT = 24;

    private final IPartyMemberAPI member;
    private final boolean isOnline;
    private final int claims;
    private final int forceloads;
    private final int claimLimit;
    private final int forceloadLimit;
    private final List<Component> dimensions;

    @SuppressWarnings("deprecation")
    public ClaimEntry(IPartyMemberAPI member, boolean isOnline, int claims, int forceloads, int claimLimit, int forceloadLimit,
                      List<Component> dimensions) {
        super(Component.literal(member.getUsername()), null);
        this.member = member;
        this.isOnline = isOnline;
        this.claims = claims;
        this.forceloads = forceloads;
        this.claimLimit = claimLimit;
        this.forceloadLimit = forceloadLimit;
        this.dimensions = dimensions;
    }

    @Override
    public void render(GuiGraphics g, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY,
            boolean isHovered, float delta) {
        Minecraft mc = Minecraft.getInstance();
        int right = x + entryWidth - 4;

        MemberEntry.drawMember(g, member, isOnline, Component.empty(), x, y, entryHeight);

        Component counts = Component.translatable("screen.opacui.claims.counts", count(claims, claimLimit), count(forceloads, forceloadLimit));
        g.drawString(mc.font, counts, right - mc.font.width(counts), y + (entryHeight - 8) / 2, 0xFFAAAAAA, false);

        if (isHovered && !dimensions.isEmpty()) {
            addTooltip(Tooltip.of(new Point(mouseX, mouseY), dimensions.toArray(Component[]::new)));
        }
    }

    private static String count(int value, int limit) {
        if (value < 0) return "?";
        return limit < 0 ? String.valueOf(value) : value + "/" + limit;
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
