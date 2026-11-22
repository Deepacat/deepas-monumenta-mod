package net.deepacat.deepamonu.hud;

import ch.njol.minecraft.uiframework.ElementPosition;
import net.deepacat.deepamonu.DMMClient;
import net.deepacat.deepamonu.configUtils.HudElementConfig;
import net.deepacat.deepamonu.ModConfig;
import net.deepacat.deepamonu.hud.elements.DMMHudElement;
import net.minecraft.client.gui.GuiGraphics;

public class FireTimerElement extends DMMHudElement {
    public static final FireTimerElement INSTANCE = new FireTimerElement();

    private final ElementPosition position = new ElementPosition(0.5f, 0, 0.5f, 33, 0.5f, 0.5f);

    private static ModConfig.Crosshair.Multiload config() {
        ModConfig config = DMMClient.config();
        return config == null ? null : config.features.crosshair.multiload;
    }

    @Override
    protected HudElementConfig hudConfig() {
        return null;
    }

    @Override
    protected ElementPosition getPosition() {
        ModConfig.Crosshair.Multiload cfg = config();
        if (cfg != null) {
            position.offsetXRelative = 0.5f;
            position.offsetXAbsolute = cfg.fireTimer.bar.xOffset;
            position.offsetYRelative = 0.5f;
            position.offsetYAbsolute = cfg.fireTimer.bar.yOffset;
            position.alignX = 0.5f;
            position.alignY = 0.5f;
        }
        return position;
    }

    @Override
    public boolean shouldRenderOverUI() {
        ModConfig.Crosshair.Multiload cfg = config();
        return cfg != null && cfg.layout.renderOverUI;
    }

    @Override
    protected boolean isEnabled() {
        return true;
    }

    @Override
    protected boolean isVisible() {
        ModConfig.Crosshair.Multiload cfg = config();
        if (cfg == null || !cfg.enabled || !cfg.fireTimer.enabled || !MultiloadDisplay.hasAmmo()
                || MultiloadDisplay.lastShotTime() <= 0) {
            return false;
        }
        long durationMs = (long) (cfg.fireTimer.duration * 1000f);
        if (durationMs <= 0) return false;
        return System.currentTimeMillis() - MultiloadDisplay.lastShotTime() < durationMs;
    }

    @Override
    protected int getWidth() {
        ModConfig.Crosshair.Multiload cfg = config();
        return cfg == null ? 0 : Math.max(0, cfg.fireTimer.bar.width);
    }

    @Override
    protected int getHeight() {
        ModConfig.Crosshair.Multiload cfg = config();
        return cfg == null ? 0 : Math.max(0, cfg.fireTimer.bar.height);
    }

    @Override
    protected void render(GuiGraphics graphics, float tickDelta) {
        ModConfig.Crosshair.Multiload cfg = config();
        if (cfg == null) return;

        long elapsed = System.currentTimeMillis() - MultiloadDisplay.lastShotTime();
        long durationMs = (long) (cfg.fireTimer.duration * 1000f);
        int barWidth = cfg.fireTimer.bar.width;
        int barHeight = cfg.fireTimer.bar.height;
        if (durationMs <= 0 || barWidth <= 0 || barHeight <= 0) return;

        float progress = 1f - Math.min(1f, (float) elapsed / durationMs);
        if (progress <= 0) return;

        int borderColor = 0xFF000000;
        int barColor = cfg.fireTimer.barColors.barColor | 0xFF000000;
        int bgColor = cfg.fireTimer.barColors.bgColor | 0xFF000000;

        graphics.fill(-1, -1, barWidth + 1, barHeight + 1, borderColor);
        graphics.fill(0, 0, barWidth, barHeight, bgColor);

        if (cfg.fireTimer.vertical) {
            int filledHeight = Math.max(0, (int) (barHeight * progress));
            if (filledHeight > 0) {
                graphics.fill(0, barHeight - filledHeight, barWidth, barHeight, barColor);
            }
        } else {
            int filledWidth = Math.max(0, (int) (barWidth * progress));
            if (filledWidth > 0) {
                graphics.fill(0, 0, filledWidth, barHeight, barColor);
            }
        }
    }
}
