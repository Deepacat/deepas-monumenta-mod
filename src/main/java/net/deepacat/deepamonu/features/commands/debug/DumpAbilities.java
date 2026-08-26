package net.deepacat.deepamonu.features.commands.debug;

import ch.njol.unofficialmonumentamod.AbilityHandler;
import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import net.deepacat.deepamonu.DMMClient;

import java.util.List;

public class DumpAbilities {

    public static void run() {
        try {
            AbilityHandler handler = UnofficialMonumentaModClient.abilityHandler;
            if (handler == null || handler.abilityData.isEmpty()) {
                DMMClient.LOGGER.info("[Debug] No ability data available.");
                return;
            }

            DMMClient.LOGGER.info("[Debug] === Dumping ability data ({}) ===", handler.abilityData.size());

            for (AbilityHandler.AbilityInfo info : handler.abilityData) {
                DMMClient.LOGGER.info(
                        "[Debug]   Name: '{}' | Class: '{}' | Mode: '{}' | " +
                                "RemCooldown: {} ({}s) | InitCooldown: {} ({}s) | " +
                                "RemDuration: {} ({}s) | InitDuration: {} ({}s) | " +
                                "Charges: {}/{} | OffCDAnimTicks: {}",
                        info.name,
                        info.className,
                        info.mode,
                        info.remainingCooldown, formatFloat(info.remainingCooldown / 20f),
                        info.initialCooldown, formatFloat(info.initialCooldown / 20f),
                        info.remainingDuration, formatFloat(info.remainingDuration / 20f),
                        info.initialDuration, formatFloat(info.initialDuration / 20f),
                        info.charges, info.maxCharges,
                        info.offCooldownAnimationTicks
                );
            }

            DMMClient.LOGGER.info("[Debug] === End of ability data dump ===");
        } catch (Throwable e) {
            DMMClient.LOGGER.error("[Debug] Failed to dump ability data", e);
        }
    }

    private static String formatFloat(float f) {
        if (f == (int) f) return String.valueOf((int) f);
        return String.format("%.1f", f);
    }
}
