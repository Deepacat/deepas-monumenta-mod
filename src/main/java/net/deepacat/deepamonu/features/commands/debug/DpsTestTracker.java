package net.deepacat.deepamonu.features.commands.debug;

import net.deepacat.deepamonu.utils.ChatUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;

public class DpsTestTracker {

    private static boolean running = false;
    private static long startTime = 0;
    private static long firstDamageTime = 0;
    private static long lastDamageTime = 0;
    private static long durationMs = 0;
    private static double targetDamage = 0;
    private static boolean totalMode = false;
    private static boolean infiniteMode = false;
    private static final List<DamageEvent> events = new ArrayList<>();

    public static boolean isRunning() {
        return running;
    }

    public static void start(int seconds) {
        events.clear();
        running = true;
        totalMode = false;
        infiniteMode = false;
        startTime = System.currentTimeMillis();
        firstDamageTime = 0;
        lastDamageTime = 0;
        durationMs = seconds * 1000L;
        targetDamage = 0;
    }

    public static void startTotal(double target) {
        events.clear();
        running = true;
        totalMode = true;
        infiniteMode = false;
        startTime = System.currentTimeMillis();
        firstDamageTime = 0;
        lastDamageTime = 0;
        targetDamage = target;
        durationMs = 0;
    }

    public static void startInfinite() {
        events.clear();
        running = true;
        totalMode = false;
        infiniteMode = true;
        startTime = System.currentTimeMillis();
        firstDamageTime = 0;
        lastDamageTime = 0;
        targetDamage = 0;
        durationMs = 0;
    }

    public static void stop() {
        if (running) {
            finish();
        }
    }

    public static void tick() {
        if (!running || firstDamageTime == 0) return;
        if (infiniteMode) return;
        if (totalMode) {
            double sum = events.stream().mapToDouble(e -> e.value).sum();
            if (sum >= targetDamage) {
                finish();
            }
        } else {
            if (System.currentTimeMillis() - firstDamageTime >= durationMs) {
                finish();
            }
        }
    }

    public static void recordDamage(double value, String source) {
        if (!running) return;
        if (firstDamageTime == 0) {
            firstDamageTime = System.currentTimeMillis();
        }
        lastDamageTime = System.currentTimeMillis();
        events.add(new DamageEvent(value, source));
    }

    public static boolean isDamageMessage(Component message) {
        return message.getString().startsWith("Damage: ");
    }

    public static void parseAndRecord(Component message) {
        if (!running) return;
        String text = message.getString();
        if (!text.startsWith("Damage: ")) return;

        double value;
        try {
            String numPart = text.substring(8).trim().split(" ")[0].replace(",", ".");
            value = Double.parseDouble(numPart);
        } catch (Exception e) {
            return;
        }

        String source = "Unknown";
        for (Component flat : message.toFlatList()) {
            HoverEvent hover = flat.getStyle().getHoverEvent();
            if (hover != null) {
                Component hoverValue = hover.getValue(HoverEvent.Action.SHOW_TEXT);
                if (hoverValue != null) {
                    source = parseSourceFromHover(hoverValue.getString());
                    break;
                }
            }
        }

        recordDamage(value, source);
    }

    private static String parseSourceFromHover(String hoverText) {
        for (String line : hoverText.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("Source: ")) {
                return trimmed.substring(8).trim();
            }
        }
        for (String line : hoverText.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("Type: ")) {
                return trimmed.substring(6).trim();
            }
        }
        return "Damage";
    }

    private static void finish() {
        running = false;
        outputResults();
    }

    private static void outputResults() {
        if (events.isEmpty()) {
            ChatUtil.send("No damage recorded during the test period.");
            return;
        }

        double totalDamage = events.stream().mapToDouble(e -> e.value).sum();
        double totalTime = lastDamageTime > 0
                ? (lastDamageTime - firstDamageTime) / 1000.0
                : firstDamageTime > 0
                    ? (System.currentTimeMillis() - firstDamageTime) / 1000.0
                    : 0.0;

        Map<String, Double> perSourceDamage = new LinkedHashMap<>();
        for (DamageEvent e : events) {
            perSourceDamage.merge(e.source, e.value, Double::sum);
        }

        List<Map.Entry<String, Double>> sorted = new ArrayList<>(perSourceDamage.entrySet());
        sorted.sort(Comparator.<Map.Entry<String, Double>>comparingDouble(Map.Entry::getValue).reversed());

        StringBuilder perAbilityDetail = new StringBuilder();
        StringBuilder perAbilityDpsDetail = new StringBuilder();
        for (Map.Entry<String, Double> entry : sorted) {
            double dps = entry.getValue() / totalTime;
            perAbilityDetail.append(entry.getKey()).append(": ")
                    .append(String.format("%.2f", entry.getValue()))
                    .append(" (").append(String.format("%.1f", entry.getValue() / totalDamage * 100)).append("%)\n");
            perAbilityDpsDetail.append(entry.getKey()).append(": ")
                    .append(String.format("%.2f", dps)).append(" /s\n");
        }

        Component totalHover = Component.literal(perAbilityDetail.toString().trim());
        Component dpsHover = Component.literal(perAbilityDpsDetail.toString().trim());

        MutableComponent totalText = Component.literal(String.format("%.2f", totalDamage));
        totalText.setStyle(Style.EMPTY.withColor(0xFFFF5555)
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, totalHover)));

        MutableComponent dpsText = Component.literal(String.format("%.2f", totalDamage / totalTime));
        dpsText.setStyle(Style.EMPTY.withColor(0xFFFF5555)
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, dpsHover)));

        ChatUtil.send(
                Component.literal("Total: ").withStyle(s -> s.withColor(0xFFAAAAAA)),
                totalText,
                Component.literal("  DPS: ").withStyle(s -> s.withColor(0xFFAAAAAA)),
                dpsText,
                Component.literal("  Time: ").withStyle(s -> s.withColor(0xFFAAAAAA)),
                Component.literal(String.format("%.2fs", totalTime)).withStyle(s -> s.withColor(0xFF55FFFF))
        );
    }

    private record DamageEvent(double value, String source) {}
}
