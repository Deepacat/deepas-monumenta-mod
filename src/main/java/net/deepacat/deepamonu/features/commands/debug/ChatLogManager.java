package net.deepacat.deepamonu.features.commands.debug;

import net.deepacat.deepamonu.DMMClient;
import net.deepacat.deepamonu.features.commands.DpsTestTracker;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class ChatLogManager {
    private static final int MAX_MESSAGES = 200;
    private static final Deque<LoggedMessage> MESSAGES = new ArrayDeque<>();
    private static boolean liveLogging = false;

    private ChatLogManager() {
    }

    public static synchronized void capture(Component content, GuiMessageTag tag) {
        if (content == null) return;

        DpsTestTracker.parseAndRecord(content);

        LoggedMessage message = new LoggedMessage(System.currentTimeMillis(), content, tag);
        MESSAGES.addLast(message);
        while (MESSAGES.size() > MAX_MESSAGES) {
            MESSAGES.removeFirst();
        }

        if (liveLogging) {
            logMessage("Live message", message);
        }
    }

    public static synchronized boolean toggleLiveLogging() {
        liveLogging = !liveLogging;
        return liveLogging;
    }

    public static synchronized void dumpLast(int count) {
        if (MESSAGES.isEmpty()) {
            DMMClient.LOGGER.info("[Debug] No captured chat messages.");
            return;
        }

        List<LoggedMessage> snapshot = new ArrayList<>(MESSAGES);
        int start = Math.max(0, snapshot.size() - count);
        int shown = snapshot.size() - start;

        DMMClient.LOGGER.info("[Debug] === Dumping last {} chat messages ({} captured) ===", shown, snapshot.size());
        for (int i = start; i < snapshot.size(); i++) {
            logMessage("Message " + (i - start + 1) + "/" + shown, snapshot.get(i));
        }
        DMMClient.LOGGER.info("[Debug] === End of chat dump ===");
    }

    private static void logMessage(String label, LoggedMessage message) {
        Component content = message.content();
        DMMClient.LOGGER.info("[Debug] --- {} | time={} ---", label, Instant.ofEpochMilli(message.time()));
        DMMClient.LOGGER.info("[Debug]   Raw text: '{}'", content.getString());
        DMMClient.LOGGER.info("[Debug]   JSON: {}", safeJson(content));
        if (message.tag() != null) {
            DMMClient.LOGGER.info("[Debug]   Tag: {}", message.tag());
        }

        List<Component> flat = content.toFlatList();
        for (int i = 0; i < flat.size(); i++) {
            logComponent("  [" + i + "]", flat.get(i), 0);
        }
    }

    private static void logComponent(String prefix, Component component, int depth) {
        Style style = component.getStyle();
        StringBuilder details = new StringBuilder();
        details.append(prefix).append(" text='").append(component.getString()).append("'");

        if (style.getColor() != null) details.append(" color=").append(style.getColor());
        if (style.isBold()) details.append(" bold");
        if (style.isItalic()) details.append(" italic");
        if (style.isUnderlined()) details.append(" underlined");
        if (style.isStrikethrough()) details.append(" strikethrough");
        if (style.isObfuscated()) details.append(" obfuscated");
        if (style.getInsertion() != null) details.append(" insertion='").append(style.getInsertion()).append("'");

        ClickEvent click = style.getClickEvent();
        if (click != null) {
            details.append(" click=").append(click.getAction()).append(" -> '").append(click.getValue()).append("'");
        }

        HoverEvent hover = style.getHoverEvent();
        if (hover != null) {
            details.append(" hover=").append(hover.getAction());
        }
        DMMClient.LOGGER.info("[Debug]{}", details);

        if (hover == null || depth >= 2) return;

        Component hoverText = hover.getValue(HoverEvent.Action.SHOW_TEXT);
        if (hoverText != null) {
            DMMClient.LOGGER.info("[Debug]{}   hoverText='{}'", prefix, hoverText.getString());
            DMMClient.LOGGER.info("[Debug]{}   hoverJson={}", prefix, safeJson(hoverText));
            for (Component line : hoverText.toFlatList()) {
                logComponent(prefix + "    hover", line, depth + 1);
            }
        }

        HoverEvent.ItemStackInfo itemInfo = hover.getValue(HoverEvent.Action.SHOW_ITEM);
        if (itemInfo != null) {
            ItemStack stack = itemInfo.getItemStack();
            DMMClient.LOGGER.info("[Debug]{}   hoverItem='{}' id={} count={} tag={}",
                    prefix, stack.getHoverName().getString(), stack.getItem(), stack.getCount(), stack.getTag());
        }

        HoverEvent.EntityTooltipInfo entityInfo = hover.getValue(HoverEvent.Action.SHOW_ENTITY);
        if (entityInfo != null) {
            DMMClient.LOGGER.info("[Debug]{}   hoverEntity type={} id={} name={}",
                    prefix, entityInfo.type, entityInfo.id,
                    entityInfo.name.map(Component::getString).orElse(""));
            try {
                for (Component line : entityInfo.getTooltipLines()) {
                    DMMClient.LOGGER.info("[Debug]{}     entityTooltip='{}' json={}",
                            prefix, line.getString(), safeJson(line));
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private static String safeJson(Component component) {
        try {
            return Component.Serializer.toJson(component);
        } catch (Throwable throwable) {
            return "<failed: " + throwable.getMessage() + ">";
        }
    }

    private record LoggedMessage(long time, Component content, GuiMessageTag tag) {
    }
}
