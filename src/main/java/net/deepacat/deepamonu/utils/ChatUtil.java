package net.deepacat.deepamonu.utils;

import net.deepacat.deepamonu.DMMClient;
import net.deepacat.deepamonu.config.ModConfig.Appearance;

import java.util.Objects;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class ChatUtil {
    public static void send(Component... message) {
        Appearance config = DMMClient.appearance();
        DMMClient.player()
                .sendSystemMessage(
                        FormatUtil.join(
                                FormatUtil.withColor("[", config.bracketColor),
                                FormatUtil.withColor(config.tagText, config.tagColor).withStyle(ChatFormatting.BOLD),
                                FormatUtil.withColor("] ", config.bracketColor),
                                FormatUtil.colored(config.textColor).append(FormatUtil.join(message))
                        )
                );
    }

    public static void send(String message) {
        send(FormatUtil.literal(message));
    }

    public static void sendWarn(Component message) {
        send(Component.empty().append(FormatUtil.withColor("WARN", DMMClient.appearance().warningColor)).append(" ").append(message));
    }

    public static void sendWarn(String message) {
        sendWarn(FormatUtil.literal(message));
    }

    public static void sendDebug(String message) {
        if (!DMMClient.features().suppressDebugWarning) {
            sendWarn("(debug/possible bug) " + message);
        } else {
            DMMClient.LOGGER.warn(message);
        }
    }

    public static void sendCommand(String command) {
        if (command.startsWith("/")) {
            DMMClient.LOGGER.warn("leading /");
        }

        DMMClient.LOGGER.debug("running command as client: {}", command);
        Objects.requireNonNull(Minecraft.getInstance().getConnection()).sendCommand(command);
    }
}
