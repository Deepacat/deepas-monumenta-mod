package net.deepacat.deepamonu.features.commands.debug;

import net.deepacat.deepamonu.DMMClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;

import java.util.List;

public class DumpChatMessage {

    public static void dumpLast(int count) {
        Minecraft mc = Minecraft.getInstance();
        ChatComponent chat = mc.gui.getChat();
        List<GuiMessage> messages = chat.allMessages;

        if (messages.isEmpty()) {
            DMMClient.LOGGER.info("[Debug] No messages in chat.");
            return;
        }

        int start = Math.max(0, messages.size() - count);
        int shown = Math.min(count, messages.size());

        DMMClient.LOGGER.info("[Debug] === Dumping last {} chat messages ({} total) ===", shown, messages.size());

        for (int i = start; i < messages.size(); i++) {
            GuiMessage msg = messages.get(i);
            Component content = msg.content();
            DMMClient.LOGGER.info("[Debug] --- Message {}/{} ---", (i - start + 1), shown);
            DMMClient.LOGGER.info("[Debug]   Plain text: '{}'", content.getString());

            List<Component> flat = content.toFlatList();
            for (int j = 0; j < flat.size(); j++) {
                Component c = flat.get(j);
                Style style = c.getStyle();
                ClickEvent click = style.getClickEvent();
                HoverEvent hover = style.getHoverEvent();

                StringBuilder sb = new StringBuilder();
                sb.append("    [").append(j).append("] '").append(c.getString()).append("'");
                if (click != null) {
                    sb.append(" | Click: ").append(click.getAction())
                            .append(" -> '").append(click.getValue()).append("'");
                }
                if (hover != null) {
                    sb.append(" | Hover: ").append(hover.getAction());
                    Component hoverValue = hover.getValue(HoverEvent.Action.SHOW_TEXT);
                    if (hoverValue != null) {
                        sb.append(" -> '").append(hoverValue.getString()).append("'");
                    }
                }
                DMMClient.LOGGER.info("[Debug]{}", sb.toString());
            }
        }

        DMMClient.LOGGER.info("[Debug] === End of chat dump ===");
    }

    public static void dumpHovered() {
        Minecraft mc = Minecraft.getInstance();

        if (!(mc.screen instanceof ChatScreen)) {
            DMMClient.LOGGER.info("[Debug] Chat is not open. Open chat and hover a message, then press the key.");
            return;
        }

        ChatComponent chat = mc.gui.getChat();
        double mouseX = mc.mouseHandler.xpos();
        double mouseY = mc.mouseHandler.ypos();

        int guiScale = (int) mc.getWindow().getGuiScale();
        double scaledX = mouseX / guiScale;
        double scaledY = mouseY / guiScale;

        net.minecraft.client.GuiMessageTag tag = chat.getMessageTagAt(scaledX, scaledY - chat.getHeight());
        if (tag == null) {
            DMMClient.LOGGER.info("[Debug] No message hovered at mouse position.");
            return;
        }

        List<GuiMessage> messages = chat.allMessages;
        Component found = null;
        for (GuiMessage msg : messages) {
            if (msg.tag() != null && msg.tag().equals(tag)) {
                found = msg.content();
                break;
            }
        }

        if (found == null) {
            DMMClient.LOGGER.info("[Debug] Could not find hovered message.");
            return;
        }

        DMMClient.LOGGER.info("[Debug] === Dumping hovered chat message ===");
        DMMClient.LOGGER.info("[Debug] Plain text: '{}'", found.getString());

        List<Component> flat = found.toFlatList();
        for (int i = 0; i < flat.size(); i++) {
            Component c = flat.get(i);
            Style style = c.getStyle();
            ClickEvent click = style.getClickEvent();
            HoverEvent hover = style.getHoverEvent();

            StringBuilder sb = new StringBuilder();
            sb.append("  [").append(i).append("] '").append(c.getString()).append("'");
            if (click != null) {
                sb.append(" | Click: ").append(click.getAction())
                        .append(" -> '").append(click.getValue()).append("'");
            }
            if (hover != null) {
                sb.append(" | Hover: ").append(hover.getAction());
                Component hoverValue = hover.getValue(HoverEvent.Action.SHOW_TEXT);
                if (hoverValue != null) {
                    sb.append(" -> '").append(hoverValue.getString()).append("'");
                }
            }
            DMMClient.LOGGER.info("[Debug]{}", sb.toString());
        }

        DMMClient.LOGGER.info("[Debug] === End of chat message dump ===");
    }
}
