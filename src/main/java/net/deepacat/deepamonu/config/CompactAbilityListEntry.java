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

public class CompactAbilityListEntry extends AbstractConfigListEntry<List<CompactAbilityEntry>> {

    private List<CompactAbilityEntry> value;
    private final Supplier<List<CompactAbilityEntry>> defaultValue;
    private final Consumer<List<CompactAbilityEntry>> saveConsumer;

    private final List<Row> rows = new ArrayList<>();
    private Button addButton;
    private boolean dirty = false;

    private static final Component ADD_TOOLTIP = Component.literal("Add a new ability tracker");
    private static final Component REMOVE_TOOLTIP = Component.literal("Remove this entry");
    private static final Component NAME_TOOLTIP = Component.literal("Monumenta ability name to track (case-insensitive)");
    private static final Component FORMAT_TOOLTIP = Component.literal("Format string. Use %cd/%cds for cooldown, %ch/%chc/%cht for charges, %cdd/%cdds for decimal cooldown. Supports § color codes");

    private static final int LEFT_PADDING = 5;
    private static final int RIGHT_PADDING = 25;
    private static final int ROW_GAP = 2;
    private static final int ROW_HEIGHT = 28;

    public CompactAbilityListEntry(Component fieldName,
                                   List<CompactAbilityEntry> value,
                                   Supplier<Optional<Component[]>> tooltipSupplier,
                                   Consumer<List<CompactAbilityEntry>> saveConsumer,
                                   Supplier<List<CompactAbilityEntry>> defaultValue,
                                   Component resetButtonKey) {
        super(fieldName, true);
        this.value = new ArrayList<>(value);
        this.defaultValue = defaultValue;
        this.saveConsumer = saveConsumer;
        if (this.value.isEmpty()) {
            this.value.add(new CompactAbilityEntry("", "%cds"));
        }
        rebuildRows();
    }

    private void rebuildRows() {
        rows.clear();
        for (CompactAbilityEntry entry : value) {
            rows.add(new Row(entry));
        }
    }

    @Override
    public boolean isEdited() {
        return dirty;
    }

    @Override
    public List<CompactAbilityEntry> getValue() {
        return value;
    }

    @Override
    public Optional<List<CompactAbilityEntry>> getDefaultValue() {
        return Optional.of(defaultValue.get());
    }

    @Override
    public void save() {
        dirty = false;
    }

    private void commitChange() {
        List<CompactAbilityEntry> newList = new ArrayList<>();
        for (Row row : rows) {
            String name = row.nameField.getValue().trim();
            if (!name.isEmpty()) {
                newList.add(new CompactAbilityEntry(name, row.formatValue));
            }
        }
        value = newList;
        saveConsumer.accept(value);
        dirty = true;
    }

    @Override
    public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight,
                       int mouseX, int mouseY, boolean isHovered, float delta) {
        int currentY = y;
        int usableWidth = entryWidth - LEFT_PADDING - RIGHT_PADDING;

        if (addButton == null) {
            addButton = Button.builder(Component.literal("+ Add ability"), btn -> addRow())
                    .bounds(0, 0, 100, 20)
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
        rows.add(new Row(new CompactAbilityEntry("", "%cds")));
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
            children.add(row.nameField);
            children.add(row.formatField);
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
            if (row.mouseClicked(mouseX, mouseY, button)) return true;
        }
        if (addButton != null && addButton.mouseClicked(mouseX, mouseY, button)) return true;
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
            if (row.nameField.isFocused() && row.nameField.keyPressed(keyCode, scanCode, modifiers)) return true;
            if (row.formatField.isFocused() && row.formatField.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        for (Row row : rows) {
            if (row.nameField.isFocused() && row.nameField.charTyped(codePoint, modifiers)) return true;
            if (row.formatField.isFocused() && row.formatField.charTyped(codePoint, modifiers)) return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    private class Row {
        final EditBox nameField;
        final EditBox formatField;
        final Button removeButton;
        final CompactAbilityEntry entry;
        String formatValue;

        private int lastX, lastY, lastWidth, lastHeight;

        Row(CompactAbilityEntry entry) {
            this.entry = entry;
            this.formatValue = entry.formatString;

            nameField = new EditBox(Minecraft.getInstance().font, 0, 0, 100, 20, Component.empty());
            nameField.setMaxLength(128);
            nameField.setValue(entry.abilityName);
            nameField.setCursorPosition(0);
            nameField.setResponder(newName -> {
                this.entry.abilityName = newName;
                commitChange();
            });
            nameField.setTooltip(Tooltip.create(NAME_TOOLTIP));

            formatField = new EditBox(Minecraft.getInstance().font, 0, 0, 100, 20, Component.empty());
            formatField.setMaxLength(256);
            formatField.setValue(entry.formatString);
            formatField.setCursorPosition(0);
            formatField.setResponder(newFmt -> {
                this.formatValue = newFmt;
                this.entry.formatString = newFmt;
                commitChange();
            });
            formatField.setTooltip(Tooltip.create(FORMAT_TOOLTIP));

            removeButton = Button.builder(Component.literal("\u2715"), btn -> removeRow(this))
                    .bounds(0, 0, 20, 20)
                    .tooltip(Tooltip.create(REMOVE_TOOLTIP))
                    .build();
        }

        private void updateBounds() {
            int gap = 4;
            int btnWidth = 20;
            int nameWidth = (lastWidth - gap * 3 - btnWidth) / 3;
            int fmtWidth = lastWidth - nameWidth - gap * 3 - btnWidth;

            nameField.setX(lastX);
            nameField.setY(lastY + 4);
            nameField.setWidth(nameWidth);
            nameField.setHeight(20);

            formatField.setX(lastX + nameWidth + gap);
            formatField.setY(lastY + 4);
            formatField.setWidth(fmtWidth);
            formatField.setHeight(20);

            removeButton.setX(lastX + nameWidth + gap + fmtWidth + gap);
            removeButton.setY(lastY + 4);
        }

        void render(GuiGraphics graphics, int x, int y, int width, int height,
                    int mouseX, int mouseY, float delta) {
            this.lastX = x;
            this.lastY = y;
            this.lastWidth = width;
            this.lastHeight = height;
            updateBounds();

            nameField.render(graphics, mouseX, mouseY, delta);
            formatField.render(graphics, mouseX, mouseY, delta);
            removeButton.render(graphics, mouseX, mouseY, delta);
        }

        boolean mouseClicked(double mouseX, double mouseY, int button) {
            updateBounds();

            if (nameField.mouseClicked(mouseX, mouseY, button)) {
                nameField.setFocused(true);
                nameField.setCursorPosition(nameField.getValue().length());
                if (getConfigScreen() != null)
                    Minecraft.getInstance().tell(() -> getConfigScreen().setFocused(nameField));
                if (formatField.isFocused()) formatField.setFocused(false);
                return true;
            }
            if (formatField.mouseClicked(mouseX, mouseY, button)) {
                formatField.setFocused(true);
                formatField.setCursorPosition(formatField.getValue().length());
                if (getConfigScreen() != null)
                    Minecraft.getInstance().tell(() -> getConfigScreen().setFocused(formatField));
                if (nameField.isFocused()) nameField.setFocused(false);
                return true;
            }
            if (removeButton.mouseClicked(mouseX, mouseY, button)) {
                if (nameField.isFocused()) nameField.setFocused(false);
                if (formatField.isFocused()) formatField.setFocused(false);
                if (getConfigScreen() != null)
                    Minecraft.getInstance().tell(() -> getConfigScreen().setFocused(null));
                return true;
            }
            if (nameField.isFocused()) {
                nameField.setFocused(false);
                if (getConfigScreen() != null)
                    Minecraft.getInstance().tell(() -> getConfigScreen().setFocused(null));
            }
            if (formatField.isFocused()) {
                formatField.setFocused(false);
                if (getConfigScreen() != null)
                    Minecraft.getInstance().tell(() -> getConfigScreen().setFocused(null));
            }
            return false;
        }

        boolean mouseReleased(double mouseX, double mouseY, int button) {
            updateBounds();
            if (nameField.mouseReleased(mouseX, mouseY, button)) return true;
            if (formatField.mouseReleased(mouseX, mouseY, button)) return true;
            if (removeButton.mouseReleased(mouseX, mouseY, button)) return true;
            return false;
        }
    }
}
