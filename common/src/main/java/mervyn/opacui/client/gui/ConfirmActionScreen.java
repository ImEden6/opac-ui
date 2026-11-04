package mervyn.opacui.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

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

    private static final int BOX_W = 240;

    private List<FormattedCharSequence> lines;
    private int boxX;
    private int boxY;
    private int boxH;

    @Override
    protected void init() {
        super.init();
        lines = font.split(message, 220);
        int msgHeight = Math.max(1, lines.size()) * (font.lineHeight + 2);
        boxH = 24 + msgHeight + 36;
        boxX = Math.max(SCREEN_MARGIN, Math.min((width - BOX_W) / 2, width - BOX_W - SCREEN_MARGIN));
        boxY = Math.max(SCREEN_MARGIN, Math.min((height - boxH) / 2, height - boxH - SCREEN_MARGIN));
        int cx = boxX + BOX_W / 2;
        int btnY = boxY + 24 + lines.size() * (font.lineHeight + 2) + 8;

        addRenderableWidget(Button.builder(
                Component.translatable("screen.opacui.confirm"),
                b -> onConfirm.run()
        ).bounds(cx - 105, btnY, 100, 20).build());

        addRenderableWidget(Button.builder(
                Component.translatable("screen.opacui.cancel"),
                b -> minecraft.setScreen(parent)
        ).bounds(cx + 5, btnY, 100, 20).build());
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.renderBackground(g, mouseX, mouseY, partial);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        g.fill(boxX - 2, boxY - 2, boxX + BOX_W + 2, boxY + boxH + 2, 0xFF222222);
        g.fill(boxX, boxY, boxX + BOX_W, boxY + boxH, 0xFF333333);

        g.drawCenteredString(font, title, boxX + BOX_W / 2, boxY + 8, 0xFFFFAA00);

        int lineY = boxY + 24;
        for (var line : lines) {
            g.drawString(font, line, boxX + 10, lineY, 0xFFDDDDDD);
            lineY += font.lineHeight + 2;
        }

        super.render(g, mouseX, mouseY, partial);
    }

    @Override
    public boolean shouldCloseOnEsc() { return true; }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
