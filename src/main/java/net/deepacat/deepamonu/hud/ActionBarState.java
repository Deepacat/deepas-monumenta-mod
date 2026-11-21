package net.deepacat.deepamonu.hud;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

public class ActionBarState {
    private static final Logger LOGGER = LoggerFactory.getLogger("CrosshairHud");

    private static final Pattern AMMO_PATTERN = Pattern.compile("[Aa]mmo:\\s*\\d+\\s*/\\s*\\d+");

    private static volatile String lastActionBarText = "";
    private static volatile long lastActionBarTime = 0;

    public static boolean isAmmoMessage(String text) {
        return AMMO_PATTERN.matcher(text).find();
    }

    public static void handleActionBar(String plainText) {
        lastActionBarText = plainText;
        lastActionBarTime = System.currentTimeMillis();
        LOGGER.debug("[ActionBar] {}", plainText);
    }

    public static String getLastActionBarText() {
        return lastActionBarText;
    }

    public static long getLastActionBarTime() {
        return lastActionBarTime;
    }
}
