package net.deepacat.deepamonu.hud.elements;

import net.deepacat.deepamonu.DMMClient;
import net.deepacat.deepamonu.configUtils.HudElementConfig;
import net.deepacat.deepamonu.ModConfig;
import net.deepacat.deepamonu.features.ShieldGuardHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Locale;

public class GuardSituText extends DMMHudElement {
    public static final GuardSituText INSTANCE = new GuardSituText();

    private static ModConfig.Features.ShieldHelpers config() {
        ModConfig config = DMMClient.config();
        return config == null ? null : config.features.shieldHelpers;
    }

    private static ModConfig.Features.ShieldHelpers.GuardTimerDisplaySettings display() {
        ModConfig.Features.ShieldHelpers config = config();
        return config == null ? null : config.guardTimerDisplaySettings;
    }

    @Override
    protected HudElementConfig hudConfig() {
        ModConfig.Features.ShieldHelpers.GuardTimerDisplaySettings display = display();
        return display == null ? null : display.hud;
    }

    @Override
    protected boolean isEnabled() {
        return true;
    }

    @Override
    protected boolean isVisible() {
        ModConfig.Features.ShieldHelpers config = config();
        if (config == null || !config.enableGuardTimer) return false;

        // Once guard is activated, keep the timer visible until it reaches 0,
        // even if the Guard-enchanted item is no longer equipped.
        if (ShieldGuardHelper.guardTicks() > 0) return true;

        // While idle, the always-show option only applies when Guard gear is equipped.
        if (!config.guardAlwaysShow) return false;
        return ShieldGuardHelper.hasGuardEnchant(Minecraft.getInstance().player);
    }

    private static String buildText() {
        ModConfig.Features.ShieldHelpers.GuardTimerDisplaySettings display = display();
        String template = display != null ? display.displayText : "Guard: %guardtime%";
        double seconds = (double) ShieldGuardHelper.guardTicks() / 20.0;
        return template.replace("%guardtime%", String.format(Locale.ROOT, "%.1f", seconds));
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
