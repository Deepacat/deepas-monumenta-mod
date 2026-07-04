package net.deepacat.deepamonu.config;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MapListEntry extends AbstractConfigListEntry<List<MapEntry>> {

    private List<MapEntry> value;
    private final Supplier<List<MapEntry>> defaultValue;
    private final Consumer<List<MapEntry>> saveConsumer;

    private final List<Row> rows = new ArrayList<>();
    private Button addButton;
    private boolean dirty = false;

    // Tooltips
    private static final Component ADD_TOOLTIP = Component.translatable("config.deepamonu.mobGlowColorOverrides.addTooltip");
    private static final Component REMOVE_TOOLTIP = Component.translatable("config.deepamonu.mobGlowColorOverrides.removeTooltip");
    private static final Component KEY_TOOLTIP = Component.translatable("config.deepamonu.mobGlowColorOverrides.keyTooltip");
    private static final Component COLOR_TOOLTIP = Component.translatable("config.deepamonu.mobGlowColorOverrides.colorTooltip");

    private static final int LEFT_PADDING = 5;
    private static final int RIGHT_PADDING = 25;
    private static final int ROW_GAP = 2;
    private static final int ROW_HEIGHT = 28;

    public MapListEntry(Component fieldName,
                        List<MapEntry> value,
                        Supplier<Optional<Component[]>> tooltipSupplier,
                        Consumer<List<MapEntry>> saveConsumer,
                        Supplier<List<MapEntry>> defaultValue,
                        Component resetButtonKey) {
        super(fieldName, true);
        this.value = new ArrayList<>(value);
        this.defaultValue = defaultValue;
        this.saveConsumer = saveConsumer;
        rebuildRows();
    }

    private void rebuildRows() {
        rows.clear();
        for (MapEntry entry : value) {
            rows.add(new Row(entry));
        }
    }

    @Override
    public boolean isEdited() {
        return dirty;
    }

    @Override
    public List<MapEntry> getValue() {
        return value;
    }

    @Override
    public Optional<List<MapEntry>> getDefaultValue() {
        return Optional.of(defaultValue.get());
    }

    @Override
    public void save() {
        dirty = false;
    }

    private void commitChange() {
        List<MapEntry> newList = new ArrayList<>();
        for (Row row : rows) {
            String key = row.keyField.getValue().trim();
            if (!key.isEmpty()) {
                newList.add(new MapEntry(key, row.currentColor));
            }
        }
        value = newList;
        saveConsumer.accept(value);

        // Mark as edited so the "Save & Quit" button activates
        dirty = true;
    }

    @Override
    public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight,
                       int mouseX, int mouseY, boolean isHovered, float delta) {
        int currentY = y;
        int usableWidth = entryWidth - LEFT_PADDING - RIGHT_PADDING;

        // Lazy-create add button
        if (addButton == null) {
            addButton = Button.builder(Component.literal("+"), btn -> addRow())
                    .bounds(0, 0, 40, 20)
                    .tooltip(Tooltip.create(ADD_TOOLTIP))
                    .build();
        }
        addButton.setX(x + LEFT_PADDING);
        addButton.setY(currentY + 2);
        addButton.render(graphics, mouseX, mouseY, delta);
        currentY += 26;

        for (Row row : rows) {
            row.render(graphics, x + LEFT_PADDING, currentY, usableWidth, ROW_HEIGHT, mouseX, mouseY, delta);
            currentY += ROW_HEIGHT + ROW_GAP;
        }
    }

    private void addRow() {
        rows.add(new Row(new MapEntry("Gravity Bomb", 0xFF0000)));
        commitChange();
    }

    private void removeRow(Row row) {
        rows.remove(row);
        commitChange();
    }

    @Override
    public int getItemHeight() {
        return 26 + rows.size() * (ROW_HEIGHT + ROW_GAP);
    }

    @Override
    public List<? extends GuiEventListener> children() {
        List<GuiEventListener> children = new ArrayList<>();
        if (addButton != null) children.add(addButton);
        for (Row row : rows) {
            children.add(row.keyField);
            children.add(row.hexField);
            children.add(row.removeButton);
        }
        return children;
    }

    @Override
    public List<? extends NarratableEntry> narratables() {
        return children().stream()
                .filter(e -> e instanceof NarratableEntry)
                .map(e -> (NarratableEntry) e)
                .collect(Collectors.toList());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (Row row : rows) {
            if (row.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        if (addButton != null && addButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        for (Row row : rows) {
            if (row.mouseReleased(mouseX, mouseY, button)) return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (Row row : rows) {
            if (row.keyField.isFocused() && row.keyField.keyPressed(keyCode, scanCode, modifiers)) return true;
            if (row.hexField.isFocused() && row.hexField.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        for (Row row : rows) {
            if (row.keyField.isFocused() && row.keyField.charTyped(codePoint, modifiers)) return true;
            if (row.hexField.isFocused() && row.hexField.charTyped(codePoint, modifiers)) return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    // ---- Row with inline color editing ----
    private class Row {
        final EditBox keyField;
        final EditBox hexField;
        final Button removeButton;
        final MapEntry entry;
        int currentColor;
        int lastValidColor;

        // Stored last render positions to update bounds before mouse events
        private int lastX, lastY, lastWidth, lastHeight;

        private static final int SWATCH_SIZE = 20;

        Row(MapEntry entry) {
            this.entry = entry;
            this.currentColor = entry.color;
            this.lastValidColor = entry.color;

            // Key field – give it a non-zero initial width
            keyField = new EditBox(
                    Minecraft.getInstance().font,
                    0, 0, 100, 20,
                    Component.empty()
            );
            keyField.setMaxLength(128);
            keyField.setValue(entry.key);
            keyField.setResponder(newKey -> {
                this.entry.key = newKey;
                commitChange();
            });
            keyField.setTooltip(Tooltip.create(KEY_TOOLTIP));

            // Hex field – same fix
            hexField = new EditBox(
                    Minecraft.getInstance().font,
                    0, 0, 100, 20,
                    Component.empty()
            );
            hexField.setMaxLength(6);
            hexField.setValue(String.format("%06X", currentColor));
            hexField.setResponder(this::updatePreviewFromHex);
            hexField.setTooltip(Tooltip.create(COLOR_TOOLTIP));

            // Remove button – unchanged
            removeButton = Button.builder(Component.literal("✕"), btn -> removeRow(this))
                    .bounds(0, 0, 20, 20)
                    .tooltip(Tooltip.create(REMOVE_TOOLTIP))
                    .build();
        }

        private void updatePreviewFromHex(String text) {
            if (text == null || text.isEmpty()) return;
            try {
                String clean = text.startsWith("#") ? text.substring(1) : text;
                int parsed = Integer.parseInt(clean, 16);
                this.currentColor = parsed;
            } catch (NumberFormatException ignored) {
            }
        }

        private void commitHex() {
            String text = hexField.getValue();
            try {
                String clean = text.startsWith("#") ? text.substring(1) : text;
                int parsed = Integer.parseInt(clean, 16);
                this.currentColor = parsed;
                this.lastValidColor = parsed;
                this.entry.color = parsed;
                commitChange();
                hexField.setValue(String.format("%06X", parsed));
            } catch (NumberFormatException e) {
                this.currentColor = lastValidColor;
                hexField.setValue(String.format("%06X", lastValidColor));
            }
        }

        // Update widget bounds based on stored last render positions
        private void updateBounds() {
            int gap = 5;
            int btnWidth = 20;
            int swatchWidth = SWATCH_SIZE;
            int totalFieldsWidth = lastWidth - gap * 4 - btnWidth - swatchWidth;

            int keyWidth = (int) (totalFieldsWidth * 0.8);
            int hexWidth = totalFieldsWidth - keyWidth;

            keyField.setX(lastX);
            keyField.setY(lastY + 4);
            keyField.setWidth(keyWidth);
            keyField.setHeight(20);

            int swatchX = lastX + keyWidth + gap;
            hexField.setX(swatchX + swatchWidth + gap);
            hexField.setY(lastY + 4);
            hexField.setWidth(hexWidth);
            hexField.setHeight(20);

            removeButton.setX(hexField.getX() + hexWidth + gap);
            removeButton.setY(lastY + 4);
        }

        void render(GuiGraphics graphics, int x, int y, int width, int height,
                    int mouseX, int mouseY, float delta) {
            // Store position for later mouse event handling
            this.lastX = x;
            this.lastY = y;
            this.lastWidth = width;
            this.lastHeight = height;

            updateBounds(); // sets widget positions

            keyField.render(graphics, mouseX, mouseY, delta);

            // Colour swatch
            int gap = 5;
            int btnWidth = 20;
            int swatchWidth = SWATCH_SIZE;
            int totalFieldsWidth = width - gap * 4 - btnWidth - swatchWidth;
            int keyWidth = (int) (totalFieldsWidth * 0.8);
            int swatchX = x + keyWidth + gap;
            int swatchY = y + 4;
            graphics.fill(swatchX, swatchY, swatchX + swatchWidth, swatchY + 20, 0xFF000000 | currentColor);
            graphics.fill(swatchX + 1, swatchY + 1, swatchX + swatchWidth - 1, swatchY + 20 - 1, currentColor);
            graphics.fill(swatchX, swatchY, swatchX + swatchWidth, swatchY + 1, 0xFF000000);
            graphics.fill(swatchX, swatchY + 19, swatchX + swatchWidth, swatchY + 20, 0xFF000000);
            graphics.fill(swatchX, swatchY, swatchX + 1, swatchY + 20, 0xFF000000);
            graphics.fill(swatchX + swatchWidth - 1, swatchY, swatchX + swatchWidth, swatchY + 20, 0xFF000000);

            hexField.render(graphics, mouseX, mouseY, delta);
            removeButton.render(graphics, mouseX, mouseY, delta);
        }

        boolean mouseClicked(double mouseX, double mouseY, int button) {
            updateBounds();

            // Key field
            if (keyField.mouseClicked(mouseX, mouseY, button)) {
                keyField.setFocused(true);
                keyField.setCursorPosition(keyField.getValue().length());
                if (getConfigScreen() != null) {
                    Minecraft.getInstance().tell(() -> getConfigScreen().setFocused(keyField));
                }
                if (hexField.isFocused()) {
                    hexField.setFocused(false);
                    commitHex();
                }
                return true;
            }
            // Hex field
            if (hexField.mouseClicked(mouseX, mouseY, button)) {
                hexField.setFocused(true);
                hexField.setCursorPosition(hexField.getValue().length());
                if (getConfigScreen() != null) {
                    Minecraft.getInstance().tell(() -> getConfigScreen().setFocused(hexField));
                }
                if (keyField.isFocused()) {
                    keyField.setFocused(false);
                }
                return true;
            }
            // Remove button
            if (removeButton.mouseClicked(mouseX, mouseY, button)) {
                if (hexField.isFocused()) {
                    hexField.setFocused(false);
                    commitHex();
                }
                if (keyField.isFocused()) {
                    keyField.setFocused(false);
                }
                if (getConfigScreen() != null) {
                    Minecraft.getInstance().tell(() -> getConfigScreen().setFocused(null));
                }
                return true;
            }
            // Clicked outside – lose focus
            if (hexField.isFocused()) {
                hexField.setFocused(false);
                commitHex();
                if (getConfigScreen() != null) {
                    Minecraft.getInstance().tell(() -> getConfigScreen().setFocused(null));
                }
            }
            if (keyField.isFocused()) {
                keyField.setFocused(false);
                if (getConfigScreen() != null) {
                    Minecraft.getInstance().tell(() -> getConfigScreen().setFocused(null));
                }
            }
            return false;
        }

        boolean mouseReleased(double mouseX, double mouseY, int button) {
            // Ensure bounds are up‑to‑date
            updateBounds();
            if (keyField.mouseReleased(mouseX, mouseY, button)) return true;
            if (hexField.mouseReleased(mouseX, mouseY, button)) return true;
            if (removeButton.mouseReleased(mouseX, mouseY, button)) return true;
            return false;
        }
    }
}