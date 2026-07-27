package mervyn.opacui.client.gui.widget;

import mervyn.opacui.client.util.PartyCommands;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import xaero.pac.client.gui.ConfigMenu;

import java.util.function.Consumer;

/**
 * Top header widget for PartyScreen: OPAC Settings gear button and Party Rename bar.
 */
public class PartyHeaderWidget {

    private EditBox partyNameBox;
    private Button btnRename;
    private Button btnConfigGear;

    public void init(Screen screen, Font font, int width, boolean localIsOwner, String currentPartyName, Consumer<Component> showFeedback, Runnable onActionComplete) {
        Minecraft mc = Minecraft.getInstance();

        // OPAC Config Gear Button in top-right corner
        btnConfigGear = Button.builder(
                Component.literal("⚙"),
                b -> mc.setScreen(new ConfigMenu(screen, screen))
        ).bounds(width - 24, 6, 18, 18)
                .tooltip(Tooltip.create(Component.translatable("screen.opacui.tooltip.config")))
                .build();

        if (localIsOwner) {
            partyNameBox = new EditBox(font, width / 2 - 100, 6, 140, 16, Component.translatable("screen.opacui.party_name_hint"));
            partyNameBox.setValue(currentPartyName != null ? currentPartyName : "");

            btnRename = Button.builder(
                    Component.translatable("screen.opacui.rename"),
                    b -> submitRename(mc, showFeedback, onActionComplete)
            ).bounds(width / 2 + 45, 6, 50, 16).build();
        } else {
            partyNameBox = null;
            btnRename = null;
        }
    }

    private void submitRename(Minecraft mc, Consumer<Component> showFeedback, Runnable onActionComplete) {
        if (partyNameBox != null) {
            String newName = partyNameBox.getValue().trim();
            if (!newName.isEmpty()) {
                PartyCommands.renameParty(mc, newName);
                showFeedback.accept(Component.translatable("screen.opacui.feedback.renamed", newName));
                onActionComplete.run();
            }
        }
    }

    public boolean isFocused() {
        return partyNameBox != null && partyNameBox.isFocused();
    }

    public EditBox getPartyNameBox() {
        return partyNameBox;
    }

    public Button getBtnRename() {
        return btnRename;
    }

    public Button getBtnConfigGear() {
        return btnConfigGear;
    }

    public boolean handleEnterKey(Minecraft mc, Consumer<Component> showFeedback, Runnable onActionComplete) {
        if (partyNameBox != null && partyNameBox.isFocused()) {
            String newName = partyNameBox.getValue().trim();
            if (!newName.isEmpty()) {
                submitRename(mc, showFeedback, onActionComplete);
                return true;
            }
        }
        return false;
    }
}
