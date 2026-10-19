package net.deepacat.deepamonu.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.player.LocalPlayer;

@FunctionalInterface
public interface LocalShieldHitEvent {
    Event<LocalShieldHitEvent> EVENT = EventFactory.createArrayBacked(LocalShieldHitEvent.class, listeners -> entity -> {
        for (LocalShieldHitEvent listener : listeners) {
            listener.onShieldHit(entity);
        }
    });

    void onShieldHit(LocalPlayer var1);
}