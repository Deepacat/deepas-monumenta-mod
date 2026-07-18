package net.deepacat.deepamonu.compat;

import ch.njol.unofficialmonumentamod.AbilityHandler;
import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import net.deepacat.deepamonu.compat.TriggerCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class TriggerSaving {
    private static final Logger LOGGER = LoggerFactory.getLogger("TriggerSaving");
    private static final Map<Integer, String> LAST_SNAPSHOTS = new HashMap<>();

    private static final Map<String, String> KEY_MAP = new LinkedHashMap<>();
    static {
        KEY_MAP.put("left click", "key.attack");
        KEY_MAP.put("right click", "key.use");
        KEY_MAP.put("swap hands", "key.swapOffhand");
        KEY_MAP.put("offhand", "key.swapOffhand");
        KEY_MAP.put("drop item", "key.drop");
        KEY_MAP.put("shoot projectile", "key.attack");
        KEY_MAP.put("throwing a potion", "key.use");
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof AbstractContainerScreen<?> screen)) return;
        if (screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen) return;

        String title = screen.getTitle().getString();
        if (!title.equals("Ability Triggers") && !title.equals("Class Selection GUI")) return;

        // Fingerprint to avoid re‑parsing the same content
        String currentSnapshot = buildSnapshot(screen);
        int menuId = System.identityHashCode(screen.getMenu());
        String last = LAST_SNAPSHOTS.get(menuId);
        if (currentSnapshot.equals(last)) return;
        LAST_SNAPSHOTS.put(menuId, currentSnapshot);

        // Build ability -> class name map from the currently active ability data
        Map<String, String> abilityClassMap = buildAbilityClassMap();

        // Parse the container into per‑class maps
        Map<String, Map<String, TriggerCache.TriggerEntry>> triggersByClass = new LinkedHashMap<>();
        parseContainer(screen, abilityClassMap, triggersByClass);

        // Save each class's triggers
        for (Map.Entry<String, Map<String, TriggerCache.TriggerEntry>> entry : triggersByClass.entrySet()) {
            String className = entry.getKey();
            Map<String, TriggerCache.TriggerEntry> classTriggers = entry.getValue();
            if (!classTriggers.isEmpty()) {
                TriggerCache.mergeAndSave(className, classTriggers);
                LOGGER.info("Saved {} auto-triggers for class {}", classTriggers.size(), className);
            }
        }
    }

    private static String buildSnapshot(AbstractContainerScreen<?> screen) {
        StringBuilder sb = new StringBuilder();
        for (Slot slot : screen.getMenu().slots) {
            if (slot.container == Minecraft.getInstance().player.getInventory()) continue;
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) continue;
            CompoundTag tag = stack.getTag();
            sb.append(stack.getHoverName().getString()).append("::");
            sb.append(tag != null ? tag.toString() : "null").append("||");
        }
        return sb.toString();
    }

    /**
     * Builds a case‑insensitive map of ability name → class name from the
     * currently loaded UMM ability data. Fallback: if empty, use the old
     * single‑class detection (getCurrentClassName).
     */
    private static Map<String, String> buildAbilityClassMap() {
        Map<String, String> map = new LinkedHashMap<>();
        try {
            AbilityHandler handler = UnofficialMonumentaModClient.abilityHandler;
            if (handler != null && !handler.abilityData.isEmpty()) {
                for (AbilityHandler.AbilityInfo info : handler.abilityData) {
                    if (info.name != null && info.className != null) {
                        map.put(info.name.toLowerCase(Locale.ROOT), info.className);
                    }
                }
            }
        } catch (Throwable ignored) {}
        return map;
    }

    /**
     * Parses the container and fills the per‑class trigger maps.
     */
    private static void parseContainer(AbstractContainerScreen<?> screen,
                                       Map<String, String> abilityClassMap,
                                       Map<String, Map<String, TriggerCache.TriggerEntry>> triggersByClass) {
        String title = screen.getTitle().getString();
        for (Slot slot : screen.getMenu().slots) {
            if (slot.container == Minecraft.getInstance().player.getInventory()) continue;
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) continue;

            CompoundTag tag = stack.getTag();
            if (tag == null || !tag.contains("display")) continue;

            String fullName = stack.getHoverName().getString();
            if (fullName.equals("Back") || fullName.equals("Help")
                    || fullName.equals("Revert all triggers to defaults")
                    || fullName.isEmpty()) continue;

            String abilityName = fullName.replaceFirst(" - .*$", "").replaceAll("\\s*\\[.*\\]$", "").trim();
            if (abilityName.isEmpty()) continue;

            // Determine the class for this ability
            String className = abilityClassMap.get(abilityName.toLowerCase(Locale.ROOT));
            if (className == null) {
                // Fallback: use the old single‑class detection (works for normal dimensions)
                className = getCurrentClassName();
                if (className == null) continue; // cannot determine class, skip
            }

            // Parse trigger details (same as before)
            String rawKey = null;
            boolean requireSneaking = false, requireNotSneaking = false;
            boolean doubleClick = false;
            boolean requireSprinting = false, hasSprintCondition = false;
            boolean requireOnGround = false, hasGroundCondition = false;
            String lookDirection = null;

            if (tag.contains("display")) {
                CompoundTag display = tag.getCompound("display");
                if (display.contains("Lore")) {
                    net.minecraft.nbt.ListTag lore = display.getList("Lore", 8);
                    List<String> lines = new ArrayList<>();
                    for (int i = 0; i < lore.size(); i++) {
                        lines.add(Component.Serializer.fromJson(lore.getString(i)).getString());
                    }

                    boolean readTrigger = false;
                    int keyLineIndex = -1;
                    for (int i = 0; i < lines.size(); i++) {
                        String line = lines.get(i);
                        if (line.startsWith("Current Trigger") || line.startsWith("Custom Trigger")) {
                            readTrigger = true;
                        } else if (readTrigger) {
                            rawKey = line.trim();
                            keyLineIndex = i;
                            readTrigger = false;
                            break;
                        }
                    }

                    if (keyLineIndex >= 0) {
                        for (int i = keyLineIndex + 1; i < lines.size(); i++) {
                            String line = lines.get(i).trim();
                            if (line.equals("- sneaking")) {
                                requireSneaking = true;
                            } else if (line.equals("- not sneaking")) {
                                requireNotSneaking = true;
                            } else if (line.equals("- double click")) {
                                doubleClick = true;
                            } else if (line.equals("- sprinting")) {
                                requireSprinting = true;
                                hasSprintCondition = true;
                            } else if (line.equals("- not sprinting")) {
                                requireSprinting = false;
                                hasSprintCondition = true;
                            } else if (line.equals("- on ground")) {
                                requireOnGround = true;
                                hasGroundCondition = true;
                            } else if (line.equals("- not on ground")) {
                                requireOnGround = false;
                                hasGroundCondition = true;
                            } else if (line.startsWith("- looking ")) {
                                String dir = line.substring("- looking ".length()).trim().toLowerCase(Locale.ROOT);
                                dir = dir.replaceAll("\\s+", " ");
                                switch (dir) {
                                    case "down" -> lookDirection = "DOWN";
                                    case "level" -> lookDirection = "LEVEL";
                                    case "up" -> lookDirection = "UP";
                                    case "down or level" -> lookDirection = "DOWN_OR_LEVEL";
                                    case "down or up" -> lookDirection = "DOWN_OR_UP";
                                    case "level or up" -> lookDirection = "LEVEL_OR_UP";
                                }
                            }
                        }
                    }
                }
            }

            // Class Selection GUI parsing
            if (title.equals("Class Selection GUI") && rawKey == null) {
                if (tag.contains("display")) {
                    CompoundTag display = tag.getCompound("display");
                    if (display.contains("Lore")) {
                        net.minecraft.nbt.ListTag lore = display.getList("Lore", 8);
                        for (int i = 0; i < lore.size(); i++) {
                            String line = Component.Serializer.fromJson(lore.getString(i)).getString();
                            if (line.startsWith(" Trigger: ")) {
                                String triggerText = line.substring(" Trigger: ".length()).trim();
                                parseClassSelectionTrigger(triggerText, className, abilityName, triggersByClass);
                                continue;
                            } else if (line.contains(" while sneaking")) {
                                String triggerText = line.trim();
                                parseClassSelectionTrigger(triggerText, className, abilityName, triggersByClass);
                                continue;
                            }
                        }
                    }
                }
                continue; // skip normal key extraction for this item
            }

            if (rawKey == null || rawKey.equalsIgnoreCase("Trigger is disabled!")) continue;

            String keyBindingName = rawKeyToBindingName(rawKey);
            if (keyBindingName == null) continue;

            TriggerCache.TriggerEntry entry = new TriggerCache.TriggerEntry(
                    keyBindingName, requireSneaking, requireNotSneaking, rawKey,
                    doubleClick, requireSprinting, hasSprintCondition,
                    requireOnGround, hasGroundCondition, lookDirection);

            // Add to the correct class map
            Map<String, TriggerCache.TriggerEntry> classMap = triggersByClass.computeIfAbsent(className, k -> new LinkedHashMap<>());
            if (!TriggerCache.containsCaseInsensitive(classMap, abilityName)) {
                classMap.put(abilityName, entry);
            }
        }
    }

    private static void parseClassSelectionTrigger(String triggerText, String className, String abilityName,
                                                   Map<String, Map<String, TriggerCache.TriggerEntry>> triggersByClass) {
        String rawKey = triggerText;
        boolean requireSneaking = false;
        String[] parts = triggerText.split(" while ", 2);
        if (parts.length > 1) {
            rawKey = parts[0].trim();
            String condition = parts[1].trim();
            if (condition.equalsIgnoreCase("Sneaking")) {
                requireSneaking = true;
            }
        }
        String lower = rawKey.toLowerCase(Locale.ROOT);
        if (lower.contains("shoot projectile") || lower.contains("firing a projectile")) {
            rawKey = "Shoot Projectile";
        } else if (lower.contains("throwing a potion")) {
            rawKey = "Throwing a Potion";
        }

        String keyBindingName = rawKeyToBindingName(rawKey);
        if (keyBindingName == null) return;

        Map<String, TriggerCache.TriggerEntry> classMap = triggersByClass.computeIfAbsent(className, k -> new LinkedHashMap<>());
        if (!TriggerCache.containsCaseInsensitive(classMap, abilityName)) {
            classMap.put(abilityName, new TriggerCache.TriggerEntry(
                    keyBindingName, requireSneaking, false, rawKey,
                    false, false, false, false, false, null));
        }
    }

    private static String rawKeyToBindingName(String rawKey) {
        String lower = rawKey.trim().toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> entry : KEY_MAP.entrySet()) {
            if (lower.equals(entry.getKey())) return entry.getValue();
        }
        if (lower.contains("shoot projectile")) return "key.attack";
        if (lower.contains("throwing a potion")) return "key.use";
        return null;
    }

    private static String getCurrentClassName() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return null;
        try {
            var handler = UnofficialMonumentaModClient.abilityHandler;
            if (!handler.abilityData.isEmpty()) {
                return handler.abilityData.get(0).className;
            }
        } catch (Throwable ignored) {}
        return null;
    }
}