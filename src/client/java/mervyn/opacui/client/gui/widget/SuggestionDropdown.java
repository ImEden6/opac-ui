package mervyn.opacui.client.gui.widget;

import mervyn.opacui.client.util.AvatarCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Encapsulates auto-complete suggestion dropdown state, rendering, and input handling.
 */
public class SuggestionDropdown {

    private List<String> suggestions = List.of();
    private int selectedSuggestion = -1;
    private boolean visible;

    public boolean isVisible() {
        return visible && !suggestions.isEmpty();
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void update(String text, Minecraft mc, Set<String> excludeNames) {
        if (text == null || text.isEmpty() || mc == null || mc.getConnection() == null) {
            suggestions = List.of();
            visible = false;
            selectedSuggestion = -1;
            return;
        }

        String lower = text.toLowerCase();
        Set<String> lowerExcluded = excludeNames != null
                ? excludeNames.stream().map(String::toLowerCase).collect(Collectors.toSet())
                : Set.of();

        suggestions = mc.getConnection().getOnlinePlayers().stream()
                .map(pi -> pi.getProfile().getName())
                .filter(name -> !lowerExcluded.contains(name.toLowerCase()))
                .filter(name -> name.toLowerCase().startsWith(lower))
                .sorted()
                .limit(5)
                .toList();

        selectedSuggestion = -1;
        visible = !suggestions.isEmpty();
    }

    public void render(GuiGraphics g, Font font, Minecraft mc, int boxX, int boxY, int width) {
        if (!isVisible()) return;

        int itemH = font.lineHeight + 4;
        int maxVisible = Math.min(suggestions.size(), 5);
        int ddH = maxVisible * itemH + 4;
        int ddY = boxY - ddH - 2;

        g.fill(boxX, ddY, boxX + width, ddY + ddH, 0xCC000000);
        for (int i = 0; i < maxVisible; i++) {
            int itemY = ddY + 2 + i * itemH;
            if (i == selectedSuggestion) {
                g.fill(boxX, itemY, boxX + width, itemY + itemH, 0x55555555);
            }
            String sName = suggestions.get(i);
            ResourceLocation skin = AvatarCache.getSkin(mc, null, sName);
            PlayerFaceRenderer.draw(g, skin, boxX + 4, itemY + 1, 8);
            g.drawString(font, sName, boxX + 16, itemY + 1, 0xFFFFFFFF, false);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int boxX, int boxY, int width, Font font, Consumer<String> onSelect) {
        if (!isVisible()) return false;

        int itemH = font.lineHeight + 4;
        int maxVisible = Math.min(suggestions.size(), 5);
        int ddH = maxVisible * itemH + 4;
        int ddY = boxY - ddH - 2;

        if (mouseX >= boxX && mouseX < boxX + width && mouseY >= ddY && mouseY < ddY + ddH) {
            int idx = (int) ((mouseY - ddY - 2) / itemH);
            if (idx >= 0 && idx < suggestions.size()) {
                selectSuggestion(idx, onSelect);
            }
            return true;
        }

        visible = false;
        return false;
    }

    public boolean keyPressed(int keyCode, Consumer<String> onSelect) {
        if (!isVisible()) return false;

        if (keyCode == GLFW.GLFW_KEY_UP) {
            selectedSuggestion = Math.max(0, selectedSuggestion - 1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            selectedSuggestion = Math.min(suggestions.size() - 1, selectedSuggestion + 1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER && selectedSuggestion >= 0) {
            selectSuggestion(selectedSuggestion, onSelect);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            visible = false;
            return true;
        }
        return false;
    }

    private void selectSuggestion(int index, Consumer<String> onSelect) {
        if (index >= 0 && index < suggestions.size()) {
            onSelect.accept(suggestions.get(index));
        }
        visible = false;
    }
}
