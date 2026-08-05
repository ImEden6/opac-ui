package mervyn.opacui.client.gui.entry;

import mervyn.opacui.client.gui.list.AbstractPartyEntry;
import mervyn.opacui.client.util.PartyCommands;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import xaero.pac.client.parties.party.api.IClientPartyAllyInfoAPI;

import java.util.List;
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
public class AllyEntry extends AbstractPartyEntry {

    private static final int BTN_W = 56;
    private static final int BTN_H = 18;

    private final IClientPartyAllyInfoAPI ally;
    private final boolean canModify;
    private final Button btnUnally;
    private final String displayName;

    public AllyEntry(IClientPartyAllyInfoAPI ally, boolean canModify, Consumer<Component> onAction) {
        this.ally = ally;
        this.canModify = canModify;
        this.displayName = (ally.getAllyName() != null && !ally.getAllyName().isEmpty()) ? ally.getAllyName() : ally.getAllyDefaultName();

        Minecraft mc = Minecraft.getInstance();
        btnUnally = Button.builder(Component.translatable("screen.opacui.unally"), b -> {
            String ownerName = resolveOwnerName();
            PartyCommands.removeAlly(mc, ownerName);
            onAction.accept(Component.translatable("screen.opacui.feedback.unallied", this.displayName));
        }).size(BTN_W, BTN_H).build();
        btnUnally.active = canModify;
    }

    private String resolveOwnerName() {
        String defaultName = ally.getAllyDefaultName() != null ? ally.getAllyDefaultName() : "";
        return parseOwnerName(defaultName);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && Minecraft.getInstance().screen instanceof mervyn.opacui.client.gui.PartyScreen ps) {
            ps.populateInputBox(resolveOwnerName());
        }
        return super.mouseClicked(event, doubleClick);
    }

    public static String parseOwnerName(String defaultName) {
        if (defaultName == null || defaultName.isEmpty()) {
            return "";
        }
        int idx = defaultName.lastIndexOf("'s Party");
        if (idx > 0) {
            return defaultName.substring(0, idx);
        }
        int apostropheIdx = defaultName.indexOf('\'');
        if (apostropheIdx > 0) {
            return defaultName.substring(0, apostropheIdx);
        }
        return defaultName;
    }

    @Override
    public void extractContent(GuiGraphicsExtractor g, int mouseX, int mouseY, boolean isHovered, float delta) {
        Minecraft mc = Minecraft.getInstance();
        int x = getContentX();
        int y = getContentY();
        int entryWidth = getContentWidth();
        int entryHeight = getContentHeight();

        g.text(mc.font, "Allied: " + displayName, x + 4, y + (entryHeight - 8) / 2, 0xFF88FFAA);

        if (canModify) {
            int btnY = y + (entryHeight - BTN_H) / 2;
            btnUnally.setX(x + entryWidth - BTN_W - 2);
            btnUnally.setY(btnY);
            btnUnally.extractRenderState(g, mouseX, mouseY, delta);
        }
    }

    @Override
    public List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children() {
        return canModify ? List.of(btnUnally) : List.of();
    }

    @Override
    public List<? extends net.minecraft.client.gui.narration.NarratableEntry> narratables() {
        return canModify ? List.of(btnUnally) : List.of();
    }
}
