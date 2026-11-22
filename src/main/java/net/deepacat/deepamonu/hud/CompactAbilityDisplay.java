package net.deepacat.deepamonu.hud;

import ch.njol.unofficialmonumentamod.AbilityHandler;
import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import net.deepacat.deepamonu.DMMClient;
import net.deepacat.deepamonu.compat.FormatParser;
import net.deepacat.deepamonu.configUtils.CompactAbilityEntry;
import net.deepacat.deepamonu.ModConfig;
import net.minecraft.client.gui.GuiGraphics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CompactAbilityDisplay {

    private static final Logger LOGGER = LoggerFactory.getLogger("CrosshairHud");
    private static List<FormatParser.AdvancedEntry> cachedAdvanced;
    private static String cachedJsonPath;

    public static void tick() {
        if (!DMMClient.features().enableDebug) return;
        ModConfig config = DMMClient.config();
        if (config == null) return;
        ModConfig.Crosshair.CompactAbilities cfg = config.features.crosshair.compactAbilities;
        if (!cfg.enabled) return;

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

    public static void render(GuiGraphics graphics) {
        ModConfig config = DMMClient.config();
        if (config == null) return;
        ModConfig.Crosshair.CompactAbilities cfg = config.features.crosshair.compactAbilities;
        if (!cfg.enabled) return;

        List<AbilityHandler.AbilityInfo> abilities;
        try {
            var handler = UnofficialMonumentaModClient.abilityHandler;
            if (handler == null || handler.abilityData.isEmpty()) return;
            abilities = new ArrayList<>(handler.abilityData);
        } catch (Throwable ignored) {
            return;
        }

        for (AbilityHandler.AbilityInfo info : abilities) {
            List<CompactAbilityEntry> entries = findEntries(cfg, info);
            for (CompactAbilityEntry entry : entries) {
                List<CrosshairTextRenderer.TextLine> parsed = getDisplayText(cfg, entry, info);
                if (parsed.isEmpty()) continue;

                CrosshairTextRenderer.renderSingleLineAligned(
                        graphics, parsed,
                        entry.xOffset, entry.yOffset,
                        0, entry.textScale, entry.alignment
                );
            }
        }
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

    private static CompactAbilityEntry findEntry(ModConfig.Crosshair.CompactAbilities cfg, AbilityHandler.AbilityInfo info) {
        List<CompactAbilityEntry> entries = findEntries(cfg, info);
        return entries.isEmpty() ? null : entries.get(0);
    }

    private static List<CrosshairTextRenderer.TextLine> getDisplayText(ModConfig.Crosshair.CompactAbilities cfg, CompactAbilityEntry entry, AbilityHandler.AbilityInfo info) {
        if (cfg.advancedMode && entry.abilityName != null && entry.abilityName.equals(info.name)) {
            return FormatParser.parseString(entry.formatString, info);
        }
        return FormatParser.parse(entry, info);
    }
}
