package net.deepacat.deepamonu.hud;

import ch.njol.minecraft.uiframework.hud.Hud;
import net.deepacat.deepamonu.hud.elements.DMMHudElement;
import net.deepacat.deepamonu.hud.elements.GuardSituText;
import net.deepacat.deepamonu.hud.elements.ShieldBreakText;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

/**
 * Single registration/rendering point for every DMM HUD element.
 *
 * <p>Add new elements in {@link #init()} and they will automatically be
 * rendered by the shared InGameHud/Gui hooks.
 */
public final class DMMHud {

    private static final List<DMMHudElement> ELEMENTS = new ArrayList<>();

    private DMMHud() {
    }

    public static void init() {
        register(ShieldBreakText.INSTANCE);
        register(GuardSituText.INSTANCE);
        register(CompactAbilityDisplay.INSTANCE);
        register(MultiloadDisplay.INSTANCE);
        register(FireTimerElement.INSTANCE);
    }

    public static void register(DMMHudElement element) {
        if (element == null || ELEMENTS.contains(element)) return;
        ELEMENTS.add(element);
        // Let the UMM HUD framework handle dragging in its chat edit screen.
        Hud.INSTANCE.addElement(element);
    }

    public static void render(GuiGraphics graphics, float tickDelta) {
        for (DMMHudElement element : ELEMENTS) {
            if (!element.shouldRenderOverUI()) {
                element.renderAbsolute(graphics, tickDelta);
            }
        }
    }

    public static void renderOverUI(GuiGraphics graphics, float tickDelta) {
        for (DMMHudElement element : ELEMENTS) {
            if (element.shouldRenderOverUI()) {
                element.renderAbsolute(graphics, tickDelta);
            }
        }
    }
}
