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

    private static final Component ADD_TOOLTIP = Component.translatable("config.deepamonu.compactAbilities.addTooltip");
    private static final Component REMOVE_TOOLTIP = Component.translatable("config.deepamonu.compactAbilities.removeTooltip");
    private static final Component NAME_TOOLTIP = Component.translatable("config.deepamonu.compactAbilities.nameTooltip");
    private static final Component FORMAT_TOOLTIP = Component.translatable("config.deepamonu.compactAbilities.formatTooltip");
    private static final Component X_TOOLTIP = Component.translatable("config.deepamonu.compactAbilities.xTooltip");
    private static final Component Y_TOOLTIP = Component.translatable("config.deepamonu.compactAbilities.yTooltip");
    private static final Component SCALE_TOOLTIP = Component.translatable("config.deepamonu.compactAbilities.scaleTooltip");
    private static final Component ALIGN_TOOLTIP = TextAlignmentTooltip.tooltip();

    private static final int LEFT_PADDING = 5;
    private static final int RIGHT_PADDING = 25;
    private static final int ROW_GAP = 2;
    private static final int ROW_HEIGHT = 26;

    public CompactAbilityListEntry(Component fieldName,
                                   List<CompactAbilityEntry> value,
                                   Supplier<Optional<Component[]>> tooltipSupplier,
                                   Consumer<List<CompactAbilityEntry>> saveConsumer,
                                   Supplier<List<CompactAbilityEntry>> defaultValue,
                                   Component resetButtonKey) {
        super(fieldName, false);
        this.value = value.stream().map(CompactAbilityEntry::new).collect(Collectors.toCollection(ArrayList::new));
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
    public boolean isEdited() { return dirty; }

    @Override
    public List<CompactAbilityEntry> getValue() { return value; }

    @Override
    public Optional<List<CompactAbilityEntry>> getDefaultValue() { return Optional.of(defaultValue.get()); }

    @Override
    public void save() {
        commitChange();
        saveConsumer.accept(value);
        dirty = false;
    }

    private void commitChange() {
        List<CompactAbilityEntry> newList = new ArrayList<>();
        for (Row row : rows) {
            String name = row.nameField.getValue().trim();
            if (!name.isEmpty()) {
                newList.add(new CompactAbilityEntry(name, row.formatValue,
                        row.xOffsetVal, row.yOffsetVal, row.scaleVal, row.alignVal));
            }
        }
        value = newList;
        dirty = true;
    }

    @Override
    public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight,
                       int mouseX, int mouseY, boolean isHovered, float delta) {
        int currentY = y;
        if (addButton == null) {
            addButton = Button.builder(Component.translatable("config.deepamonu.compactAbilities.addButton"), btn -> addRow())
                    .bounds(0, 0, 100, 20).tooltip(Tooltip.create(ADD_TOOLTIP)).build();
        }
        addButton.setX(x + LEFT_PADDING);
        addButton.setY(currentY + 2);
        addButton.render(graphics, mouseX, mouseY, delta);
        currentY += 24;

        int usableWidth = entryWidth - LEFT_PADDING - RIGHT_PADDING;
        for (Row row : rows) {
            row.render(graphics, x + LEFT_PADDING, currentY, usableWidth, mouseX, mouseY, delta);
            currentY += ROW_HEIGHT * 2 + ROW_GAP;
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
    public int getItemHeight() { return 24 + rows.size() * (ROW_HEIGHT * 2 + ROW_GAP); }

    @Override
    public List<? extends GuiEventListener> children() {
        List<GuiEventListener> children = new ArrayList<>();
        if (addButton != null) children.add(addButton);
        for (Row row : rows) {
            children.add(row.nameField);
            children.add(row.formatField);
            children.add(row.xField);
            children.add(row.yField);
            children.add(row.scaleField);
            children.add(row.alignField);
            children.add(row.removeButton);
        }
        return children;
    }

    @Override
    public List<? extends NarratableEntry> narratables() {
        return children().stream().filter(e -> e instanceof NarratableEntry)
                .map(e -> (NarratableEntry) e).collect(Collectors.toList());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (Row row : rows) { if (row.mouseClicked(mouseX, mouseY, button)) return true; }
        if (addButton != null && addButton.mouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        for (Row row : rows) { if (row.mouseReleased(mouseX, mouseY, button)) return true; }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override public boolean keyPressed(int k, int s, int m) {
        for (Row row : rows) {
            if (row.nameField.isFocused() && row.nameField.keyPressed(k, s, m)) return true;
            if (row.formatField.isFocused() && row.formatField.keyPressed(k, s, m)) return true;
            if (row.xField.isFocused() && row.xField.keyPressed(k, s, m)) return true;
            if (row.yField.isFocused() && row.yField.keyPressed(k, s, m)) return true;
            if (row.scaleField.isFocused() && row.scaleField.keyPressed(k, s, m)) return true;
            if (row.alignField.isFocused() && row.alignField.keyPressed(k, s, m)) return true;
        }
        return super.keyPressed(k, s, m);
    }

    @Override public boolean charTyped(char c, int m) {
        for (Row row : rows) {
            if (row.nameField.isFocused() && row.nameField.charTyped(c, m)) return true;
            if (row.formatField.isFocused() && row.formatField.charTyped(c, m)) return true;
            if (row.xField.isFocused() && row.xField.charTyped(c, m)) return true;
            if (row.yField.isFocused() && row.yField.charTyped(c, m)) return true;
            if (row.scaleField.isFocused() && row.scaleField.charTyped(c, m)) return true;
            if (row.alignField.isFocused() && row.alignField.charTyped(c, m)) return true;
        }
        return super.charTyped(c, m);
    }

    private class Row {
        final EditBox nameField, formatField, xField, yField, scaleField, alignField;
        final Button removeButton;
        final CompactAbilityEntry entry;
        String formatValue;
        int xOffsetVal, yOffsetVal, alignVal;
        float scaleVal;

        private int lastX, lastY, lastWidth;

        Row(CompactAbilityEntry entry) {
            this.entry = entry;
            this.formatValue = entry.formatString;
            this.xOffsetVal = entry.xOffset;
            this.yOffsetVal = entry.yOffset;
            this.scaleVal = entry.textScale;
            this.alignVal = entry.alignment;

            nameField = makeEditBox(entry.abilityName, 128, NAME_TOOLTIP, val -> { entry.abilityName = val; commitChange(); });
            formatField = makeEditBox(entry.formatString, 256, FORMAT_TOOLTIP, val -> { formatValue = val; entry.formatString = val; commitChange(); });
            xField = makeNumberBox(xOffsetVal, X_TOOLTIP, val -> { xOffsetVal = val; entry.xOffset = val; commitChange(); });
            yField = makeNumberBox(yOffsetVal, Y_TOOLTIP, val -> { yOffsetVal = val; entry.yOffset = val; commitChange(); });
            scaleField = makeBox(String.valueOf(scaleVal), 4, SCALE_TOOLTIP, val -> {
                try { scaleVal = Float.parseFloat(val); entry.textScale = scaleVal; } catch (Exception ignored) {}
                commitChange();
            });
            alignField = makeNumberBox(alignVal, ALIGN_TOOLTIP, val -> { alignVal = val; entry.alignment = val; commitChange(); });

            removeButton = Button.builder(Component.literal("\u2715"), btn -> removeRow(this))
                    .bounds(0, 0, 20, 20).tooltip(Tooltip.create(REMOVE_TOOLTIP)).build();
        }

        private EditBox makeEditBox(String val, int maxLen, Component tooltip, Consumer<String> responder) {
            EditBox box = new EditBox(Minecraft.getInstance().font, 0, 0, 100, 20, Component.empty());
            box.setMaxLength(maxLen);
            box.setValue(val);
            box.setCursorPosition(0);
            box.setHighlightPos(0);
            box.setResponder(responder);
            box.setTooltip(Tooltip.create(tooltip));
            return box;
        }

        private EditBox makeNumberBox(int val, Component tooltip, java.util.function.IntConsumer responder) {
            return makeEditBox(String.valueOf(val), 6, tooltip, s -> {
                try { responder.accept(Integer.parseInt(s.trim())); } catch (Exception ignored) {}
            });
        }

        private EditBox makeBox(String val, int maxLen, Component tooltip, Consumer<String> responder) {
            return makeEditBox(val, maxLen, tooltip, responder);
        }

        private void unfocusAll() {
            if (nameField.isFocused()) nameField.setFocused(false);
            if (formatField.isFocused()) formatField.setFocused(false);
            if (xField.isFocused()) xField.setFocused(false);
            if (yField.isFocused()) yField.setFocused(false);
            if (scaleField.isFocused()) scaleField.setFocused(false);
            if (alignField.isFocused()) alignField.setFocused(false);
        }

        void render(GuiGraphics graphics, int x, int y, int width, int mouseX, int mouseY, float delta) {
            lastX = x; lastY = y; lastWidth = width;
            int gap = 3;
            int btnW = 20;
            int availW = width - btnW - gap;
            int bigW = (int)(availW * 0.35f) - gap / 2;
            int smallW = (availW - bigW * 2) / 4;
            int rowY1 = y + 2;
            int rowY2 = y + ROW_HEIGHT;

            // Row 1: name (big), x (small), y (small)
            setPos(nameField, x, rowY1, bigW);
            setPos(xField, x + bigW + gap, rowY1, smallW);
            setPos(yField, x + bigW + smallW + gap * 2, rowY1, smallW);
            // Row 2: format (big), scale (small), align (small), remove
            setPos(formatField, x, rowY2, bigW);
            setPos(scaleField, x + bigW + gap, rowY2, smallW);
            setPos(alignField, x + bigW + smallW + gap * 2, rowY2, smallW);
            removeButton.setX(x + bigW + smallW * 2 + gap * 3);
            removeButton.setY(rowY2);

            nameField.render(graphics, mouseX, mouseY, delta);
            xField.render(graphics, mouseX, mouseY, delta);
            yField.render(graphics, mouseX, mouseY, delta);
            formatField.render(graphics, mouseX, mouseY, delta);
            scaleField.render(graphics, mouseX, mouseY, delta);
            alignField.render(graphics, mouseX, mouseY, delta);
            removeButton.render(graphics, mouseX, mouseY, delta);
        }

        private void setPos(EditBox box, int x, int y, int w) {
            box.setX(x); box.setY(y); box.setWidth(w); box.setHeight(20);
        }

        boolean mouseClicked(double mx, double my, int btn) {
            if (clickField(nameField, mx, my, btn)) return true;
            if (clickField(formatField, mx, my, btn)) return true;
            if (clickField(xField, mx, my, btn)) return true;
            if (clickField(yField, mx, my, btn)) return true;
            if (clickField(scaleField, mx, my, btn)) return true;
            if (clickField(alignField, mx, my, btn)) return true;
            if (removeButton.mouseClicked(mx, my, btn)) { unfocusAll(); return true; }
            unfocusAll();
            return false;
        }

        private boolean clickField(EditBox field, double mx, double my, int btn) {
            if (field.mouseClicked(mx, my, btn)) {
                unfocusAll();
                field.setFocused(true);
                field.setCursorPosition(field.getValue().length());
                return true;
            }
            return false;
        }

        boolean mouseReleased(double mx, double my, int btn) {
            return nameField.mouseReleased(mx, my, btn) || formatField.mouseReleased(mx, my, btn)
                    || xField.mouseReleased(mx, my, btn) || yField.mouseReleased(mx, my, btn)
                    || scaleField.mouseReleased(mx, my, btn) || alignField.mouseReleased(mx, my, btn)
                    || removeButton.mouseReleased(mx, my, btn);
        }
    }
}
