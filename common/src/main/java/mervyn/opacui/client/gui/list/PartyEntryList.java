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
}
