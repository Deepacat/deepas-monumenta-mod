package net.deepacat.deepamonu.compat.clothconfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.IntConsumer;

public class ColorPickerScreen extends Screen {
    private static final int PANEL_WIDTH = 190;
    private static final int SQUARE_SIZE = 100;
    private static final int SLIDER_HEIGHT = 14;

    private final Screen parent;
    private final boolean alphaAllowed;
    private final IntConsumer saveConsumer;
    private final int anchorX;
    private final int anchorY;

    private float hue;
    private float saturation;
    private float brightness;
    private int alpha = 255;
    private boolean updatingHex;

    private int panelX;
    private int panelY;
    private int panelHeight;
    private EditBox hexField;
    private SaturationValueBox saturationValueBox;
    private HueSlider hueSlider;
    private AlphaSlider alphaSlider;

    private ColorPickerScreen(Screen parent, int initialColor, boolean alphaAllowed, IntConsumer saveConsumer,
                              int anchorX, int anchorY) {
        super(Component.translatable("config.deepamonu.colorPicker.title"));
        this.parent = parent;
        this.alphaAllowed = alphaAllowed;
        this.saveConsumer = saveConsumer;
        this.anchorX = anchorX;
        this.anchorY = anchorY;

        this.alpha = alphaAllowed ? (initialColor >>> 24) & 0xFF : 255;
        setRgb(initialColor & 0xFFFFFF);
    }

    public static void open(Screen parent, int color, boolean alphaAllowed, IntConsumer saveConsumer,
                            int anchorX, int anchorY) {
        Minecraft minecraft = Minecraft.getInstance();
        Screen actualParent = parent == null ? minecraft.screen : parent;
        minecraft.setScreen(new ColorPickerScreen(actualParent, color, alphaAllowed, saveConsumer, anchorX, anchorY));
    }

    @Override
    protected void init() {
        this.panelHeight = alphaAllowed ? 216 : 196;
        int margin = 8;
        this.panelX = this.anchorX - PANEL_WIDTH - margin;
        if (this.panelX < margin) {
            this.panelX = this.anchorX + 24;
        }
        this.panelX = Mth.clamp(this.panelX, margin, Math.max(margin, this.width - PANEL_WIDTH - margin));

        this.panelY = this.anchorY - 4;
        this.panelY = Mth.clamp(this.panelY, margin, Math.max(margin, this.height - this.panelHeight - margin));

        int contentX = this.panelX + 15;
        int contentWidth = PANEL_WIDTH - 30;
        int squareX = this.panelX + (PANEL_WIDTH - SQUARE_SIZE) / 2;
        int squareY = this.panelY + 22;

        this.saturationValueBox = new SaturationValueBox(squareX, squareY);
        this.addRenderableWidget(this.saturationValueBox);

        int sliderY = squareY + SQUARE_SIZE + 6;
        this.hueSlider = new HueSlider(contentX, sliderY, contentWidth, SLIDER_HEIGHT);
        this.addRenderableWidget(this.hueSlider);

        int nextY = sliderY + SLIDER_HEIGHT + 6;
        if (alphaAllowed) {
            this.alphaSlider = new AlphaSlider(contentX, nextY, contentWidth, SLIDER_HEIGHT, this.alpha / 255.0);
            this.addRenderableWidget(this.alphaSlider);
            nextY += SLIDER_HEIGHT + 6;
        }

        int hexWidth = contentWidth - 44;
        this.hexField = new EditBox(this.font, contentX, nextY, hexWidth, 16, Component.empty());
        this.hexField.setMaxLength(alphaAllowed ? 9 : 7);
        this.hexField.setFilter(text -> text.matches(alphaAllowed
                ? "[#]?[0-9a-fA-F]{0,8}"
                : "[#]?[0-9a-fA-F]{0,6}"));
        this.hexField.setValue(hexString());
        this.hexField.setResponder(this::onHexChanged);
        this.addRenderableWidget(this.hexField);

        int buttonY = nextY + 22;
        this.addRenderableWidget(Button.builder(
                        Component.translatable("config.deepamonu.colorPicker.save"), button -> save())
                .bounds(this.panelX + (PANEL_WIDTH - 70) / 2, buttonY, 70, 18)
                .build());
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Background of the color picker panel.
        graphics.fill(this.panelX, this.panelY, this.panelX + PANEL_WIDTH, this.panelY + this.panelHeight, 0xFF000000);
        // Outline around the color picker panel.
        drawOutline(graphics, this.panelX, this.panelY, PANEL_WIDTH, this.panelHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (this.parent != null) {
            this.parent.render(graphics, -10000, -10000, partialTick);
        }

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 400);
        super.render(graphics, mouseX, mouseY, partialTick);

        int previewX = this.hexField.getX() + this.hexField.getWidth() + 8;
        int previewY = this.hexField.getY();
        int previewWidth = this.panelX + PANEL_WIDTH - 15 - previewX;
        drawCheckerboard(graphics, previewX, previewY, previewWidth, 16);
        graphics.fill(previewX, previewY, previewX + previewWidth, previewY + 16, argb());
        // Outline around the color preview rectangle.
        drawOutline(graphics, previewX, previewY, previewWidth, 16);
        graphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX < this.panelX || mouseX >= this.panelX + PANEL_WIDTH
                || mouseY < this.panelY || mouseY >= this.panelY + this.panelHeight) {
            closeWithoutSaving();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        closeWithoutSaving();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void closeWithoutSaving() {
        Minecraft minecraft = Minecraft.getInstance();
        Screen current = minecraft.screen;
        if (current != null) {
            current.removed();
        }
        minecraft.screen = this.parent;
        if (this.parent != null) {
            this.parent.setDragging(false);
            this.parent.setFocused(null);
            this.parent.added();
        }
    }

    private void save() {
        this.saveConsumer.accept(this.alphaAllowed ? argb() : rgb());
        closeWithoutSaving();
    }

    private void onHexChanged(String text) {
        if (this.updatingHex) return;
        if (text == null) return;

        String clean = text.startsWith("#") ? text.substring(1) : text;
        if (clean.isEmpty()) return;

        try {
            if (this.alphaAllowed) {
                if (clean.length() == 8) {
                    int value = (int) Long.parseLong(clean, 16);
                    this.alpha = (value >>> 24) & 0xFF;
                    setRgb(value & 0xFFFFFF);
                    syncControls();
                }
            } else if (clean.length() == 6) {
                setRgb((int) Long.parseLong(clean, 16));
                syncControls();
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private void syncControls() {
        if (this.hueSlider != null) this.hueSlider.setSliderValue(this.hue);
        if (this.alphaSlider != null) this.alphaSlider.setSliderValue(this.alpha / 255.0);
        updateHexField();
    }

    private void updateHexField() {
        if (this.hexField == null) return;
        this.updatingHex = true;
        this.hexField.setValue(hexString());
        this.updatingHex = false;
    }

    private void onHueChanged(float hue) {
        this.hue = Mth.clamp(hue, 0.0F, 1.0F);
        updateHexField();
    }

    private void onAlphaChanged(int alpha) {
        this.alpha = Mth.clamp(alpha, 0, 255);
        updateHexField();
    }

    private void onSaturationValueChanged(float saturation, float brightness) {
        this.saturation = Mth.clamp(saturation, 0.0F, 1.0F);
        this.brightness = Mth.clamp(brightness, 0.0F, 1.0F);
        updateHexField();
    }

    private void setRgb(int rgb) {
        float red = ((rgb >> 16) & 0xFF) / 255.0F;
        float green = ((rgb >> 8) & 0xFF) / 255.0F;
        float blue = (rgb & 0xFF) / 255.0F;

        float max = Math.max(red, Math.max(green, blue));
        float min = Math.min(red, Math.min(green, blue));
        float delta = max - min;

        this.brightness = max;
        this.saturation = max == 0.0F ? 0.0F : delta / max;
        this.hue = delta == 0.0F ? 0.0F : hueFromRgb(red, green, blue, max, delta);
    }

    private static float hueFromRgb(float red, float green, float blue, float max, float delta) {
        float hue;
        if (red == max) {
            hue = (green - blue) / delta;
        } else if (green == max) {
            hue = 2.0F + (blue - red) / delta;
        } else {
            hue = 4.0F + (red - green) / delta;
        }
        hue /= 6.0F;
        if (hue < 0.0F) hue += 1.0F;
        return hue;
    }

    private int rgb() {
        return hsvToRgb(this.hue, this.saturation, this.brightness);
    }

    private int argb() {
        return (this.alpha << 24) | rgb();
    }

    private String hexString() {
        return this.alphaAllowed
                ? String.format("#%08X", argb())
                : String.format("#%06X", rgb());
    }

    private static int hsvToRgb(float hue, float saturation, float brightness) {
        int segment = (int) (hue * 6.0F);
        float fraction = hue * 6.0F - segment;
        int p = (int) (brightness * (1.0F - saturation) * 255.0F);
        int q = (int) (brightness * (1.0F - fraction * saturation) * 255.0F);
        int t = (int) (brightness * (1.0F - (1.0F - fraction) * saturation) * 255.0F);
        int v = (int) (brightness * 255.0F);

        int red;
        int green;
        int blue;
        switch (segment % 6) {
            case 0 -> { red = v; green = t; blue = p; }
            case 1 -> { red = q; green = v; blue = p; }
            case 2 -> { red = p; green = v; blue = t; }
            case 3 -> { red = p; green = q; blue = v; }
            case 4 -> { red = t; green = p; blue = v; }
            default -> { red = v; green = p; blue = q; }
        }
        return (red << 16) | (green << 8) | blue;
    }

    private static void drawOutline(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.renderOutline(x, y, width, height, 0xFFA0A0A0);
    }

    private static void drawCheckerboard(GuiGraphics graphics, int x, int y, int width, int height) {
        int cell = 1;
        for (int candidate = Math.min(6, height); candidate >= 2; candidate--) {
            if (height % candidate == 0) {
                cell = candidate;
                break;
            }
        }

        for (int yy = 0; yy < height; yy += cell) {
            for (int xx = 0; xx < width; xx += cell) {
                int color = ((xx / cell) + (yy / cell)) % 2 == 0 ? 0xFF888888 : 0xFF555555;
                graphics.fill(x + xx, y + yy, Math.min(x + xx + cell, x + width), Math.min(y + yy + cell, y + height), color);
            }
        }
    }

    private class SaturationValueBox extends AbstractWidget {
        private SaturationValueBox(int x, int y) {
            super(x, y, SQUARE_SIZE, SQUARE_SIZE, Component.empty());
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int x = getX();
            int y = getY();
            int size = getWidth();
            int hueColor = 0xFF000000 | hsvToRgb(hue, 1.0F, 1.0F);

            for (int i = 0; i < size; i++) {
                int color = lerpColor(0xFFFFFFFF, hueColor, i / (float) (size - 1));
                graphics.fill(x + i, y, x + i + 1, y + size, color);
            }
            graphics.fillGradient(x, y, x + size, y + size, 0x00000000, 0xFF000000);

            // Outline around the main color picker square.
            drawOutline(graphics, x, y, size, size);

            // Square picker handle, drawn over the outline.
            int handleX = x + (int) (saturation * (size - 1));
            int handleY = y + (int) ((1.0F - brightness) * (size - 1));
            graphics.fill(handleX - 2, handleY - 2, handleX + 3, handleY + 3, 0xFF000000);
            graphics.fill(handleX - 1, handleY - 1, handleX + 2, handleY + 2, 0xFFFFFFFF);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            updateFromMouse(mouseX, mouseY);
        }

        @Override
        protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
            updateFromMouse(mouseX, mouseY);
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            if (button == 0) {
                updateFromMouse(mouseX, mouseY);
                return true;
            }
            return false;
        }

        private void updateFromMouse(double mouseX, double mouseY) {
            float newSaturation = (float) ((mouseX - getX()) / (getWidth() - 1.0));
            float newBrightness = 1.0F - (float) ((mouseY - getY()) / (getHeight() - 1.0));
            onSaturationValueChanged(newSaturation, newBrightness);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        }
    }

    private class HueSlider extends AbstractSliderButton {
        private HueSlider(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(), hue);
        }

        private void setSliderValue(float value) {
            this.value = Mth.clamp(value, 0.0F, 1.0F);
        }

        @Override
        protected void updateMessage() {
        }

        @Override
        protected void applyValue() {
            onHueChanged((float) this.value);
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int x = getX();
            int y = getY();
            int width = getWidth();
            int height = getHeight();

            for (int i = 0; i < width; i++) {
                int color = 0xFF000000 | hsvToRgb(i / (float) (width - 1), 1.0F, 1.0F);
                graphics.fill(x + i, y, x + i + 1, y + height, color);
            }

            // Outline around the hue slider.
            drawOutline(graphics, x, y, width, height);

            // Hue slider handle, drawn over the outline.
            int handleX = x + (int) (this.value * (width - 1));
            graphics.fill(handleX - 1, y - 1, handleX + 2, y + height + 1, 0xFF000000);
            graphics.fill(handleX, y, handleX + 1, y + height, 0xFFFFFFFF);
        }
    }

    private class AlphaSlider extends AbstractSliderButton {
        private AlphaSlider(int x, int y, int width, int height, double initialValue) {
            super(x, y, width, height, Component.empty(), initialValue);
        }

        private void setSliderValue(double value) {
            this.value = Mth.clamp(value, 0.0, 1.0);
        }

        @Override
        protected void updateMessage() {
        }

        @Override
        protected void applyValue() {
            onAlphaChanged((int) Math.round(this.value * 255.0));
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int x = getX();
            int y = getY();
            int width = getWidth();
            int height = getHeight();

            drawCheckerboard(graphics, x, y, width, height);
            int color = rgb();
            for (int i = 0; i < width; i++) {
                int a = (int) (i / (float) (width - 1) * 255.0F);
                graphics.fill(x + i, y, x + i + 1, y + height, (a << 24) | color);
            }

            // Outline around the alpha slider.
            drawOutline(graphics, x, y, width, height);

            // Alpha slider handle, drawn over the outline.
            int handleX = x + (int) (this.value * (width - 1));
            graphics.fill(handleX - 1, y - 1, handleX + 2, y + height + 1, 0xFF000000);
            graphics.fill(handleX, y, handleX + 1, y + height, 0xFFFFFFFF);
        }
    }

    private static int lerpColor(int start, int end, float delta) {
        int startA = (start >>> 24) & 0xFF;
        int startR = (start >> 16) & 0xFF;
        int startG = (start >> 8) & 0xFF;
        int startB = start & 0xFF;
        int endA = (end >>> 24) & 0xFF;
        int endR = (end >> 16) & 0xFF;
        int endG = (end >> 8) & 0xFF;
        int endB = end & 0xFF;

        int a = (int) (startA + (endA - startA) * delta);
        int r = (int) (startR + (endR - startR) * delta);
        int g = (int) (startG + (endG - startG) * delta);
        int b = (int) (startB + (endB - startB) * delta);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
