package mervyn.opacui.client.gui.entry;

import mervyn.opacui.client.gui.list.AbstractPartyEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import xaero.pac.common.parties.party.member.api.IPartyMemberAPI;

import java.util.List;

public class ClaimEntry extends AbstractPartyEntry {

    private final IPartyMemberAPI member;
    private final boolean isOnline;
    private final int claims;
    private final int forceloads;
    private final int claimLimit;
    private final int forceloadLimit;
    private final List<Component> dimensions;

    public ClaimEntry(IPartyMemberAPI member, boolean isOnline, int claims, int forceloads, int claimLimit, int forceloadLimit, List<Component> dimensions) {
        this.member = member;
        this.isOnline = isOnline;
        this.claims = claims;
        this.forceloads = forceloads;
        this.claimLimit = claimLimit;
        this.forceloadLimit = forceloadLimit;
        this.dimensions = dimensions;
    }

    @Override
    public void extractContent(GuiGraphicsExtractor g, int mouseX, int mouseY, boolean isHovered, float delta) {
        Minecraft mc = Minecraft.getInstance();
        int x = getContentX();
        int y = getContentY();
        int entryHeight = getContentHeight();

        MemberEntry.drawMember(g, member, isOnline, Component.empty(), x, y, entryHeight);

        Component counts = Component.translatable("screen.opacui.claims.counts", count(claims, claimLimit), count(forceloads, forceloadLimit));
        g.text(mc.font, counts, x + getContentWidth() - 4 - mc.font.width(counts), y + (entryHeight - 8) / 2, 0xFFAAAAAA);

        if (isHovered && !dimensions.isEmpty()) {
            g.setComponentTooltipForNextFrame(mc.font, dimensions, mouseX, mouseY);
        }
    }

    private static String count(int value, int limit) {
        if (value < 0) return "?";
        return limit < 0 ? String.valueOf(value) : value + "/" + limit;
    }

    @Override
    public List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children() { return List.of(); }

    @Override
    public List<? extends net.minecraft.client.gui.narration.NarratableEntry> narratables() { return List.of(); }
}
