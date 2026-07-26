package net.deepacat.deepamonu.hud;

import ch.njol.unofficialmonumentamod.AbilityHandler;
import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import net.deepacat.deepamonu.DMMClient;
import net.deepacat.deepamonu.config.ModConfig;
import net.minecraft.client.gui.GuiGraphics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AlchPotionDisplay {

    private static final Logger LOGGER = LoggerFactory.getLogger("CrosshairHud");
    private static final String ALCHEMIST_CLASS = "alchemist";

    private static boolean isAlchemist() {
        try {
            var handler = UnofficialMonumentaModClient.abilityHandler;
            if (handler == null || handler.abilityData.isEmpty()) return false;
            String className = handler.abilityData.get(0).className;
            return className != null && className.toLowerCase(Locale.ROOT).contains(ALCHEMIST_CLASS);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void tick() {
        ModConfig config = DMMClient.config();
        if (config == null) return;
        ModConfig.Crosshair.AlchPotions cfg = config.features.crosshair.alchPotions;
        if (!cfg.enabled) return;

        if (!isAlchemist()) return;

        if (DMMClient.features().enableDebug) {
            try {
                var handler = UnofficialMonumentaModClient.abilityHandler;
                if (handler != null && !handler.abilityData.isEmpty()) {
                    for (AbilityHandler.AbilityInfo info : handler.abilityData) {
                        if (info.maxCharges > 1 || (info.maxCharges == 1 && info.initialCooldown <= 0)) {
                            LOGGER.debug("[AlchPotions] {} ({}): Charges={}/{},",
                                    info.name, info.className,
                                    info.charges, info.maxCharges);
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }
    }

    public static void render(GuiGraphics graphics) {
        ModConfig config = DMMClient.config();
        if (config == null) return;
        ModConfig.Crosshair.AlchPotions cfg = config.features.crosshair.alchPotions;
        if (!cfg.enabled) return;

        if (!isAlchemist()) return;

        List<AbilityHandler.AbilityInfo> abilities;
        try {
            var handler = UnofficialMonumentaModClient.abilityHandler;
            if (handler == null || handler.abilityData.isEmpty()) return;
            abilities = new ArrayList<>(handler.abilityData);
        } catch (Throwable ignored) {
            return;
        }

        List<AbilityHandler.AbilityInfo> potionAbilities = new ArrayList<>();
        for (AbilityHandler.AbilityInfo info : abilities) {
            if (info.maxCharges > 1 || (info.maxCharges == 1 && info.initialCooldown <= 0)) {
                potionAbilities.add(info);
            }
        }

        if (potionAbilities.isEmpty()) return;

        if (cfg.display.potionDisplayMode == ModConfig.Crosshair.DisplayMode.ICONS) {
            float rowOffset = cfg.icons.vertical
                    ? (8 * cfg.layout.textScale + 2 * cfg.layout.textScale)
                    : (8 * cfg.layout.textScale + 2 * cfg.layout.textScale + 10);

            for (int i = 0; i < potionAbilities.size(); i++) {
                AbilityHandler.AbilityInfo info = potionAbilities.get(i);
                float yOff = cfg.layout.yOffset + i * rowOffset * (cfg.icons.vertical ? 1.5f : 1f);
                CrosshairTextRenderer.renderAmmoIcons(
                        graphics, info.charges, info.maxCharges,
                        cfg.layout.xOffset + (cfg.icons.vertical ? i * rowOffset : 0), yOff,
                        0, cfg.layout.textScale,
                        cfg.colors.potionFullColor, cfg.colors.potionMidColor, cfg.colors.potionEmptyColor, cfg.colors.potionEmptySlotColor,
                        cfg.icons.iconCharStyle,
                        cfg.icons.filledIconChar, cfg.icons.emptyIconChar,
                        false,
                        false, ModConfig.Crosshair.Alignment.CENTER,
                        cfg.icons.iconShadow
                );
            }
        } else {
            List<CrosshairTextRenderer.TextLine> lines = new ArrayList<>();
            for (AbilityHandler.AbilityInfo info : potionAbilities) {
                int color = getPotionColor(cfg.colors.potionFullColor, cfg.colors.potionMidColor, cfg.colors.potionEmptyColor, info.charges, info.maxCharges);
                color |= 0xFF000000;
                String text = info.maxCharges > 1
                        ? String.format("%d/%d", info.charges, info.maxCharges)
                        : String.valueOf(info.charges);
                lines.add(new CrosshairTextRenderer.TextLine(text, color));
            }

            CrosshairTextRenderer.renderLines(
                    graphics, lines,
                    cfg.layout.xOffset, cfg.layout.yOffset,
                    0, cfg.layout.textScale
            );
        }
    }

    private static int getPotionColor(int fullColor, int midColor, int emptyColor, int charges, int max) {
        if (charges == max || max <= 0) {
            return fullColor;
        }
        float fraction = (float) charges / max;
        if (fraction > 0.66f) {
            return fullColor;
        } else if (fraction > 0.33f) {
            return midColor;
        } else {
            return emptyColor;
        }
    }
}
