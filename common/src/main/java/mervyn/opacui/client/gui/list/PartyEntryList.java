package mervyn.opacui.client.gui.list;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;

/**
 * One tab's scrollable row list (members / invites / allies). Replaces the
 * per-{@code ConfigCategory} list widget Cloth Config used to build.
 * Rebuilt fresh (new instance) whenever the underlying party data changes.
 */
public class PartyEntryList extends ContainerObjectSelectionList<AbstractPartyEntry> {

    public static final int ROW_HEIGHT = 24;

    public PartyEntryList(Minecraft minecraft, int width, int height, int y0) {
        super(minecraft, width, height, y0, ROW_HEIGHT);
    }

    public void addRow(AbstractPartyEntry entry) {
        addEntry(entry);
    }

    /**
     * Vanilla's {@code AbstractSelectionList.getRowWidth()} hardcodes 220px regardless of the
     * width this list was constructed with, which is nowhere near enough for an avatar, username,
     * rank label, and up to four buttons on a member row. Stretch rows to (nearly) the list's
     * actual width instead, leaving a small margin for the scrollbar.
     */
    @Override
    public int getRowWidth() {
        return Math.max(220, getWidth() - 20);
    }
}
