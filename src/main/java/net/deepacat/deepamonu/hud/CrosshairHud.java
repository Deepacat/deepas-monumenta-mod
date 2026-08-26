package net.deepacat.deepamonu.hud;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.deepacat.deepamonu.DMMClient;
import net.deepacat.deepamonu.config.ModConfig;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.GuiGraphics;

public class CrosshairHud {

    private static final Logger LOGGER = LoggerFactory.getLogger("CrosshairHud");

    public static void init() {
        HudRenderCallback.EVENT.register(CrosshairHud::onHudRender);
        LOGGER.debug("[CrosshairHud] initialized HudRenderCallback");
    }

    private static void onHudRender(GuiGraphics graphics, float tickDelta) {
        ModConfig config = DMMClient.config();
        if (config == null) return;

        if (config.features.crosshair.compactAbilities.enabled) {
            CompactAbilityDisplay.render(graphics);
        }
        if (config.features.crosshair.alchPotions.enabled && !config.features.crosshair.alchPotions.layout.renderOverUI) {
            AlchPotionDisplay.render(graphics);
        }
        if (config.features.crosshair.multiload.enabled && !config.features.crosshair.multiload.layout.renderOverUI) {
            MultiloadDisplay.render(graphics);
        }
    }

    public static void onRenderOverUI(GuiGraphics graphics, float tickDelta) {
        ModConfig config = DMMClient.config();
        if (config == null) return;

        if (config.features.crosshair.alchPotions.enabled && config.features.crosshair.alchPotions.layout.renderOverUI) {
            AlchPotionDisplay.render(graphics);
        }
        if (config.features.crosshair.multiload.enabled && config.features.crosshair.multiload.layout.renderOverUI) {
            MultiloadDisplay.render(graphics);
        }
    }

    public static void tick() {
        CompactAbilityDisplay.tick();
        AlchPotionDisplay.tick();
        MultiloadDisplay.tick();
    }
}
