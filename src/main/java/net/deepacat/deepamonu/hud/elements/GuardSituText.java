package net.deepacat.deepamonu.hud.elements;

import ch.njol.minecraft.uiframework.ElementPosition;
import ch.njol.minecraft.uiframework.hud.HudElement;
import net.deepacat.deepamonu.features.ShieldBlockHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class GuardSituText extends HudElement {
    public static final GuardSituText INSTANCE = new GuardSituText();

    private final ElementPosition pos = new ElementPosition(1.0F, 0, 0.0F, 20, 1.0F, 0.0F);

    private static final String DISPLAY_TEXT = "Guard: %guardtime%";
    private static final String REPLACER = "%guardtime%";

    private final int pad = 5;
    private final int textCol = 16733695;

    private Component buildText() {
        double guardTicks = (double) Math.max(ShieldBlockHelper.guardBlockTicks, ShieldBlockHelper.guardHitTicks) / 20;
        String formatted = String.format("%.1f", guardTicks);
        return Component.literal(DISPLAY_TEXT.replaceFirst(REPLACER, formatted));
    }

    @Override
    protected boolean isEnabled() { return true; }

    @Override
    protected boolean isVisible() { return true; }

    @Override
    protected int getWidth() {
        return Minecraft.getInstance().font.width(buildText()) + pad * 2;
    }

    @Override
    protected int getHeight() {
        return Minecraft.getInstance().font.lineHeight * 2;
    }

    @Override
    protected ElementPosition getPosition() { return pos; }

    @Override
    protected int getZOffset() { return 0; }

    @Override
    protected void render(GuiGraphics draw, float v) {
        Font fo = Minecraft.getInstance().font;
        Component text = buildText();
        int bgCol = Minecraft.getInstance().options.getBackgroundColor(0.3F);

        int x = pad;
        int y = fo.lineHeight / 2;
        int width = fo.width(text);

        draw.fill(0, 0, width + pad * 2, 18, bgCol);
        draw.drawString(fo, text, x, y, textCol, true);
    }
}