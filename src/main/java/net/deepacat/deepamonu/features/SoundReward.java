package net.deepacat.deepamonu.features;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import com.dayssky.mma.events.ClientReceiveSystemChatEvent;
import com.dayssky.mma.events.EventResult;

import java.util.Locale;

public class SoundReward {
    public static void init() {
        ClientReceiveSystemChatEvent.EVENT.register(text -> {
            String a = "a";
            String b = !UnofficialMonumentaModClient.abilityHandler.abilityData.isEmpty() ? UnofficialMonumentaModClient.abilityHandler.abilityData.get(0).className.toLowerCase(Locale.ROOT) : "Timed out";

//            DMMClient.LOGGER.info(b);

            return EventResult.CONTINUE;
        });
    }
}
