package net.deepacat.deepamonu.configUtils;

import me.shedaniel.clothconfig2.gui.entries.AbstractListListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MobGlowListEntry extends AbstractListListEntry<MapEntry, MobGlowListEntry.MobGlowCell, MobGlowListEntry> {

    public MobGlowListEntry(Component fieldName,
                            List<MapEntry> value,
                            boolean defaultExpanded,
                            Supplier<Optional<Component[]>> tooltipSupplier,
                            Consumer<List<MapEntry>> saveConsumer,
                            Supplier<List<MapEntry>> defaultValue,
                            Component resetButtonKey) {
        super(fieldName, value, defaultExpanded, tooltipSupplier, saveConsumer, defaultValue,
                resetButtonKey, false, true, false, MobGlowCell::new);
    }

    @Override
    public MobGlowListEntry self() {
        return this;
    }

    public static class MobGlowCell extends AbstractListCell<MapEntry, MobGlowCell, MobGlowListEntry> {
        private static final int CELL_HEIGHT = 20;
        private static final int GAP = 5;
        private static final int SWATCH_SIZE = 18;
        private static final int COLOR_FIELD_WIDTH = 60;

        private final MapEntry entry;
        private final String originalKey;
        private final int originalColor;
        private final EditBox keyField;
        private final EditBox colorField;

        private int currentColor;
        private boolean selected;
        private boolean hovered;

        public MobGlowCell(MapEntry value, MobGlowListEntry listListEntry) {
            super(value, listListEntry);

            this.entry = value == null ? new MapEntry("Gravity Bomb", 0xFF0000) : new MapEntry(value);
            this.originalKey = entry.key == null ? "" : entry.key;
            this.originalColor = entry.color & 0xFFFFFF;
            this.currentColor = originalColor;

            this.keyField = new EditBox(Minecraft.getInstance().font, 0, 0, 100, 18, Component.empty());
            this.keyField.setMaxLength(128);
            this.keyField.setValue(originalKey);
            this.keyField.moveCursorToStart(false);
            this.keyField.setResponder(text -> entry.key = text);
            this.keyField.setTooltip(Tooltip.create(
                    Component.translatable("config.deepamonu.mobGlowColorOverrides.keyTooltip")));

            this.colorField = new EditBox(Minecraft.getInstance().font, 0, 0, 100, 18, Component.empty());
            this.colorField.setMaxLength(7);
            this.colorField.setValue(String.format("%06X", currentColor));
            this.colorField.moveCursorToStart(false);
            this.colorField.setFilter(text -> text.matches("[#]?[0-9a-fA-F]{0,6}"));
            this.colorField.setResponder(this::updateColor);
            this.colorField.setTooltip(Tooltip.create(
                    Component.translatable("config.deepamonu.mobGlowColorOverrides.colorTooltip")));
        }

        private void updateColor(String text) {
            if (text == null || text.isEmpty()) return;
            String clean = text.startsWith("#") ? text.substring(1) : text;
            if (clean.isEmpty()) return;
            try {
                currentColor = Integer.parseInt(clean, 16) & 0xFFFFFF;
            } catch (NumberFormatException ignored) {
            }
        }

        @Override
        public MapEntry getValue() {
            return new MapEntry(keyField.getValue().trim(), currentColor);
        }

        @Override
        public Optional<Component> getError() {
            return Optional.empty();
        }

        @Override
        public int getCellHeight() {
            return CELL_HEIGHT;
        }

        @Override
        public void updateSelected(boolean selected) {
            this.selected = selected;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight,
                           int mouseX, int mouseY, boolean isSelected, float delta) {
            int keyWidth = Math.max(40, entryWidth - SWATCH_SIZE - COLOR_FIELD_WIDTH - GAP * 2);

            keyField.setX(x);
            keyField.setY(y + 1);
            keyField.setWidth(keyWidth);
            keyField.setHeight(18);
            keyField.setEditable(listListEntry.isEditable());
            keyField.setFocused(selected && getFocused() != colorField);

            int swatchX = x + keyWidth + GAP;
            int swatchY = y + 1;

            int colorX = swatchX + SWATCH_SIZE + GAP;
            colorField.setX(colorX);
            colorField.setY(y + 1);
            colorField.setWidth(COLOR_FIELD_WIDTH);
            colorField.setHeight(18);
            colorField.setEditable(listListEntry.isEditable());
            colorField.setFocused(selected && getFocused() == colorField);

            keyField.render(graphics, mouseX, mouseY, delta);

            graphics.fill(swatchX, swatchY, swatchX + SWATCH_SIZE, swatchY + SWATCH_SIZE, 0xFF000000);
            graphics.fill(swatchX + 1, swatchY + 1, swatchX + SWATCH_SIZE - 1, swatchY + SWATCH_SIZE - 1,
                    0xFF000000 | currentColor);

            colorField.render(graphics, mouseX, mouseY, delta);

            hovered = keyField.isMouseOver(mouseX, mouseY) || colorField.isMouseOver(mouseX, mouseY);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(keyField, colorField);
        }

        @Override
        public boolean isEdited() {
            MapEntry value = getValue();
            return !value.key.equals(originalKey) || value.color != originalColor;
        }

        @Override
        public NarrationPriority narrationPriority() {
            return selected ? NarrationPriority.FOCUSED : hovered ? NarrationPriority.HOVERED : NarrationPriority.NONE;
        }

        @Override
        public void updateNarration(NarrationElementOutput output) {
            keyField.updateNarration(output);
            colorField.updateNarration(output);
        }
    }
}
