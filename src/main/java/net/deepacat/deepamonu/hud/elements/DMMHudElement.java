package net.deepacat.deepamonu.hud.elements;

import ch.njol.minecraft.uiframework.ElementPosition;
import ch.njol.minecraft.uiframework.hud.HudElement;
import net.deepacat.deepamonu.DMMClient;
import net.deepacat.deepamonu.configUtils.HudElementConfig;
import net.deepacat.deepamonu.hud.CrosshairTextRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Base class for all Deepa's Monumenta Mod HUD elements.
 *
 * <p>Positions and colours come from a shared {@link HudElementConfig}, so new
 * widgets only need to expose one of these objects instead of duplicating
 * offset/alignment/colour fields.
 */
public abstract class DMMHudElement extends HudElement {
    protected static final int PADDING = 3;

    private final ElementPosition position = new ElementPosition();

    /**
     * @return the config that positions and colours this element, or {@code null}
     * if the element manages its position itself.
     */
    protected abstract HudElementConfig hudConfig();

    /**
     * Elements rendered after chat/UI should return {@code true}.
     */
    public boolean shouldRenderOverUI() {
        return false;
    }

    @Override
    protected ElementPosition getPosition() {
        HudElementConfig config = hudConfig();
        if (config != null && config.positioning != null) {
            float ax = HudElementConfig.clamp01(config.positioning.alignX);
            float ay = HudElementConfig.clamp01(config.positioning.alignY);
            position.offsetXRelative = ax;
            position.offsetXAbsolute = config.positioning.xOffset;
            position.offsetYRelative = ay;
            position.offsetYAbsolute = config.positioning.yOffset;
            position.alignX = ax;
            position.alignY = ay;
        }
        return position;
    }

    @Override
    protected int getZOffset() {
        return 0;
    }

    protected int contentPadding() {
        HudElementConfig config = hudConfig();
        return config != null && config.background != null && config.hasBackground() ? PADDING : 0;
    }

    protected int textColor() {
        HudElementConfig config = hudConfig();
        return config != null && config.text != null ? config.textArgb() : 0xFFFFFFFF;
    }

    protected boolean textShadow() {
        HudElementConfig config = hudConfig();
        return config == null || config.text == null || config.hasTextShadow();
    }

    protected void drawText(GuiGraphics graphics, String text, float x, float y, float scale) {
        CrosshairTextRenderer.drawScaledText(graphics, Minecraft.getInstance().font, text, x, y,
                textColor(), scale, textShadow());
    }

    protected void renderBackground(GuiGraphics graphics, int width, int height) {
        HudElementConfig config = hudConfig();
        if (config != null && config.background != null && config.hasBackground()) {
            graphics.fill(0, 0, width, height, config.backgroundArgb());
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        boolean dragged = super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        if (dragged) {
            HudElementConfig config = hudConfig();
            if (config != null) {
                config.applyPosition(position);
            }
        }
        return dragged;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean released = super.mouseReleased(mouseX, mouseY, button);
        if (released && DMMClient.CONFIG != null) {
            DMMClient.CONFIG.save();
        }
        return released;
    }
}
