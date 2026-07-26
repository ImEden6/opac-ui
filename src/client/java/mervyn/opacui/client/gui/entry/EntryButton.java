package mervyn.opacui.client.gui.entry;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * Factory and helper methods for standardized entry row buttons (56x18 px default).
 */
public final class EntryButton {

    public static final int WIDTH = 56;
    public static final int HEIGHT = 18;
    public static final int GAP = 4;

    private EntryButton() {}

    /** Constructs a standard 56x18 entry row button. */
    public static Button create(Component label, Button.OnPress onPress) {
        return create(label, WIDTH, HEIGHT, onPress);
    }

    /** Constructs an entry row button with custom dimensions. */
    public static Button create(Component label, int width, int height, Button.OnPress onPress) {
        return Button.builder(label, onPress)
                .size(width, height)
                .build();
    }
}
