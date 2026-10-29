package mervyn.opacui.client.gui.entry;

import mervyn.opacui.client.gui.list.AbstractPartyEntry;
import mervyn.opacui.client.util.AvatarCache;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.PlayerSkin;
import xaero.pac.common.parties.party.member.api.IPartyMemberAPI;

import java.util.List;

/**
 * A single row on the Claims tab: one party member's claim and forceload counts.
 * Renders: ● username ... 42/100 claims · 3/10 forceloaded, with a per-dimension
 * breakdown as a hover tooltip. Limits are only known for the local player.
 */
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
        int entryWidth = getContentWidth();
        int entryHeight = getContentHeight();

        PlayerSkin skin = AvatarCache.getSkin(mc, member.getUUID(), member.getUsername());
        int headSize = 12;
        PlayerFaceExtractor.extractRenderState(g, skin, x + 4, y + (entryHeight - headSize) / 2, headSize);

        MutableComponent label = Component.literal(isOnline ? "●" : "○")
                .withStyle(isOnline ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY)
                .append(Component.literal(" " + member.getUsername()).withStyle(s -> s.withColor(member.getRank().getColor())));
        int textY = y + (entryHeight - 8) / 2;
        g.text(mc.font, label, x + 22, textY, 0xFFFFFFFF);

        Component counts = Component.translatable("screen.opacui.claims.counts", count(claims, claimLimit), count(forceloads, forceloadLimit));
        g.text(mc.font, counts, x + entryWidth - 4 - mc.font.width(counts), textY, 0xFFAAAAAA);

        if (isHovered && !dimensions.isEmpty()) {
            g.setComponentTooltipForNextFrame(mc.font, dimensions, mouseX, mouseY);
        }
    }

    private static String count(int value, int limit) {
        return limit < 0 ? String.valueOf(value) : value + "/" + limit;
    }

    @Override
    public List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children() { return List.of(); }

    @Override
    public List<? extends net.minecraft.client.gui.narration.NarratableEntry> narratables() { return List.of(); }
}
