package net.deepacat.deepamonu.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface ClientPlayerHurtEvent {
    Event<ClientPlayerHurtEvent> EVENT = EventFactory.createArrayBacked(
            ClientPlayerHurtEvent.class,
            listeners -> damage -> {
                for (ClientPlayerHurtEvent listener : listeners) {
                    listener.onHurt(damage);
                }
            }
    );

    void onHurt(float damage);
}