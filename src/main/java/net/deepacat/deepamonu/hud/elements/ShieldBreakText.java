package net.deepacat.deepamonu.hud.elements;

import net.deepacat.deepamonu.DMMClient;
import net.deepacat.deepamonu.configUtils.HudElementConfig;
import net.deepacat.deepamonu.ModConfig;
import net.deepacat.deepamonu.features.ShieldGuardHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Locale;

public class ShieldBreakText extends DMMHudElement {
    public static final ShieldBreakText INSTANCE = new ShieldBreakText();

    private static ModConfig.Features.ShieldHelpers config() {
        ModConfig config = DMMClient.config();
        return config == null ? null : config.features.shieldHelpers;
    }

    private static ModConfig.Features.ShieldHelpers.ShieldStunTimerDisplaySettings display() {
        ModConfig.Features.ShieldHelpers config = config();
        return config == null ? null : config.shieldStunTimerDisplaySettings;
    }

    @Override
    protected HudElementConfig hudConfig() {
        ModConfig.Features.ShieldHelpers.ShieldStunTimerDisplaySettings display = display();
        return display == null ? null : display.hud;
    }

    @Override
    protected boolean isEnabled() {
        return true;
    }

    @Override
    protected boolean isVisible() {
        ModConfig.Features.ShieldHelpers config = config();
        if (config == null || !config.enableShieldStunTimer) return false;
        return config.shieldStunAlwaysShow || ShieldGuardHelper.shieldBrokenTicks > 0;
    }

    private static String buildText() {
        ModConfig.Features.ShieldHelpers.ShieldStunTimerDisplaySettings display = display();
        String template = display != null ? display.displayText : "Break: %breaktime%";
        double seconds = (double) ShieldGuardHelper.shieldBrokenTicks / 20.0;
        return template.replace("%breaktime%", String.format(Locale.ROOT, "%.1f", seconds));
    }

    @Override
    protected int getWidth() {
        return Minecraft.getInstance().font.width(buildText()) + contentPadding() * 2;
    }

    @Override
    protected int getHeight() {
        return Minecraft.getInstance().font.lineHeight + contentPadding() * 2;
    }

    @Override
    protected void render(GuiGraphics graphics, float tickDelta) {
        int width = getWidth();
        int height = getHeight();
        int pad = contentPadding();
        renderBackground(graphics, width, height);
        drawText(graphics, buildText(), pad, pad, 1.0f);
    }
}
