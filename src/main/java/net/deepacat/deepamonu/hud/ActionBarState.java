package net.deepacat.deepamonu.hud;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ActionBarState {
    private static final Logger LOGGER = LoggerFactory.getLogger("CrosshairHud");

    private static volatile String lastActionBarText = "";
    private static volatile long lastActionBarTime = 0;
    private static volatile int multiloadCurrent = -1;
    private static volatile int multiloadMax = -1;
    private static volatile String multiloadRawText = "";
    private static volatile long lastAmmoDecreaseTime = 0;

    private static final Pattern MULTILOAD_PATTERN = Pattern.compile("[Aa]mmo:\\s*(\\d+)\\s*/\\s*(\\d+)");

    public static boolean isAmmoMessage(String text) {
        return MULTILOAD_PATTERN.matcher(text).find();
    }

    public static void handleActionBar(String plainText) {
        lastActionBarText = plainText;
        lastActionBarTime = System.currentTimeMillis();

        Matcher matcher = MULTILOAD_PATTERN.matcher(plainText);
        if (matcher.find()) {
            int current = Integer.parseInt(matcher.group(1));
            int max = Integer.parseInt(matcher.group(2));
            if (current < multiloadCurrent) {
                lastAmmoDecreaseTime = System.currentTimeMillis();
            }
            multiloadCurrent = current;
            multiloadMax = max;
            multiloadRawText = plainText;
            LOGGER.debug("[Multiload] Parsed: {}/{} ammo (raw: '{}')", current, max, plainText);
        } else {
            LOGGER.debug("[Multiload] Pattern did NOT match: '{}'", plainText);
        }
    }

    public static String getLastActionBarText() {
        return lastActionBarText;
    }

    public static long getLastActionBarTime() {
        return lastActionBarTime;
    }

    public static int getMultiloadCurrent() {
        return multiloadCurrent;
    }

    public static int getMultiloadMax() {
        return multiloadMax;
    }

    public static long getLastAmmoDecreaseTime() {
        return lastAmmoDecreaseTime;
    }

    public static String getMultiloadRawText() {
        return multiloadRawText;
    }

    public static void clearMultiload() {
        multiloadCurrent = -1;
        multiloadMax = -1;
        multiloadRawText = "";
    }

    public static void setMultiload(int current, int max) {
        multiloadCurrent = current;
        multiloadMax = max;
    }
}
