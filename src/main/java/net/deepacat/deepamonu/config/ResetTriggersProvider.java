package net.deepacat.deepamonu.config;

import me.shedaniel.autoconfig.gui.registry.api.GuiProvider;
import me.shedaniel.autoconfig.gui.registry.api.GuiRegistryAccess;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import net.deepacat.deepamonu.compat.TriggerCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ResetTriggersProvider implements GuiProvider {

    @SuppressWarnings({"rawtypes"})
    @Override
    public List<AbstractConfigListEntry> get(String i13n, Field field, Object config, Object defaults,
                                             GuiRegistryAccess registry) {
        List<AbstractConfigListEntry> entries = new ArrayList<>();
        entries.add(new ResetButtonEntry(
                Component.translatable("text.deepamonu.config.resetDetectedTriggers"),
                Component.translatable("text.deepamonu.config.resetDetectedTriggers.confirm"),
                Component.translatable("text.deepamonu.config.resetDetectedTriggers.tooltip")
        ));
        entries.add(new SpacerEntry(4));
        return entries;
    }

    // ----- Reset button entry -----
    private static class ResetButtonEntry extends AbstractConfigListEntry<Void> {
        private final Component normalText;
        private final Component confirmText;
        private final Component tooltipText;
        private long confirmationDeadline = 0;
        private int lastX, lastY, lastWidth;
        private static final long CONFIRM_MS = 3000;

        private static final ResourceLocation BUTTON_SPRITE = new ResourceLocation("widget/button");
        private static final ResourceLocation BUTTON_HOVERED_SPRITE = new ResourceLocation("widget/button_highlighted");

        public ResetButtonEntry(Component normal, Component confirm, Component tooltip) {
            super(normal, false);
            this.normalText = normal;
            this.confirmText = confirm;
            this.tooltipText = tooltip;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight,
                           int mouseX, int mouseY, boolean isHovered, float delta) {
            lastX = x;
            lastY = y;
            lastWidth = entryWidth;

            int btnWidth = 150;
            int btnHeight = 20;
            int btnX = x + entryWidth / 2 - btnWidth / 2;
            int btnY = y + 2;

            boolean hovered = mouseX >= btnX && mouseY >= btnY && mouseX < btnX + btnWidth && mouseY < btnY + btnHeight;
            boolean confirming = confirmationDeadline > System.currentTimeMillis();

            // Button background
            ResourceLocation sprite = hovered ? BUTTON_HOVERED_SPRITE : BUTTON_SPRITE;
            graphics.blitSprite(sprite, btnX, btnY, btnWidth, btnHeight);

            // Button text
            int textColor = confirming ? 0xFFFF5555 : 0xFFFFFF;
            Component label = confirming ? confirmText : normalText;
            graphics.drawCenteredString(Minecraft.getInstance().font, label,
                    btnX + btnWidth / 2, btnY + (btnHeight - 8) / 2, textColor);

            // Custom tooltip when hovering
            if (hovered && !confirming) {
                int tooltipWidth = Minecraft.getInstance().font.width(tooltipText) + 8;
                int tooltipHeight = 12;
                int tooltipX = btnX + btnWidth / 2 - tooltipWidth / 2;
                int tooltipY = btnY - tooltipHeight - 2;
                graphics.fill(tooltipX, tooltipY, tooltipX + tooltipWidth, tooltipY + tooltipHeight, 0xCC000000);
                graphics.drawCenteredString(Minecraft.getInstance().font, tooltipText,
                        tooltipX + tooltipWidth / 2, tooltipY + 2, 0xFFFFFFFF);
            }
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of();
        }

        @Override
        public @NotNull List<? extends GuiEventListener> children() {
            return List.of();
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0) return false;
            int btnWidth = 150;
            int btnHeight = 20;
            int btnX = lastX + lastWidth / 2 - btnWidth / 2;
            int btnY = lastY + 2;
            if (mouseX >= btnX && mouseY >= btnY && mouseX < btnX + btnWidth && mouseY < btnY + btnHeight) {
                long now = System.currentTimeMillis();
                if (confirmationDeadline > now) {
                    TriggerCache.clearAllAutoTriggers();
                    confirmationDeadline = 0;
                } else {
                    confirmationDeadline = now + CONFIRM_MS;
                }
                return true;
            }
            return false;
        }

        @Override public Void getValue() { return null; }
        @Override public void save() {}
        @Override public Optional<Component> getError() { return Optional.empty(); }

        @Override
        public Optional<Void> getDefaultValue() {
            return Optional.empty();
        }

        @Override public int getItemHeight() { return 24; }
    }

    // ----- Simple spacer to avoid clipping -----
    private static class SpacerEntry extends AbstractConfigListEntry<Void> {
        private final int height;

        public SpacerEntry(int height) {
            super(Component.empty(), false);
            this.height = height;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight,
                           int mouseX, int mouseY, boolean isHovered, float delta) { /* nothing */ }

        @Override public Void getValue() { return null; }
        @Override public void save() {}
        @Override public Optional<Component> getError() { return Optional.empty(); }

        @Override
        public Optional<Void> getDefaultValue() {
            return Optional.empty();
        }

        @Override public int getItemHeight() { return height; }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of();
        }

        @Override
        public @NotNull List<? extends GuiEventListener> children() {
            return List.of();
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) { return false; }
    }
}