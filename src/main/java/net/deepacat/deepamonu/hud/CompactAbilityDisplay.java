package net.deepacat.deepamonu.hud;

import ch.njol.unofficialmonumentamod.AbilityHandler;
import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import net.deepacat.deepamonu.DMMClient;
import net.deepacat.deepamonu.configUtils.HudElementConfig;
import net.deepacat.deepamonu.compat.umm.FormatParser;
import net.deepacat.deepamonu.configUtils.CompactAbilityEntry;
import net.deepacat.deepamonu.ModConfig;
import net.deepacat.deepamonu.hud.elements.DMMHudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CompactAbilityDisplay extends DMMHudElement {

    public static final CompactAbilityDisplay INSTANCE = new CompactAbilityDisplay();

    private static final Logger LOGGER = LoggerFactory.getLogger("CrosshairHud");
    private static List<FormatParser.AdvancedEntry> cachedAdvanced;
    private static String cachedJsonPath;

    private static ModConfig.Crosshair.CompactAbilities config() {
        ModConfig config = DMMClient.config();
        return config == null ? null : config.features.crosshair.compactAbilities;
    }

    public static void tick() {
        if (!DMMClient.features().enableDebug) return;
        ModConfig.Crosshair.CompactAbilities cfg = config();
        if (cfg == null || !cfg.enabled) return;

        try {
            var handler = UnofficialMonumentaModClient.abilityHandler;
            if (handler != null && !handler.abilityData.isEmpty()) {
                for (AbilityHandler.AbilityInfo info : handler.abilityData) {
                    LOGGER.debug("[CompactAbility] {} ({}): CD={}, Charges={}/{}",
                            info.name, info.className,
                            info.remainingCooldown, info.charges, info.maxCharges);
                }
            }
        } catch (Throwable ignored) {}
    }

    private static List<AbilityHandler.AbilityInfo> abilities() {
        try {
            var handler = UnofficialMonumentaModClient.abilityHandler;
            if (handler == null || handler.abilityData.isEmpty()) return List.of();
            return new ArrayList<>(handler.abilityData);
        } catch (Throwable ignored) {
            return List.of();
        }
    }

    @Override
    protected HudElementConfig hudConfig() {
        ModConfig.Crosshair.CompactAbilities cfg = config();
        return cfg == null ? null : cfg.hud;
    }

    @Override
    protected boolean isEnabled() {
        return true;
    }

    @Override
    protected boolean isVisible() {
        ModConfig.Crosshair.CompactAbilities cfg = config();
        return cfg != null && cfg.enabled && !abilities().isEmpty();
    }

    @Override
    protected int getWidth() {
        ModConfig.Crosshair.CompactAbilities cfg = config();
        if (cfg == null) return 0;
        float[] bounds = computeBounds(cfg);
        int width = (int) Math.ceil(bounds[2] - bounds[0]) + contentPadding() * 2;
        return Math.max(0, width);
    }

    @Override
    protected int getHeight() {
        ModConfig.Crosshair.CompactAbilities cfg = config();
        if (cfg == null) return 0;
        float[] bounds = computeBounds(cfg);
        int height = (int) Math.ceil(bounds[3] - bounds[1]) + contentPadding() * 2;
        return Math.max(0, height);
    }

    @Override
    public Rectangle getDimension() {
        ModConfig.Crosshair.CompactAbilities cfg = config();
        if (cfg == null) return new Rectangle(0, 0, 0, 0);
        float[] bounds = computeBounds(cfg);
        if (bounds[2] <= bounds[0] || bounds[3] <= bounds[1]) {
            return new Rectangle(0, 0, 0, 0);
        }

        HudElementConfig hud = cfg.hud;
        float ax = HudElementConfig.clamp01(hud.positioning.alignX);
        float ay = HudElementConfig.clamp01(hud.positioning.alignY);
        Minecraft mc = Minecraft.getInstance();
        int anchorX = Math.round(mc.getWindow().getGuiScaledWidth() * ax + hud.positioning.xOffset);
        int anchorY = Math.round(mc.getWindow().getGuiScaledHeight() * ay + hud.positioning.yOffset);
        int pad = contentPadding();
        int x = anchorX + (int) Math.floor(bounds[0]) - pad;
        int y = anchorY + (int) Math.floor(bounds[1]) - pad;
        int width = (int) Math.ceil(bounds[2] - bounds[0]) + pad * 2;
        int height = (int) Math.ceil(bounds[3] - bounds[1]) + pad * 2;
        return new Rectangle(x, y, width, height);
    }

    @Override
    protected void render(GuiGraphics graphics, float tickDelta) {
        ModConfig.Crosshair.CompactAbilities cfg = config();
        if (cfg == null) return;

        List<AbilityHandler.AbilityInfo> abilities = abilities();
        if (abilities.isEmpty()) return;

        float[] bounds = computeBounds(cfg);
        int pad = contentPadding();
        int minX = (int) Math.floor(bounds[0]);
        int minY = (int) Math.floor(bounds[1]);
        renderBackground(graphics, getWidth(), getHeight());

        for (AbilityHandler.AbilityInfo info : abilities) {
            for (CompactAbilityEntry entry : findEntries(cfg, info)) {
                List<CrosshairTextRenderer.TextLine> parsed = getDisplayText(cfg, entry, info);
                if (parsed.isEmpty()) continue;

                CrosshairTextRenderer.renderSegmentsAt(
                        graphics, parsed,
                        entry.xOffset - minX + pad,
                        entry.yOffset - minY + pad,
                        entry.textScale, entry.alignment, cfg.hud.hasTextShadow()
                );
            }
        }
    }

    private static float[] computeBounds(ModConfig.Crosshair.CompactAbilities cfg) {
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;

        for (AbilityHandler.AbilityInfo info : abilities()) {
            for (CompactAbilityEntry entry : findEntries(cfg, info)) {
                List<CrosshairTextRenderer.TextLine> parsed = getDisplayText(cfg, entry, info);
                if (parsed.isEmpty()) continue;
                float[] bounds = CrosshairTextRenderer.measureSegmentsRelative(
                        parsed, entry.xOffset, entry.yOffset, entry.textScale, entry.alignment);
                minX = Math.min(minX, bounds[0]);
                minY = Math.min(minY, bounds[1]);
                maxX = Math.max(maxX, bounds[2]);
                maxY = Math.max(maxY, bounds[3]);
            }
        }

        if (minX == Float.MAX_VALUE) {
            return new float[]{0, 0, 0, 0};
        }
        return new float[]{minX, minY, maxX, maxY};
    }

    private static List<CompactAbilityEntry> findEntries(ModConfig.Crosshair.CompactAbilities cfg, AbilityHandler.AbilityInfo info) {
        if (info == null || info.name == null) return List.of();
        if (cfg.advancedMode) {
            String jsonPath = cfg.advancedJsonPath;
            if (jsonPath != null && !jsonPath.isEmpty()) {
                if (cachedAdvanced == null || !jsonPath.equals(cachedJsonPath)) {
                    cachedAdvanced = FormatParser.loadAdvancedConfig(Path.of(jsonPath));
                    cachedJsonPath = jsonPath;
                }
                if (cachedAdvanced != null && !cachedAdvanced.isEmpty()) {
                    String display = FormatParser.evaluateAdvanced(cachedAdvanced, info);
                    if (display != null) {
                        return List.of(new CompactAbilityEntry(info.name, display));
                    }
                }
            }
        }
        String searchLower = info.name.toLowerCase(Locale.ROOT);
        List<CompactAbilityEntry> matches = new ArrayList<>();
        for (CompactAbilityEntry entry : cfg.trackedAbilities) {
            if (entry.abilityName != null && !entry.abilityName.isEmpty()
                    && entry.abilityName.toLowerCase(Locale.ROOT).equals(searchLower)) {
                matches.add(entry);
            }
        }
        return matches;
    }

    private static List<CrosshairTextRenderer.TextLine> getDisplayText(ModConfig.Crosshair.CompactAbilities cfg, CompactAbilityEntry entry, AbilityHandler.AbilityInfo info) {
        if (cfg.advancedMode && entry.abilityName != null && entry.abilityName.equals(info.name)) {
            return FormatParser.parseString(entry.formatString, info);
        }
        return FormatParser.parse(entry, info);
    }
}
