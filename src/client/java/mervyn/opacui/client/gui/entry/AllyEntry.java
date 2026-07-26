package mervyn.opacui.client.gui.entry;

import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import mervyn.opacui.client.util.PartyCommands;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import xaero.pac.client.parties.party.api.IClientPartyAllyInfoAPI;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * A single row in the Allies tab showing an allied party name and an [Unally] button.
 *
 * The unally command requires the **allied party's owner username**, which OPAC
 * exposes via {@link IClientPartyAllyInfoAPI#getAllyName()} (this is the custom or
 * default name string that the server uses to look up the ally).
 * The actual command argument is the owner's username, which is the default name
 * of the allied party ({@link IClientPartyAllyInfoAPI#getAllyDefaultName()}).
 */
public class AllyEntry extends TooltipListEntry<Void> {

    private static final int ENTRY_HEIGHT = 24;
    private static final int BTN_W = 56;
    private static final int BTN_H = 18;

    private final IClientPartyAllyInfoAPI ally;
    private final boolean canModify;
    private final Button btnUnally;

    @SuppressWarnings("deprecation")
    public AllyEntry(IClientPartyAllyInfoAPI ally, boolean canModify, Consumer<Component> onAction) {
        super(Component.empty(), null);
        this.ally = ally;
        this.canModify = canModify;

        Minecraft mc = Minecraft.getInstance();
        btnUnally = Button.builder(Component.translatable("screen.opacui.unally"), b -> {
            String defaultName = ally.getAllyDefaultName() != null ? ally.getAllyDefaultName() : "";
            String ownerName = defaultName.endsWith("'s Party")
                    ? defaultName.substring(0, defaultName.length() - 8)
                    : defaultName;
            PartyCommands.removeAlly(mc, ownerName);
            String displayName = (ally.getAllyName() != null && !ally.getAllyName().isEmpty()) ? ally.getAllyName() : defaultName;
            onAction.accept(Component.translatable("screen.opacui.feedback.unallied", displayName));
        }).size(BTN_W, BTN_H).build();
        btnUnally.active = canModify;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && Minecraft.getInstance().screen instanceof mervyn.opacui.client.gui.PartyScreen ps) {
            String defaultName = ally.getAllyDefaultName() != null ? ally.getAllyDefaultName() : "";
            String ownerName = defaultName.endsWith("'s Party")
                    ? defaultName.substring(0, defaultName.length() - 8)
                    : defaultName;
            ps.populateInputBox(ownerName);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics g, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
        Minecraft mc = Minecraft.getInstance();
        String display = ally.getAllyName().isEmpty() ? ally.getAllyDefaultName() : ally.getAllyName();
        g.drawString(mc.font, "Allied: " + display, x + 4, y + (entryHeight - 8) / 2, 0xFF88FFAA, false);

        if (canModify) {
            int btnY = y + (entryHeight - BTN_H) / 2;
            btnUnally.setX(x + entryWidth - BTN_W - 2);
            btnUnally.setY(btnY);
            btnUnally.render(g, mouseX, mouseY, delta);
        }
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
    public List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children() {
        return canModify ? List.of(btnUnally) : List.of();
    }

    @Override
    public List<? extends net.minecraft.client.gui.narration.NarratableEntry> narratables() {
        return canModify ? List.of(btnUnally) : List.of();
    }

    @Override
    public Optional<Component[]> getTooltip() { return Optional.empty(); }
}
