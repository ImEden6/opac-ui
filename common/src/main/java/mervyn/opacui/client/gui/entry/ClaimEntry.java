package mervyn.opacui.client.gui.entry;

import mervyn.opacui.client.gui.list.AbstractPartyEntry;
import mervyn.opacui.client.util.WorldMapLink;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import xaero.pac.common.parties.party.member.api.IPartyMemberAPI;

import java.util.List;

public class ClaimEntry extends AbstractPartyEntry {

    private static final Identifier MAP_ICON = Identifier.fromNamespaceAndPath("opacui", "textures/gui/map.png");
    private static final int MAP_BUTTON_SIZE = 20;

    private final IPartyMemberAPI member;
    private final boolean isOnline;
    private final int claims;
    private final int forceloads;
    private final int claimLimit;
    private final int forceloadLimit;
    private final List<Component> dimensions;
    private final Button btnMap;

    public ClaimEntry(IPartyMemberAPI member, boolean isOnline, int claims, int forceloads, int claimLimit, int forceloadLimit,
                      List<Component> dimensions, ChunkPos mapTarget, String mapElsewhere) {
        this.member = member;
        this.isOnline = isOnline;
        this.claims = claims;
        this.forceloads = forceloads;
        this.claimLimit = claimLimit;
        this.forceloadLimit = forceloadLimit;
        this.dimensions = dimensions;

        btnMap = Button.builder(Component.empty(), b -> WorldMapLink.open(Minecraft.getInstance(), mapTarget))
                .size(MAP_BUTTON_SIZE, MAP_BUTTON_SIZE)
                .build();
        btnMap.visible = mapTarget != null || mapElsewhere != null;
        btnMap.active = mapTarget != null;
        btnMap.setTooltip(Tooltip.create(mapTarget != null
                ? Component.translatable("screen.opacui.claims.map")
                : Component.translatable("screen.opacui.claims.map_elsewhere", mapElsewhere == null ? "" : mapElsewhere)));
    }

    @Override
    public void extractContent(GuiGraphicsExtractor g, int mouseX, int mouseY, boolean isHovered, float delta) {
        Minecraft mc = Minecraft.getInstance();
        int x = getContentX();
        int y = getContentY();
        int entryHeight = getContentHeight();
        int right = x + getContentWidth() - 2;

        MemberEntry.drawMember(g, member, isOnline, Component.empty(), x, y, entryHeight);

        if (btnMap.visible) {
            btnMap.setX(right - MAP_BUTTON_SIZE);
            btnMap.setY(y + (entryHeight - MAP_BUTTON_SIZE) / 2);
            btnMap.extractRenderState(g, mouseX, mouseY, delta);
            g.blit(RenderPipelines.GUI_TEXTURED, MAP_ICON, btnMap.getX() + 2, btnMap.getY() + 2, 0, 0, 16, 16, 16, 16,
                    btnMap.active ? 0xFFFFFFFF : 0xFF707070);
            right -= MAP_BUTTON_SIZE + EntryButton.GAP;
        } else {
            right -= 2;
        }

        Component counts = Component.translatable("screen.opacui.claims.counts", count(claims, claimLimit), count(forceloads, forceloadLimit));
        g.text(mc.font, counts, right - mc.font.width(counts), y + (entryHeight - 8) / 2, 0xFFAAAAAA);

        if (isHovered && !dimensions.isEmpty() && !btnMap.isHovered()) {
            g.setComponentTooltipForNextFrame(mc.font, dimensions, mouseX, mouseY);
        }
    }

    private static String count(int value, int limit) {
        if (value < 0) return "?";
        return limit < 0 ? String.valueOf(value) : value + "/" + limit;
    }

    @Override
    public List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children() {
        return btnMap.visible ? List.of(btnMap) : List.of();
    }

    @Override
    public List<? extends net.minecraft.client.gui.narration.NarratableEntry> narratables() {
        return btnMap.visible ? List.of(btnMap) : List.of();
    }
}
