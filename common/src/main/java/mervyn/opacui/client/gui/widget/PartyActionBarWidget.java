package mervyn.opacui.client.gui.widget;

import mervyn.opacui.client.gui.ConfirmActionScreen;
import mervyn.opacui.client.util.PartyCommands;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import xaero.pac.client.parties.party.api.IClientPartyAPI;

import java.util.function.Consumer;

/**
 * Bottom action bar widget for PartyScreen: Invite/Add Ally input, Send button, Leave/Disband, and Done.
 */
public class PartyActionBarWidget {

    private EditBox inviteBox;
    private Button btnSendInvite;
    private Button btnLeaveDisband;
    private Button btnDone;

    public void init(
            Screen screen,
            Font font,
            int width,
            int height,
            int barYOffset,
            int bottomYOffset,
            boolean localIsOwner,
            boolean canModeratorPlus,
            int activeTabIndex,
            IClientPartyAPI party,
            Consumer<Component> showFeedback,
            Runnable scheduleRefresh,
            Consumer<String> onTextChange
    ) {
        Minecraft mc = Minecraft.getInstance();
        int barY = height - barYOffset;

        inviteBox = new EditBox(font, width / 2 - 132, barY, 198, 20, Component.translatable("screen.opacui.invite_placeholder"));
        inviteBox.setMaxLength(32);
        inviteBox.setHint(Component.translatable("screen.opacui.invite_placeholder"));
        inviteBox.setResponder(onTextChange);

        btnSendInvite = Button.builder(
                activeTabIndex == 2
                        ? Component.translatable("screen.opacui.add_ally")
                        : Component.translatable("screen.opacui.invite"),
                b -> handleSend(mc, party, activeTabIndex, showFeedback, scheduleRefresh)
        ).bounds(width / 2 + 70, barY, 62, 20).build();
        btnSendInvite.active = canModeratorPlus;

        int bottomY = height - bottomYOffset;
        btnLeaveDisband = Button.builder(
                localIsOwner
                        ? Component.translatable("screen.opacui.disband_party")
                        : Component.translatable("screen.opacui.leave_party"),
                b -> {
                    if (localIsOwner) {
                        mc.setScreen(new ConfirmActionScreen(
                                screen,
                                Component.translatable("screen.opacui.confirm_title"),
                                Component.translatable("screen.opacui.confirm_disband"),
                                () -> {
                                    PartyCommands.destroyParty(mc);
                                    mc.setScreen(null);
                                }
                        ));
                    } else {
                        PartyCommands.leaveParty(mc);
                        mc.setScreen(null);
                    }
                }
        ).bounds(width / 2 - 105, bottomY, 100, 20).build();

        btnDone = Button.builder(
                Component.translatable("gui.done"),
                b -> screen.onClose()
        ).bounds(width / 2 + 5, bottomY, 100, 20).build();

        updateForTab(activeTabIndex);
    }

    public void updateForTab(int tabIndex) {
        if (btnSendInvite == null || inviteBox == null) return;
        if (tabIndex == 2) {
            btnSendInvite.setMessage(Component.translatable("screen.opacui.add_ally"));
            inviteBox.setHint(Component.translatable("screen.opacui.ally_placeholder"));
        } else {
            btnSendInvite.setMessage(Component.translatable("screen.opacui.invite"));
            inviteBox.setHint(Component.translatable("screen.opacui.invite_placeholder"));
        }
    }

    public void populateInputBox(String text) {
        if (inviteBox != null) {
            inviteBox.setValue(text);
            inviteBox.moveCursorToEnd();
        }
    }

    public boolean isFocused() {
        return inviteBox != null && inviteBox.isFocused();
    }

    public EditBox getInviteBox() {
        return inviteBox;
    }

    public Button getBtnSendInvite() {
        return btnSendInvite;
    }

    public Button getBtnLeaveDisband() {
        return btnLeaveDisband;
    }

    public Button getBtnDone() {
        return btnDone;
    }

    public boolean handleEnterKey() {
        if (inviteBox != null && inviteBox.isFocused() && btnSendInvite != null && btnSendInvite.active) {
            btnSendInvite.onPress();
            return true;
        }
        return false;
    }

    private void handleSend(Minecraft mc, IClientPartyAPI party, int activeTabIndex, Consumer<Component> showFeedback, Runnable scheduleRefresh) {
        if (inviteBox == null || party == null) return;
        String name = inviteBox.getValue().trim();
        if (name.isEmpty()) return;

        boolean isOnline = mc.getConnection() != null && mc.getConnection().getOnlinePlayers().stream()
                .anyMatch(pi -> pi.getProfile().getName().equalsIgnoreCase(name));
        if (!isOnline) {
            showFeedback.accept(Component.translatable("screen.opacui.feedback.player_not_online", name));
            return;
        }

        boolean isAlreadyMember = party.getMemberInfoStream()
                .anyMatch(m -> m.getUsername().equalsIgnoreCase(name));
        if (isAlreadyMember) {
            showFeedback.accept(Component.translatable("screen.opacui.feedback.already_in_party", name));
            return;
        }

        if (activeTabIndex == 2) {
            PartyCommands.addAlly(mc, name);
            showFeedback.accept(Component.translatable("screen.opacui.feedback.ally_added", name));
        } else {
            PartyCommands.invite(mc, name);
            showFeedback.accept(Component.translatable("screen.opacui.feedback.invited", name));
        }
        inviteBox.setValue("");
        scheduleRefresh.run();
    }
}
