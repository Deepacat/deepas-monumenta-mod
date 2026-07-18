package net.deepacat.deepamonu.compat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.deepacat.deepamonu.DMMClient;
import net.deepacat.deepamonu.utils.SafeExceptionLogger;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TriggerCache {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CACHE_DIR = FabricLoader.getInstance().getGameDir()
            .resolve("config/deepamonumentamod/triggerCache");
    private static final Path BACKUP_DIR = CACHE_DIR.resolve("backups");

    public static class TriggerEntry {
        public String keyBindingName;
        public boolean requireSneaking;
        public boolean requireNotSneaking;
        public String rawTrigger;
        public boolean doubleClick;
        public boolean requireSprinting;
        public boolean hasSprintCondition;
        public boolean requireOnGround;
        public boolean hasGroundCondition;
        public String lookDirection;
        public String displayString;
        public int displayColor;
        public boolean suppressDisplay;
        public int displayOffsetX;
        public int displayOffsetY;
        public int backgroundOffsetY;

        public TriggerEntry() {}

        public TriggerEntry(String keyBindingName, boolean requireSneaking, boolean requireNotSneaking,
                            String rawTrigger, boolean doubleClick, boolean requireSprinting,
                            boolean hasSprintCondition, boolean requireOnGround, boolean hasGroundCondition,
                            String lookDirection) {
            this.keyBindingName = keyBindingName;
            this.requireSneaking = requireSneaking;
            this.requireNotSneaking = requireNotSneaking;
            this.rawTrigger = rawTrigger;
            this.doubleClick = doubleClick;
            this.requireSprinting = requireSprinting;
            this.hasSprintCondition = hasSprintCondition;
            this.requireOnGround = requireOnGround;
            this.hasGroundCondition = hasGroundCondition;
            this.lookDirection = lookDirection;
        }
    }

    public static class ClassTriggers {
        public String className;
        public Map<String, TriggerEntry> triggers = new LinkedHashMap<>();
        public Map<String, TriggerEntry> hardcoded = new LinkedHashMap<>();

        public ClassTriggers(String className) {
            this.className = className;
        }
    }

    // ---------- helpers ----------
    private static <K,V> Map<K,V> copyMap(Map<K,V> source) {
        return source != null ? new LinkedHashMap<>(source) : new LinkedHashMap<>();
    }

    /**
     * Case‑insensitive lookup in a map of TriggerEntry.
     */
    public static TriggerEntry getCaseInsensitive(Map<String, TriggerEntry> map, String name) {
        if (map == null || name == null) return null;
        for (Map.Entry<String, TriggerEntry> entry : map.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) return entry.getValue();
        }
        return null;
    }

    /**
     * Checks whether a map already contains the given name (ignoring case).
     */
    public static boolean containsCaseInsensitive(Map<String, TriggerEntry> map, String name) {
        return getCaseInsensitive(map, name) != null;
    }

    // ---------- file loading & merging ----------
    public static ClassTriggers loadClassTriggers(String className) {
        Path file = CACHE_DIR.resolve(className.toLowerCase(Locale.ROOT) + ".json");
        if (!Files.exists(file)) return null;
        try {
            String json = Files.readString(file);
            return GSON.fromJson(json, ClassTriggers.class);
        } catch (IOException | JsonSyntaxException e) {
            DMMClient.LOGGER.warn("Failed to load trigger cache for {}. Corrupted file will be moved to backups.", className, e);
            DMMClient.GLOBAL_SAFE_EH.runSafely(() -> {
                try {
                    Files.createDirectories(BACKUP_DIR);
                    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                    Path backupFile = BACKUP_DIR.resolve(className.toLowerCase(Locale.ROOT) + "_" + timestamp + ".json");
                    Files.move(file, backupFile, StandardCopyOption.REPLACE_EXISTING);
                    DMMClient.LOGGER.info("Corrupted trigger file moved to {}", backupFile);
                    notifyPlayer(Component.literal("⚠ Trigger file for " + className + " was corrupted and has been backed up."));
                } catch (IOException moveEx) {
                    DMMClient.LOGGER.error("Could not backup corrupted file; deleting instead.", moveEx);
                    try {
                        Files.deleteIfExists(file);
                    } catch (IOException delEx) {
                        DMMClient.LOGGER.error("Could not delete corrupted trigger file", delEx);
                    }
                }
            }, () -> "Backup corrupted trigger file for " + className);
            return null;
        }
    }

    public static void mergeAndSave(String className, Map<String, TriggerEntry> newAutoTriggers) {
        if (className == null) return;
        if (newAutoTriggers == null) newAutoTriggers = Collections.emptyMap();

        ClassTriggers existing = loadClassTriggers(className);
        Map<String, TriggerEntry> mergedAuto = copyMap(existing != null ? existing.triggers : null);
        Map<String, TriggerEntry> hardcoded = copyMap(existing != null ? existing.hardcoded : null);

        // Merge new abilities – keep existing ones, overwrite only if the name matches (case-insensitive)
        for (Map.Entry<String, TriggerEntry> entry : newAutoTriggers.entrySet()) {
            String name = entry.getKey();
            // Remove any existing entry with the same name (ignoring case)
            mergedAuto.keySet().removeIf(k -> k.equalsIgnoreCase(name));
            // Insert the new entry with its original casing
            mergedAuto.put(name, entry.getValue());
        }

        try {
            Files.createDirectories(CACHE_DIR);
            ClassTriggers ct = new ClassTriggers(className);
            ct.triggers = mergedAuto;
            ct.hardcoded = hardcoded;
            String json = GSON.toJson(ct);
            Files.writeString(CACHE_DIR.resolve(className.toLowerCase(Locale.ROOT) + ".json"), json);
        } catch (IOException e) {
            DMMClient.LOGGER.error("Failed to save trigger cache for " + className, e);
        }
    }

    public static void clearAllAutoTriggers() {
        if (!Files.exists(CACHE_DIR)) return;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(CACHE_DIR, "*.json")) {
            for (Path file : stream) {
                try {
                    String json = Files.readString(file);
                    ClassTriggers ct = GSON.fromJson(json, ClassTriggers.class);
                    if (ct != null) {
                        ct.triggers.clear();
                        ct.hardcoded = ct.hardcoded != null ? new LinkedHashMap<>(ct.hardcoded) : new LinkedHashMap<>();
                        Files.writeString(file, GSON.toJson(ct));
                    }
                } catch (IOException | JsonSyntaxException e) {
                    DMMClient.LOGGER.warn("Failed to reset triggers in {}", file, e);
                }
            }
        } catch (IOException e) {
            DMMClient.LOGGER.error("Could not list trigger cache files", e);
        }
    }

    private static void notifyPlayer(Component message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(message, false);
        }
    }
}