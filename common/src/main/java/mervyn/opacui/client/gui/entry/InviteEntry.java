package mervyn.opacui.client.gui.entry;

import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import mervyn.opacui.client.gui.PartyScreen;
import mervyn.opacui.client.util.AvatarCache;
import mervyn.opacui.client.util.PartyCommands;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import xaero.pac.common.parties.party.api.IPartyPlayerInfoAPI;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * A single row in the Invites tab: shows the invited player's name and
 * a [Revoke] button that cancels the invitation (uses the kick command).
 */
public class InviteEntry extends TooltipListEntry<Void> {

    private static final int ENTRY_HEIGHT = 24;
    private static final int BTN_W = EntryButton.WIDTH;
    private static final int BTN_H = EntryButton.HEIGHT;

    private final IPartyPlayerInfoAPI invite;
    private final Button btnRevoke;

    @SuppressWarnings("deprecation")
    public InviteEntry(IPartyPlayerInfoAPI invite, Consumer<Component> onAction) {
        super(Component.empty(), null);
        this.invite = invite;

        Minecraft mc = Minecraft.getInstance();
        btnRevoke = EntryButton.create(Component.translatable("screen.opacui.revoke"), b -> {
            // Revoking an invite uses the same kick command server-side
            PartyCommands.kick(mc, invite.getUsername());
            onAction.accept(Component.translatable("screen.opacui.feedback.revoked", invite.getUsername()));
        });
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && Minecraft.getInstance().screen instanceof PartyScreen ps) {
            ps.populateInputBox(invite.getUsername());
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics g, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
        Minecraft mc = Minecraft.getInstance();

        // ── Player Face Avatar ────────────────────────────────────────────
        ResourceLocation skin = AvatarCache.getSkin(mc, invite.getUUID(), invite.getUsername());
        int headSize = 12;
        int headX = x + 4;
        int headY = y + (entryHeight - headSize) / 2;
        PlayerFaceRenderer.draw(g, skin, headX, headY, headSize);

        g.drawString(mc.font, invite.getUsername(), x + 22, y + (entryHeight - 8) / 2, 0xFFFFFF88, false);

        int btnY = y + (entryHeight - BTN_H) / 2;
        btnRevoke.setX(x + entryWidth - BTN_W - 2);
        btnRevoke.setY(btnY);
        btnRevoke.render(g, mouseX, mouseY, delta);
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
        return List.of(btnRevoke);
    }

    @Override
    public List<? extends net.minecraft.client.gui.narration.NarratableEntry> narratables() {
        return List.of(btnRevoke);
    }

    @Override
    public Optional<Component[]> getTooltip() { return Optional.empty(); }
}
