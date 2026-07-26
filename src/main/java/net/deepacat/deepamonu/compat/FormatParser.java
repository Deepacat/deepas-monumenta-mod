package net.deepacat.deepamonu.compat;

import ch.njol.unofficialmonumentamod.AbilityHandler;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.deepacat.deepamonu.config.CompactAbilityEntry;
import net.deepacat.deepamonu.hud.CrosshairTextRenderer;
import net.minecraft.ChatFormatting;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FormatParser {

    private static final Gson GSON = new Gson();

    public static List<CrosshairTextRenderer.TextLine> parse(CompactAbilityEntry entry, AbilityHandler.AbilityInfo info) {
        if (info == null) return List.of();
        String format = entry.formatString != null ? entry.formatString : "%cds";
        String result = replacePlaceholders(format, info);
        result = convertAmpersandColors(result);
        if (result.isEmpty()) return List.of();

        int defaultColor = 0xFFFFFFFF;
        return parseColorSegments(result, defaultColor);
    }

    public static List<CrosshairTextRenderer.TextLine> parseString(String text, AbilityHandler.AbilityInfo info) {
        String result = replacePlaceholders(text, info);
        result = convertAmpersandColors(result);
        if (result.isEmpty()) return List.of();
        return parseColorSegments(result, 0xFFFFFFFF);
    }

    private static String convertAmpersandColors(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '&' && i + 1 < text.length()) {
                char code = text.charAt(i + 1);
                ChatFormatting fmt = ChatFormatting.getByCode(code);
                if (fmt != null) {
                    sb.append('\u00A7').append(code);
                    i += 2;
                    continue;
                }
            }
            sb.append(c);
            i++;
        }
        return sb.toString();
    }

    private static String replacePlaceholders(String text, AbilityHandler.AbilityInfo info) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '\\' && i + 1 < text.length() && text.charAt(i + 1) == '%') {
                sb.append('%');
                i += 2;
                continue;
            }
            if (c == '%' && i + 1 < text.length()) {
                String code = readPlaceholder(text, i);
                String replacement = resolvePlaceholder(code, info);
                if (replacement != null) {
                    sb.append(replacement);
                    i += code.length();
                    continue;
                }
            }
            sb.append(c);
            i++;
        }
        return sb.toString();
    }

    private static String readPlaceholder(String text, int start) {
        int end = start + 1;
        while (end < text.length()) {
            char c = text.charAt(end);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                end++;
            } else {
                break;
            }
        }
        return text.substring(start, end);
    }

    private static String resolvePlaceholder(String code, AbilityHandler.AbilityInfo info) {
        float cdSec = info.remainingCooldown / 20f;
        float initCdSec = info.initialCooldown / 20f;
        float durSec = info.remainingDuration / 20f;
        float initDurSec = info.initialDuration / 20f;
        int cdInt = (int) Math.ceil(cdSec);

        switch (code.toLowerCase(Locale.ROOT)) {
            case "%cd":   return String.valueOf(cdInt);
            case "%cds":  return cdInt + "s";
            case "%cdd":  return String.format("%.1f", cdSec);
            case "%cdds": return String.format("%.1fs", cdSec);
            case "%ch":   return info.charges + "/" + info.maxCharges;
            case "%chc":  return String.valueOf(info.charges);
            case "%cht":  return String.valueOf(info.maxCharges);
            case "%icd":  return String.valueOf((int) Math.ceil(initCdSec));
            case "%icds": return ((int) Math.ceil(initCdSec)) + "s";
            case "%id":   return String.format("%.1f", durSec);
            case "%ids":  return String.format("%.1fs", durSec);
            case "%idur": return String.valueOf((int) Math.ceil(initDurSec));
            case "%idus": return ((int) Math.ceil(initDurSec)) + "s";
            case "%n":    return info.name != null ? info.name : "";
            case "%cn":   return info.className != null ? info.className : "";
            case "%m":    return info.mode != null ? info.mode : "";
            default:      return null;
        }
    }

    public static List<CrosshairTextRenderer.TextLine> parseColorSegments(String text, int defaultColor) {
        List<CrosshairTextRenderer.TextLine> segments = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int currentColor = defaultColor;
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '\u00A7' && i + 1 < text.length()) {
                if (!current.isEmpty()) {
                    segments.add(new CrosshairTextRenderer.TextLine(current.toString(), currentColor));
                    current.setLength(0);
                }
                char code = text.charAt(i + 1);
                ChatFormatting formatting = ChatFormatting.getByCode(code);
                if (formatting != null) {
                    if (formatting == ChatFormatting.RESET) {
                        currentColor = defaultColor;
                    } else if (formatting.getColor() != null) {
                        currentColor = 0xFF000000 | formatting.getColor();
                    }
                }
                i += 2;
            } else {
                current.append(c);
                i++;
            }
        }
        if (!current.isEmpty()) {
            segments.add(new CrosshairTextRenderer.TextLine(current.toString(), currentColor));
        }
        return segments;
    }

    // ===== Advanced JSON mode =====

    public static class AdvancedEntry {
        public String ability;
        public List<String> conditions;
        public String display;
    }

    public static class AdvancedConfig {
        public List<AdvancedEntry> abilities;
    }

    public static List<AdvancedEntry> loadAdvancedConfig(Path path) {
        try {
            if (!Files.exists(path)) return List.of();
            try (Reader reader = Files.newBufferedReader(path)) {
                AdvancedConfig config = GSON.fromJson(reader, AdvancedConfig.class);
                return config != null && config.abilities != null ? config.abilities : List.of();
            }
        } catch (Exception e) {
            return List.of();
        }
    }

    public static String evaluateAdvanced(List<AdvancedEntry> advancedEntries, AbilityHandler.AbilityInfo info) {
        if (info == null || info.name == null) return null;
        String searchName = info.name.toLowerCase(Locale.ROOT);
        for (AdvancedEntry entry : advancedEntries) {
            if (entry.ability == null) continue;
            if (!entry.ability.toLowerCase(Locale.ROOT).equals(searchName)) continue;
            if (entry.conditions == null || entry.conditions.isEmpty()) {
                return entry.display != null ? entry.display : "";
            }
            boolean allMet = true;
            for (String cond : entry.conditions) {
                if (!evaluateCondition(cond, info)) {
                    allMet = false;
                    break;
                }
            }
            if (allMet) {
                return entry.display != null ? entry.display : "";
            }
        }
        return null;
    }

    private static boolean evaluateCondition(String condition, AbilityHandler.AbilityInfo info) {
        condition = condition.trim();
        String[] parts = condition.split("(?<=[><=!]=?)|(?=[><=!]=?)", 2);
        if (parts.length < 2) return false;
        String field = parts[0].trim().toLowerCase(Locale.ROOT);
        String rest = parts[1].trim();
        if (rest.isEmpty()) return false;

        String op;
        String valueStr;
        if (rest.startsWith(">=")) { op = ">="; valueStr = rest.substring(2).trim(); }
        else if (rest.startsWith("<=")) { op = "<="; valueStr = rest.substring(2).trim(); }
        else if (rest.startsWith("!=")) { op = "!="; valueStr = rest.substring(2).trim(); }
        else if (rest.startsWith("==")) { op = "=="; valueStr = rest.substring(2).trim(); }
        else if (rest.startsWith(">")) { op = ">"; valueStr = rest.substring(1).trim(); }
        else if (rest.startsWith("<")) { op = "<"; valueStr = rest.substring(1).trim(); }
        else return false;

        try {
            float fieldValue = getAbilityField(field, info);
            float compareValue = Float.parseFloat(valueStr);
            switch (op) {
                case ">":  return fieldValue > compareValue;
                case "<":  return fieldValue < compareValue;
                case ">=": return fieldValue >= compareValue;
                case "<=": return fieldValue <= compareValue;
                case "==": return fieldValue == compareValue;
                case "!=": return fieldValue != compareValue;
                default: return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static float getAbilityField(String field, AbilityHandler.AbilityInfo info) {
        switch (field) {
            case "charges":       return info.charges;
            case "maxcharges":    return info.maxCharges;
            case "remainingcooldown": return info.remainingCooldown;
            case "initialcooldown":  return info.initialCooldown;
            case "remainingduration": return info.remainingDuration;
            case "initialduration":  return info.initialDuration;
            case "offcooldownanimationticks": return info.offCooldownAnimationTicks;
            default: return 0;
        }
    }
}
