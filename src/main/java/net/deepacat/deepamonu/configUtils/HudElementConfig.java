package net.deepacat.deepamonu.configUtils;

import ch.njol.minecraft.uiframework.ElementPosition;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

/**
 * Shared layout/appearance settings for a HUD element.
 *
 * <p>The offsets are absolute pixel offsets from the screen anchor chosen by
 * {@code positioning.alignX}/{@code positioning.alignY}. Alignment values are 0..1:
 * <ul>
 *     <li>X: 0 = left, 0.5 = centre, 1 = right</li>
 *     <li>Y: 0 = top, 0.5 = middle, 1 = bottom</li>
 * </ul>
 */
public class HudElementConfig {
    @ConfigEntry.Gui.CollapsibleObject
    public Positioning positioning = new Positioning();

    @ConfigEntry.Gui.CollapsibleObject
    public TextSettings text = new TextSettings();

    @ConfigEntry.Gui.CollapsibleObject
    public BackgroundSettings background = new BackgroundSettings();

    public HudElementConfig() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private void ensureDefaults() {
        if (positioning == null) positioning = new Positioning();
        if (text == null) text = new TextSettings();
        if (background == null) background = new BackgroundSettings();
    }

    /**
     * Converts these settings to the position object used by the UMM HUD framework.
     */
    public ElementPosition toPosition() {
        ensureDefaults();
        float ax = clamp01(positioning.alignX);
        float ay = clamp01(positioning.alignY);
        return new ElementPosition(ax, positioning.xOffset, ay, positioning.yOffset, ax, ay);
    }

    /**
     * Keeps the values in sync after the element has been dragged in the HUD edit screen.
     */
    public void applyPosition(ElementPosition position) {
        if (position == null) return;
        ensureDefaults();
        positioning.alignX = clamp01(position.alignX);
        positioning.alignY = clamp01(position.alignY);
        positioning.xOffset = position.offsetXAbsolute;
        positioning.yOffset = position.offsetYAbsolute;
    }

    public int textArgb() {
        ensureDefaults();
        return text.textColor | 0xFF000000;
    }

    public boolean hasTextShadow() {
        ensureDefaults();
        return text.textShadow;
    }

    public boolean hasBackground() {
        ensureDefaults();
        return (background.bgColor >>> 24) != 0;
    }

    public int backgroundArgb() {
        ensureDefaults();
        return background.bgColor;
    }

    public static class Positioning {
        @ConfigEntry.Gui.Tooltip(count = 1)
        public int xOffset;

        @ConfigEntry.Gui.Tooltip(count = 1)
        public int yOffset;

        @ConfigEntry.Gui.Tooltip(count = 1)
        public float alignX = 0.5f;

        @ConfigEntry.Gui.Tooltip(count = 1)
        public float alignY = 0.5f;
    }

    public static class TextSettings {
        @ConfigEntry.Gui.Tooltip(count = 1)
        @ConfigEntry.ColorPicker(allowAlpha = false)
        public int textColor = 0xFFFFFF;

        @ConfigEntry.Gui.Tooltip(count = 1)
        public boolean textShadow = true;
    }

    public static class BackgroundSettings {
        @ConfigEntry.Gui.Tooltip(count = 1)
        @ConfigEntry.ColorPicker(allowAlpha = true)
        public int bgColor = 0x00000000;
    }

    public static class Builder {
        private final HudElementConfig config = new HudElementConfig();

        public Builder xOffset(int xOffset) {
            config.positioning.xOffset = xOffset;
            return this;
        }

        public Builder yOffset(int yOffset) {
            config.positioning.yOffset = yOffset;
            return this;
        }

        public Builder offset(int xOffset, int yOffset) {
            config.positioning.xOffset = xOffset;
            config.positioning.yOffset = yOffset;
            return this;
        }

        public Builder alignX(float alignX) {
            config.positioning.alignX = clamp01(alignX);
            return this;
        }

        public Builder alignY(float alignY) {
            config.positioning.alignY = clamp01(alignY);
            return this;
        }

        public Builder alignment(float alignX, float alignY) {
            config.positioning.alignX = clamp01(alignX);
            config.positioning.alignY = clamp01(alignY);
            return this;
        }

        public Builder textColor(int textColor) {
            config.text.textColor = textColor & 0xFFFFFF;
            return this;
        }

        public Builder textShadow(boolean textShadow) {
            config.text.textShadow = textShadow;
            return this;
        }

        public Builder bgColor(int bgColor) {
            config.background.bgColor = bgColor;
            return this;
        }

        public HudElementConfig build() {
            return config;
        }
    }
}
