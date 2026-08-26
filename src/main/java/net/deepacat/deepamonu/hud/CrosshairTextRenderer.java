package net.deepacat.deepamonu.hud;

import net.deepacat.deepamonu.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CrosshairTextRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger("CrosshairHud");
    private static long lastRendererLogTime = 0;

    public static class TextLine {
        public final String text;
        public final int color;

        public TextLine(String text, int color) {
            this.text = text;
            this.color = color;
        }

        public TextLine(String text) {
            this(text, 0xFFFFFFFF);
        }
    }

    public static void renderLines(GuiGraphics graphics, List<TextLine> lines, float xOffset, float yOffset, int zOffset, float scale) {
        if (lines.isEmpty()) return;

        long now = System.currentTimeMillis();
        if (now - lastRendererLogTime > 2000) {
            LOGGER.debug("[CrosshairRenderer] renderLines called: {} lines, xOff={}, yOff={}, zOff={}, scale={}",
                    lines.size(), xOffset, yOffset, zOffset, scale);
            for (TextLine line : lines) {
                LOGGER.debug("[CrosshairRenderer]   '{}' color={}", line.text, Integer.toHexString(line.color));
            }
            lastRendererLogTime = now;
        }

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;

        int lineHeight = font.lineHeight + 1;
        float scaledLineHeight = lineHeight * scale;
        float totalHeight = scaledLineHeight * lines.size();
        float baseX = centerX + xOffset;
        float baseY = centerY + yOffset;

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, zOffset);

        for (int i = 0; i < lines.size(); i++) {
            TextLine line = lines.get(i);
            if (line == null || line.text == null || line.text.isEmpty()) continue;

            float y = baseY + i * scaledLineHeight - totalHeight / 2f;
            float textWidth = font.width(line.text) * scale;
            float x = baseX - textWidth / 2f;

            drawScaledOutlinedText(graphics, font, line.text, x, y, line.color, scale);
        }

        graphics.pose().popPose();
    }

    public static void renderSegmentedLines(GuiGraphics graphics, List<List<TextLine>> lineSegments, float xOffset, float yOffset, int zOffset, float scale) {
        if (lineSegments.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;

        int lineHeight = font.lineHeight + 1;
        float scaledLineHeight = lineHeight * scale;
        float totalHeight = scaledLineHeight * lineSegments.size();
        float baseX = centerX + xOffset;
        float baseY = centerY + yOffset;

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, zOffset);

        for (int i = 0; i < lineSegments.size(); i++) {
            List<TextLine> segments = lineSegments.get(i);
            if (segments == null || segments.isEmpty()) continue;

            float y = baseY + i * scaledLineHeight - totalHeight / 2f;
            float totalWidth = 0;
            for (TextLine seg : segments) {
                if (seg.text != null) totalWidth += font.width(seg.text) * scale;
            }
            float x = baseX - totalWidth / 2f;

            for (TextLine seg : segments) {
                if (seg.text == null || seg.text.isEmpty()) continue;
                drawScaledOutlinedText(graphics, font, seg.text, x, y, seg.color, scale);
                x += font.width(seg.text) * scale;
            }
        }

        graphics.pose().popPose();
    }

    public static void renderLinesLeftAligned(GuiGraphics graphics, List<TextLine> lines, float xOffset, float yOffset, int zOffset, float scale) {
        if (lines.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;

        int lineHeight = font.lineHeight + 1;
        float scaledLineHeight = lineHeight * scale;
        float baseX = centerX + xOffset;
        float baseY = centerY + yOffset;

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, zOffset);

        for (int i = 0; i < lines.size(); i++) {
            TextLine line = lines.get(i);
            if (line == null || line.text == null || line.text.isEmpty()) continue;

            float y = baseY + i * scaledLineHeight;
            drawScaledOutlinedText(graphics, font, line.text, baseX, y, line.color, scale);
        }

        graphics.pose().popPose();
    }

    private static void drawScaledOutlinedText(GuiGraphics graphics, Font font, String text, float x, float y, int color, float scale) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        if (scale != 1.0f) {
            graphics.pose().scale(scale, scale, 1.0f);
        }

        int outlineColor = 0xFF000000;
        graphics.drawString(font, text, 1, 0, outlineColor, false);
        graphics.drawString(font, text, -1, 0, outlineColor, false);
        graphics.drawString(font, text, 0, 1, outlineColor, false);
        graphics.drawString(font, text, 0, -1, outlineColor, false);
        graphics.drawString(font, text, 0, 0, color, false);

        graphics.pose().popPose();
    }

    public static void renderAmmoIcons(GuiGraphics graphics, int current, int max, float xOffset, float yOffset, int zOffset, float scale, int fullColor, int midColor, int emptyColor, int emptySlotColor, ModConfig.Crosshair.IconCharStyle iconStyle, String filledCharCustom, String emptyCharCustom, boolean vertical, boolean hideEmptySlots, ModConfig.Crosshair.Alignment alignment, ModConfig.Crosshair.ShadowType shadowType) {
        if (max <= 0) return;

        boolean hasCustomChars = (filledCharCustom != null && !filledCharCustom.isEmpty())
                || (emptyCharCustom != null && !emptyCharCustom.isEmpty());

        if (iconStyle != ModConfig.Crosshair.IconCharStyle.BLOCKS || hasCustomChars) {
            renderCharIcons(graphics, current, max, xOffset, yOffset, zOffset, scale, fullColor, midColor, emptyColor, emptySlotColor, iconStyle, filledCharCustom, emptyCharCustom, vertical, hideEmptySlots, alignment, shadowType);
            return;
        }

        renderBlockIcons(graphics, current, max, xOffset, yOffset, zOffset, scale, fullColor, midColor, emptyColor, emptySlotColor, vertical, hideEmptySlots, alignment);
    }

    private static void renderBlockIcons(GuiGraphics graphics, int current, int max, float xOffset, float yOffset, int zOffset, float scale, int fullColor, int midColor, int emptyColor, int emptySlotColor, boolean vertical, boolean hideEmptySlots, ModConfig.Crosshair.Alignment alignment) {

        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;

        int iconSize = (int) (8 * scale);
        int gap = (int) (2 * scale);

        int filledColor = MultiloadDisplay.getAmmoColor(fullColor, midColor, emptyColor, current, max);
        filledColor |= 0xFF000000;
        int slotColor = emptySlotColor | 0xFF000000;

        int visibleCount = hideEmptySlots ? current : max;

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, zOffset);

        if (vertical) {
            int totalHeight = visibleCount * iconSize + (visibleCount - 1) * gap;
            float baseX = centerX + xOffset - iconSize / 2f;
            float baseY;
            if (hideEmptySlots) {
                switch (alignment) {
                    case LEFT:  baseY = centerY + yOffset; break;
                    case RIGHT: baseY = centerY + yOffset - totalHeight; break;
                    default:    baseY = centerY + yOffset - totalHeight / 2f; break;
                }
            } else {
                baseY = centerY + yOffset - totalHeight / 2f;
            }

            for (int i = 0; i < max; i++) {
                if (hideEmptySlots && i >= current) continue;
                int x = (int) baseX;
                int y = (int) (baseY + i * (iconSize + gap));
                if (i < current) {
                    graphics.fill(x, y, x + iconSize, y + iconSize, filledColor);
                    graphics.fill(x - 1, y - 1, x + iconSize + 1, y, 0xFF000000);
                    graphics.fill(x - 1, y + iconSize, x + iconSize + 1, y + iconSize + 1, 0xFF000000);
                    graphics.fill(x - 1, y, x, y + iconSize, 0xFF000000);
                    graphics.fill(x + iconSize, y, x + iconSize + 1, y + iconSize, 0xFF000000);
                } else {
                    graphics.fill(x, y, x + iconSize, y + 1, slotColor);
                    graphics.fill(x, y + iconSize - 1, x + iconSize, y + iconSize, slotColor);
                    graphics.fill(x, y, x + 1, y + iconSize, slotColor);
                    graphics.fill(x + iconSize - 1, y, x + iconSize, y + iconSize, slotColor);
                }
            }
        } else {
            int totalWidth = visibleCount * iconSize + (visibleCount - 1) * gap;
            float baseY = centerY + yOffset - iconSize / 2f;
            float baseX;
            if (hideEmptySlots) {
                switch (alignment) {
                    case LEFT:   baseX = centerX + xOffset; break;
                    case RIGHT:  baseX = centerX + xOffset - totalWidth; break;
                    default:     baseX = centerX + xOffset - totalWidth / 2f; break;
                }
            } else {
                baseX = centerX + xOffset - totalWidth / 2f;
            }

            for (int i = 0; i < max; i++) {
                if (hideEmptySlots && i >= current) continue;
                int x = (int) (baseX + i * (iconSize + gap));
                int y = (int) baseY;
                if (i < current) {
                    graphics.fill(x, y, x + iconSize, y + iconSize, filledColor);
                    graphics.fill(x - 1, y - 1, x + iconSize + 1, y, 0xFF000000);
                    graphics.fill(x - 1, y + iconSize, x + iconSize + 1, y + iconSize + 1, 0xFF000000);
                    graphics.fill(x - 1, y, x, y + iconSize, 0xFF000000);
                    graphics.fill(x + iconSize, y, x + iconSize + 1, y + iconSize, 0xFF000000);
                } else {
                    graphics.fill(x, y, x + iconSize, y + 1, slotColor);
                    graphics.fill(x, y + iconSize - 1, x + iconSize, y + iconSize, slotColor);
                    graphics.fill(x, y, x + 1, y + iconSize, slotColor);
                    graphics.fill(x + iconSize - 1, y, x + iconSize, y + iconSize, slotColor);
                }
            }
        }

        graphics.pose().popPose();
    }

    private static void renderCharIcons(GuiGraphics graphics, int current, int max, float xOffset, float yOffset, int zOffset, float scale, int fullColor, int midColor, int emptyColor, int emptySlotColor, ModConfig.Crosshair.IconCharStyle iconStyle, String filledCharCustom, String emptyCharCustom, boolean vertical, boolean hideEmptySlots, ModConfig.Crosshair.Alignment alignment, ModConfig.Crosshair.ShadowType shadowType) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        String filledChar = filledCharCustom != null && !filledCharCustom.isEmpty() ? filledCharCustom : getDefaultFilledChar(iconStyle);
        String emptyChar = emptyCharCustom != null && !emptyCharCustom.isEmpty() ? emptyCharCustom : getDefaultEmptyChar(iconStyle);

        int filledColor = MultiloadDisplay.getAmmoColor(fullColor, midColor, emptyColor, current, max);
        filledColor |= 0xFF000000;
        int slotColor = emptySlotColor | 0xFF000000;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;

        float charWidth = font.width(filledChar) * scale;
        float charHeight = font.lineHeight * scale;

        int visibleCount = hideEmptySlots ? current : max;

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, zOffset);

        if (vertical) {
            float totalHeight = visibleCount * charHeight;
            float baseX = centerX + xOffset - charWidth / 2f;
            float baseY;
            if (hideEmptySlots) {
                switch (alignment) {
                    case LEFT:  baseY = centerY + yOffset; break;
                    case RIGHT: baseY = centerY + yOffset - totalHeight; break;
                    default:    baseY = centerY + yOffset - totalHeight / 2f; break;
                }
            } else {
                baseY = centerY + yOffset - totalHeight / 2f;
            }

            for (int i = 0; i < max; i++) {
                if (hideEmptySlots && i >= current) continue;
                float y = baseY + i * charHeight;
                String ch = i < current ? filledChar : emptyChar;
                int color = i < current ? filledColor : slotColor;

                graphics.pose().pushPose();
                graphics.pose().translate(baseX, y, 0);
                if (scale != 1.0f) {
                    graphics.pose().scale(scale, scale, 1.0f);
                }
                drawCharWithShadow(graphics, font, ch, 0, 0, color, shadowType);
                graphics.pose().popPose();
            }
        } else {
            float totalWidth = visibleCount * charWidth;
            float baseY = centerY + yOffset - charHeight / 2f;
            float baseX;
            if (hideEmptySlots) {
                switch (alignment) {
                    case LEFT:   baseX = centerX + xOffset; break;
                    case RIGHT:  baseX = centerX + xOffset - totalWidth; break;
                    default:     baseX = centerX + xOffset - totalWidth / 2f; break;
                }
            } else {
                baseX = centerX + xOffset - totalWidth / 2f;
            }

            for (int i = 0; i < max; i++) {
                if (hideEmptySlots && i >= current) continue;
                float x = baseX + i * charWidth;
                String ch = i < current ? filledChar : emptyChar;
                int color = i < current ? filledColor : slotColor;

                graphics.pose().pushPose();
                graphics.pose().translate(x, baseY, 0);
                if (scale != 1.0f) {
                    graphics.pose().scale(scale, scale, 1.0f);
                }
                drawCharWithShadow(graphics, font, ch, 0, 0, color, shadowType);
                graphics.pose().popPose();
            }
        }

        graphics.pose().popPose();
    }

    private static void drawCharWithShadow(GuiGraphics graphics, Font font, String ch, int x, int y, int color, ModConfig.Crosshair.ShadowType shadowType) {
        if (shadowType == ModConfig.Crosshair.ShadowType.OUTLINE) {
            int outline = 0xFF000000;
            graphics.drawString(font, ch, x + 1, y, outline, false);
            graphics.drawString(font, ch, x - 1, y, outline, false);
            graphics.drawString(font, ch, x, y + 1, outline, false);
            graphics.drawString(font, ch, x, y - 1, outline, false);
        }
        graphics.drawString(font, ch, x, y, color, shadowType == ModConfig.Crosshair.ShadowType.VANILLA);
    }

    private static String getDefaultFilledChar(ModConfig.Crosshair.IconCharStyle style) {
        return style == ModConfig.Crosshair.IconCharStyle.CIRCLES ? "\u25CF" : "\u25A0";
    }

    private static String getDefaultEmptyChar(ModConfig.Crosshair.IconCharStyle style) {
        return style == ModConfig.Crosshair.IconCharStyle.CIRCLES ? "\u25CB" : "\u25A1";
    }

    public static void renderTimerBar(GuiGraphics graphics, long elapsedMs, long durationMs, float xOffset, float yOffset, int barWidth, int barHeight, int barColor, int bgColor, boolean vertical) {
        if (durationMs <= 0) return;

        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;

        float progress = 1f - Math.min(1f, (float) elapsedMs / durationMs);
        if (progress <= 0) return;

        int x = (int) (centerX + xOffset - barWidth / 2f);
        int y = (int) (centerY + yOffset - barHeight / 2f);
        int borderColor = 0xFF000000;

        if (vertical) {
            int filledHeight = Math.max(0, (int) (barHeight * progress));
            // border
            graphics.fill(x - 1, y - 1, x + barWidth + 1, y + barHeight + 1, borderColor);
            // background
            graphics.fill(x, y, x + barWidth, y + barHeight, bgColor | 0xFF000000);
            // filled (drains from top)
            if (filledHeight > 0) {
                graphics.fill(x, y + barHeight - filledHeight, x + barWidth, y + barHeight, barColor | 0xFF000000);
            }
        } else {
            int filledWidth = Math.max(0, (int) (barWidth * progress));
            graphics.fill(x - 1, y - 1, x + barWidth + 1, y + barHeight + 1, borderColor);
            graphics.fill(x, y, x + barWidth, y + barHeight, bgColor | 0xFF000000);
            if (filledWidth > 0) {
                graphics.fill(x, y, x + filledWidth, y + barHeight, barColor | 0xFF000000);
            }
        }
    }
}
