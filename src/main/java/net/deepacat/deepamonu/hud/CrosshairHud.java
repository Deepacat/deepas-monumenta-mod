package net.deepacat.deepamonu.hud;

public class CrosshairHud {

    /**
     * Crosshair displays are registered as HUD elements by {@link DMMHud}.
     * This only drives their per-tick state updates.
     */
    public static void tick() {
        CompactAbilityDisplay.tick();
        MultiloadDisplay.tick();
    }
}
