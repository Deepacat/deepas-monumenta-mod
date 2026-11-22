package net.deepacat.deepamonu.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.player.LocalPlayer;

public interface LocalShieldBreakEvent {
    Event<LocalShieldBreakEvent> EVENT = EventFactory.createArrayBacked(LocalShieldBreakEvent.class, listeners -> entity -> {
        for (LocalShieldBreakEvent listener : listeners) {
            listener.onShieldBreak(entity);
        }
    });

    void onShieldBreak(LocalPlayer var1);
}
