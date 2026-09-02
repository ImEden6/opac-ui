package mervyn.opacui.client.gui.entry;

import mervyn.opacui.client.gui.list.AbstractPartyEntry;
import mervyn.opacui.client.gui.PartyScreen;
import mervyn.opacui.client.util.AvatarCache;
import mervyn.opacui.client.util.PartyCommands;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.PlayerSkin;
import xaero.pac.common.parties.party.api.IPartyPlayerInfoAPI;

import java.util.List;
import java.util.function.Consumer;

/**
 * A single row in the Invites tab: shows the invited player's name and
 * a [Revoke] button that cancels the invitation (uses the kick command).
 */
public class InviteEntry extends AbstractPartyEntry {

    private static final int BTN_W = EntryButton.WIDTH;
    private static final int BTN_H = EntryButton.HEIGHT;

    private final IPartyPlayerInfoAPI invite;
    private final Button btnRevoke;

    public InviteEntry(IPartyPlayerInfoAPI invite, Consumer<Component> onAction) {
        this.invite = invite;

        Minecraft mc = Minecraft.getInstance();
        btnRevoke = EntryButton.create(Component.translatable("screen.opacui.revoke"), b -> {
            // Revoking an invite uses the same kick command server-side
            PartyCommands.kick(mc, invite.getUsername());
            onAction.accept(Component.translatable("screen.opacui.feedback.revoked", invite.getUsername()));
        });
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && Minecraft.getInstance().gui.screen() instanceof PartyScreen ps) {
            ps.populateInputBox(invite.getUsername());
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractContent(GuiGraphicsExtractor g, int mouseX, int mouseY, boolean isHovered, float delta) {
        Minecraft mc = Minecraft.getInstance();
        int x = getContentX();
        int y = getContentY();
        int entryWidth = getContentWidth();
        int entryHeight = getContentHeight();

        // ── Player Face Avatar ────────────────────────────────────────────
        PlayerSkin skin = AvatarCache.getSkin(mc, invite.getUUID(), invite.getUsername());
        int headSize = 12;
        int headX = x + 4;
        int headY = y + (entryHeight - headSize) / 2;
        PlayerFaceExtractor.extractRenderState(g, skin, headX, headY, headSize);

        g.text(mc.font, invite.getUsername(), x + 22, y + (entryHeight - 8) / 2, 0xFFFFFF88);

        int btnY = y + (entryHeight - BTN_H) / 2;
        btnRevoke.setX(x + entryWidth - BTN_W - 2);
        btnRevoke.setY(btnY);
        btnRevoke.extractRenderState(g, mouseX, mouseY, delta);
    }

    @Override
    public List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children() {
        return List.of(btnRevoke);
    }

    @Override
    public List<? extends net.minecraft.client.gui.narration.NarratableEntry> narratables() {
        return List.of(btnRevoke);
    }
}
