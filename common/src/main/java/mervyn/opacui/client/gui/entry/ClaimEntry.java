package mervyn.opacui.client.gui.entry;

import me.shedaniel.clothconfig2.api.Tooltip;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import me.shedaniel.math.Point;
import mervyn.opacui.client.util.WorldMapLink;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import xaero.pac.common.parties.party.member.api.IPartyMemberAPI;

import java.util.List;
import java.util.Optional;

public class ClaimEntry extends TooltipListEntry<Void> {

    private static final int ENTRY_HEIGHT = 24;
    private static final ResourceLocation MAP_ICON = ResourceLocation.fromNamespaceAndPath("opacui", "textures/gui/map.png");
    private static final int MAP_BUTTON_SIZE = 20;

    private final IPartyMemberAPI member;
    private final boolean isOnline;
    private final int claims;
    private final int forceloads;
    private final int claimLimit;
    private final int forceloadLimit;
    private final List<Component> dimensions;
    private final Button btnMap;

    @SuppressWarnings("deprecation")
    public ClaimEntry(IPartyMemberAPI member, boolean isOnline, int claims, int forceloads, int claimLimit, int forceloadLimit,
                      List<Component> dimensions, ChunkPos mapTarget, String mapElsewhere) {
        super(Component.literal(member.getUsername()), null);
        this.member = member;
        this.isOnline = isOnline;
        this.claims = claims;
        this.forceloads = forceloads;
        this.claimLimit = claimLimit;
        this.forceloadLimit = forceloadLimit;
        this.dimensions = dimensions;

        Component mapLabel = mapTarget != null ? Component.translatable("screen.opacui.claims.map")
                : mapElsewhere != null ? Component.translatable("screen.opacui.claims.map_elsewhere", mapElsewhere)
                : null;
        btnMap = Button.builder(Component.empty(), b -> WorldMapLink.open(Minecraft.getInstance(), mapTarget))
                .size(MAP_BUTTON_SIZE, MAP_BUTTON_SIZE)
                .createNarration(message -> Component.translatable("gui.narrate.button", Component.translatable("screen.opacui.claims.map_button")))
                .build();
        btnMap.visible = mapLabel != null;
        btnMap.active = mapTarget != null;
        if (mapLabel != null) {
            btnMap.setTooltip(net.minecraft.client.gui.components.Tooltip.create(mapLabel));
        }
    }

    @Override
    public void render(GuiGraphics g, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY,
            boolean isHovered, float delta) {
        Minecraft mc = Minecraft.getInstance();
        int right = x + entryWidth - 2;

        MemberEntry.drawMember(g, member, isOnline, Component.empty(), x, y, entryHeight);

        if (btnMap.visible) {
            btnMap.setX(right - MAP_BUTTON_SIZE);
            btnMap.setY(y + (entryHeight - MAP_BUTTON_SIZE) / 2);
            btnMap.render(g, mouseX, mouseY, delta);
            float shade = btnMap.active ? 1 : 0.44f;
            g.setColor(shade, shade, shade, 1);
            g.blit(MAP_ICON, btnMap.getX() + 2, btnMap.getY() + 2, 0, 0, 16, 16, 16, 16);
            g.setColor(1, 1, 1, 1);
            right -= MAP_BUTTON_SIZE + EntryButton.GAP;
        } else {
            right -= 2;
        }

        Component counts = Component.translatable("screen.opacui.claims.counts", count(claims, claimLimit), count(forceloads, forceloadLimit));
        g.drawString(mc.font, counts, right - mc.font.width(counts), y + (entryHeight - 8) / 2, 0xFFAAAAAA, false);

        if (isHovered && !dimensions.isEmpty() && !btnMap.isHovered()) {
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
        return btnMap.visible ? List.of(btnMap) : List.of();
    }

    @Override
    public List<? extends net.minecraft.client.gui.narration.NarratableEntry> narratables() {
        return btnMap.visible ? List.of(btnMap) : List.of();
    }

    @Override
    public Optional<Component[]> getTooltip() {
        return Optional.empty();
    }
}
