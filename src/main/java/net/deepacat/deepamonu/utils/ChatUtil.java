package net.deepacat.deepamonu.utils;

import net.deepacat.deepamonu.ClientInit;
import net.deepacat.deepamonu.config.ModConfig.Appearance;

import java.util.Objects;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class ChatUtil {
    public static void send(Component... message) {
        Appearance config = ClientInit.appearance();
        ClientInit.player()
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
        send(Component.empty().append(FormatUtil.withColor("WARN", ClientInit.appearance().warningColor)).append(" ").append(message));
    }

    public static void sendWarn(String message) {
        sendWarn(FormatUtil.literal(message));
    }

    public static void sendDebug(String message) {
        if (!ClientInit.features().suppressDebugWarning) {
            sendWarn("(debug/possible bug) " + message);
        } else {
            ClientInit.LOGGER.warn(message);
        }
    }

    public static void sendCommand(String command) {
        if (command.startsWith("/")) {
            ClientInit.LOGGER.warn("leading /");
        }

        ClientInit.LOGGER.debug("running command as client: {}", command);
        Objects.requireNonNull(Minecraft.getInstance().getConnection()).sendCommand(command);
    }
}
