package net.deepacat.deepamonu.utils;

import net.deepacat.deepamonu.DMMClient;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads Monumenta enchantment data from every equipped item slot, including
 * both hands and all armour slots.
 *
 * Recreated from MMA due to issues with not detecting all equipped pieces
 */
public final class NBTUtils {
    private static final String MONUMENTA_TAG = "Monumenta";
    private static final String STOCK_TAG = "Stock";
    private static final String PLAYER_MODIFIED_TAG = "PlayerModified";
    private static final String ENCHANTMENTS_TAG = "Enchantments";
    private static final String LEVEL_TAG = "Level";
    private static final String DISPLAY_TAG = "display";
    private static final String PLAIN_TAG = "plain";
    private static final String LORE_TAG = "Lore";

    private static final Pattern ENCHANT_LINE =
            Pattern.compile("^([A-Za-z][A-Za-z' -]*?)\\s+(\\d+|[IVXLCDM]+)$");

    private NBTUtils() {
    }

    public static Map<String, Integer> getAllEnchants(Player player) {
        Map<String, Integer> result = new HashMap<>();
        if (player == null) {
            DMMClient.LOGGER.info("[DEBUG] NBTUtils.getAllEnchants player=null");
            return result;
        }

        DMMClient.LOGGER.info("[DEBUG] NBTUtils.getAllEnchants player={} slots={}",
                player.getScoreboardName(), EquipmentSlot.values().length);

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = player.getItemBySlot(slot);
            Map<String, Integer> enchantments = getEnchants(stack);
            logSlot(slot, stack, enchantments);
            enchantments.forEach((name, level) -> result.merge(name, level, Integer::sum));
        }

        DMMClient.LOGGER.info("[DEBUG] NBTUtils.getAllEnchants result={}", result);
        return result;
    }

    private static void logSlot(EquipmentSlot slot, ItemStack stack, Map<String, Integer> parsed) {
        if (stack == null || stack.isEmpty()) {
            DMMClient.LOGGER.info("[DEBUG] NBTUtils slot={} empty=true", slot.getName());
            return;
        }

        CompoundTag root = stack.getTag();
        CompoundTag monumenta = root == null ? null : root.getCompound(MONUMENTA_TAG);
        CompoundTag stock = monumenta == null
                ? new CompoundTag()
                : monumenta.getCompound(STOCK_TAG).getCompound(ENCHANTMENTS_TAG);
        CompoundTag modified = monumenta == null
                ? new CompoundTag()
                : monumenta.getCompound(PLAYER_MODIFIED_TAG).getCompound(ENCHANTMENTS_TAG);
        Map<String, Integer> loreParsed = new HashMap<>();
        if (root != null) {
            CompoundTag display = root.getCompound(DISPLAY_TAG);
            collectLoreEnchants(display.getList(LORE_TAG, 8), true, loreParsed);
        }

        DMMClient.LOGGER.info(
                "[DEBUG] NBTUtils slot={} item='{}' id={} empty=false hasTag={} rootKeys={} hasMonumenta={} stockEnch={} modifiedEnch={} loreParsed={} parsed={}",
                slot.getName(),
                stack.getHoverName().getString(),
                stack.getItem(),
                root != null,
                root == null ? "[]" : root.getAllKeys(),
                monumenta != null && !monumenta.isEmpty(),
                stock,
                modified,
                loreParsed,
                parsed);
    }

    public static Map<String, Integer> getEnchants(ItemStack stack) {
        Map<String, Integer> result = new HashMap<>();
        if (stack == null || stack.isEmpty()) return result;

        CompoundTag root = stack.getTag();
        if (root == null) return result;

        CompoundTag monumenta = root.getCompound(MONUMENTA_TAG);

        // Plain lore is a generated fallback. The visible display lore is
        // applied later so it takes priority for enchantment levels.
        CompoundTag plainDisplay = root.getCompound(PLAIN_TAG).getCompound(DISPLAY_TAG);
        collectLoreEnchants(plainDisplay.getList(LORE_TAG, 8), false, result);

        if (!monumenta.isEmpty()) {
            collectEnchants(monumenta.getCompound(STOCK_TAG).getCompound(ENCHANTMENTS_TAG), result);
            collectEnchants(monumenta.getCompound(PLAYER_MODIFIED_TAG).getCompound(ENCHANTMENTS_TAG), result);
        }

        CompoundTag display = root.getCompound(DISPLAY_TAG);
        collectLoreEnchants(display.getList(LORE_TAG, 8), true, result);

        return result;
    }

    private static void collectLoreEnchants(ListTag lore, boolean json, Map<String, Integer> result) {
        if (lore == null || lore.isEmpty()) return;

        for (int i = 0; i < lore.size(); i++) {
            String line = lore.getString(i);
            if (json) {
                line = toPlainText(line);
            }
            parseEnchantLine(line, result);
        }
    }

    private static String toPlainText(String line) {
        try {
            Component component = Component.Serializer.fromJson(line);
            return component == null ? line : component.getString();
        } catch (Exception ignored) {
            return line;
        }
    }

    private static void parseEnchantLine(String line, Map<String, Integer> result) {
        if (line == null || line.isEmpty()) return;

        String plain = ChatFormatting.stripFormatting(line).trim();
        Matcher matcher = ENCHANT_LINE.matcher(plain);
        if (!matcher.matches()) return;

        String name = matcher.group(1).trim();
        int level = parseLevel(matcher.group(2));
        if (level > 0) {
            result.put(name, level);
        }
    }

    private static int parseLevel(String value) {
        if (value.matches("\\d+")) {
            return Integer.parseInt(value);
        }

        int total = 0;
        int previous = 0;
        for (int i = value.length() - 1; i >= 0; i--) {
            int current = romanValue(value.charAt(i));
            if (current < previous) {
                total -= current;
            } else {
                total += current;
                previous = current;
            }
        }
        return Math.max(total, 1);
    }

    private static int romanValue(char c) {
        return switch (Character.toUpperCase(c)) {
            case 'I' -> 1;
            case 'V' -> 5;
            case 'X' -> 10;
            case 'L' -> 50;
            case 'C' -> 100;
            case 'D' -> 500;
            case 'M' -> 1000;
            default -> 0;
        };
    }

    private static void collectEnchants(CompoundTag enchantments, Map<String, Integer> result) {
        if (enchantments == null || enchantments.isEmpty()) return;

        for (String key : enchantments.getAllKeys()) {
            CompoundTag data = enchantments.getCompound(key);
            int level = data.contains(LEVEL_TAG) ? data.getInt(LEVEL_TAG) : 1;
            if (level <= 0) continue;

            String name = ChatFormatting.stripFormatting(key);
            result.put(name == null ? key : name, level);
        }
    }
}
