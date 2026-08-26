package net.deepacat.deepamonu.features;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import com.dayssky.mma.events.ClientReceiveSystemChatEvent;
import com.dayssky.mma.events.EventResult;
import net.deepacat.deepamonu.features.commands.debug.DpsTestTracker;

import java.util.Locale;

public class SoundReward {
    public static void init() {
        ClientReceiveSystemChatEvent.EVENT.register(text -> {
            if (DpsTestTracker.isRunning() && DpsTestTracker.isDamageMessage(text)) {
                DpsTestTracker.parseAndRecord(text);
                return EventResult.CONTINUE;
            }
            return EventResult.CONTINUE;
        });
    }
}
