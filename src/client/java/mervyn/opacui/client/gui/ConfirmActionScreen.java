package mervyn.opacui.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * A minimal yes/no confirmation dialog placed over the current screen.
 * Confirm runs the given action; Cancel returns to parent without doing anything.
 */
public class ConfirmActionScreen extends Screen {

    private final Screen parent;
    private final Component message;
    private final Runnable onConfirm;

    public ConfirmActionScreen(Screen parent, Component title, Component message, Runnable onConfirm) {
        super(title);
        this.parent = parent;
        this.message = message;
        this.onConfirm = onConfirm;
    }

    @Override
    protected void init() {
        super.init();
        int cx = width / 2;
        int cy = height / 2;

        addRenderableWidget(Button.builder(
                Component.translatable("screen.opacui.confirm"),
                b -> {
                    onConfirm.run();
                }
        ).bounds(cx - 105, cy + 14, 100, 20).build());

        addRenderableWidget(Button.builder(
                Component.translatable("screen.opacui.cancel"),
                b -> minecraft.setScreen(parent)
        ).bounds(cx + 5, cy + 14, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        // Dim the background
        renderBackground(g);

        // Dialog box
        int boxW = 240, boxH = 86;
        int x = (width - boxW) / 2;
        int y = (height - boxH) / 2;
        g.fill(x - 2, y - 2, x + boxW + 2, y + boxH + 2, 0xFF222222);
        g.fill(x, y, x + boxW, y + boxH, 0xFF333333);

        // Title
        g.drawCenteredString(font, title, width / 2, y + 8, 0xFFFFAA00);
        // Message (word-wrap at 220 px)
        int msgColor = 0xFFDDDDDD;
        int lineY = y + 24;
        for (var line : font.split(message, 220)) {
            g.drawString(font, line, x + 10, lineY, msgColor, false);
            lineY += font.lineHeight + 2;
        }
        super.render(g, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldCloseOnEsc() { return true; }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
