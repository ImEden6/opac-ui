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

    private static final int SCREEN_MARGIN = 16;

    private final Screen parent;
    private final Component message;
    private final Runnable onConfirm;

    public ConfirmActionScreen(Screen parent, Component title, Component message, Runnable onConfirm) {
        super(title);
        this.parent = parent;
        this.message = message;
        this.onConfirm = onConfirm;
    }

    private Button confirmButton;
    private Button cancelButton;

    @Override
    protected void init() {
        super.init();
        confirmButton = addRenderableWidget(Button.builder(
                Component.translatable("screen.opacui.confirm"),
                b -> onConfirm.run()
        ).bounds(0, 0, 100, 20).build());

        cancelButton = addRenderableWidget(Button.builder(
                Component.translatable("screen.opacui.cancel"),
                b -> minecraft.setScreen(parent)
        ).bounds(0, 0, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        renderBackground(g, mouseX, mouseY, delta);

        var lines = font.split(message, 220);
        int lineCount = Math.max(1, lines.size());
        int msgHeight = lineCount * (font.lineHeight + 2);
        int boxW = 240;
        int boxH = 24 + msgHeight + 36;

        int x = (width - boxW) / 2;
        int y = (height - boxH) / 2;
        x = Math.max(SCREEN_MARGIN, Math.min(x, width - boxW - SCREEN_MARGIN));
        y = Math.max(SCREEN_MARGIN, Math.min(y, height - boxH - SCREEN_MARGIN));
        int boxCenterX = x + boxW / 2;

        g.fill(x - 2, y - 2, x + boxW + 2, y + boxH + 2, 0xFF222222);
        g.fill(x, y, x + boxW, y + boxH, 0xFF333333);

        g.drawCenteredString(font, title, boxCenterX, y + 8, 0xFFFFAA00);

        int msgColor = 0xFFDDDDDD;
        int lineY = y + 24;
        for (var line : lines) {
            g.drawString(font, line, x + 10, lineY, msgColor, false);
            lineY += font.lineHeight + 2;
        }

        int btnY = lineY + 8;
        int cx = boxCenterX;
        if (confirmButton != null) {
            confirmButton.setX(cx - 105);
            confirmButton.setY(btnY);
        }
        if (cancelButton != null) {
            cancelButton.setX(cx + 5);
            cancelButton.setY(btnY);
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
