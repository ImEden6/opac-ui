package mervyn.opacui.client.gui.list;

import net.minecraft.client.gui.components.ContainerObjectSelectionList;

/**
 * Common row base for {@link PartyEntryList}. Replaces Cloth Config's
 * {@code TooltipListEntry<Void>} — subclasses implement
 * {@code extractContent}, {@code children}, and {@code narratables}
 * directly.
 */
public abstract class AbstractPartyEntry extends ContainerObjectSelectionList.Entry<AbstractPartyEntry> {
}
