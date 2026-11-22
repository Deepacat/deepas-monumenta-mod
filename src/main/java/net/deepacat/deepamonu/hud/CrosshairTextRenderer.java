package net.deepacat.deepamonu.hud;

import net.deepacat.deepamonu.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
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

    /**
     * Renders color segments inline on a single logical line with horizontal alignment.
     * Unlike {@link #renderLines} (which stacks each TextLine vertically), all segments
     * in {@code segments} are drawn left-to-right on the same row so multiple
     * colors (e.g. "&6%n &f%cds &7(%ch)") stay inline instead of wrapping.
     * <p>
     * Alignment (-1 to 1): -1 = left-anchored (leftmost char stays exactly
     * at x/y, rest extends right), 0 = centered, 1 = right-anchored
     * (rightmost char stays exactly at x/y, rest extends left).
     * Fractional values interpolate between these anchors.
     * Explicit '\n' characters in segment text start a new row below.
     */
    public static void renderSingleLineAligned(GuiGraphics graphics, List<TextLine> segments, float xOffset, float yOffset, int zOffset, float scale, int alignment) {
        renderSingleLineAligned(graphics, segments, xOffset, yOffset, zOffset, scale, (float) alignment);
    }

    /**
     * Float-based variant: -1 = left, 0 = centered, 1 = right (clamped).
     * Matches the per-entry {@code alignment} convention used by compact abilities.
     */
    public static void renderSingleLineAligned(GuiGraphics graphics, List<TextLine> segments, float xOffset, float yOffset, int zOffset, float scale, float alignment) {
        if (segments == null || segments.isEmpty()) return;

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

        List<List<TextLine>> rows = splitOnNewlines(segments);
        if (rows.isEmpty()) return;

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, zOffset);

        for (int i = 0; i < rows.size(); i++) {
            List<TextLine> row = rows.get(i);
            if (row == null || row.isEmpty()) continue;

            float rowWidth = 0;
            for (TextLine seg : row) {
                if (seg != null && seg.text != null && !seg.text.isEmpty()) {
                    rowWidth += font.width(seg.text) * scale;
                }
            }
            if (rowWidth <= 0) continue;

            float x = baseX - rowWidth * alignFraction(alignment);
            float y = baseY + i * scaledLineHeight;

            for (TextLine seg : row) {
                if (seg == null || seg.text == null || seg.text.isEmpty()) continue;
                drawScaledOutlinedText(graphics, font, seg.text, x, y, seg.color, scale);
                x += font.width(seg.text) * scale;
            }
        }

        graphics.pose().popPose();
    }

    /**
     * Renders a list of text lines (each entry its own row) with horizontal
     * alignment. Uses the same top-anchored Y convention as
     * {@link #renderSingleLineAligned}: the first row starts exactly at
     * {@code centerY + yOffset} so identical x/y offsets line up across
     * displays (unlike legacy {@link #renderLines}, which vertically centers
     * the block and therefore sits half a line higher for a single line).
     * <p>
     * Alignment (-1 to 1): -1 = each row's left edge at x, 0 = centered on x,
     * 1 = each row's right edge at x. Fractional values interpolate.
     * <p>
     * Int-based variant: negative = left, 0 = centered, positive = right.
     * Matches the per-entry {@code alignment} convention used by compact abilities.
     */
    public static void renderLinesAligned(GuiGraphics graphics, List<TextLine> lines, float xOffset, float yOffset, int zOffset, float scale, int alignment) {
        renderLinesAligned(graphics, lines, xOffset, yOffset, zOffset, scale, (float) alignment);
    }

    /**
     * Float-based variant (-1 = left, 0 = centered, 1 = right, clamped).
     * Matches the per-entry {@code alignment} convention used by compact abilities.
     */
    public static void renderLinesAligned(GuiGraphics graphics, List<TextLine> lines, float xOffset, float yOffset, int zOffset, float scale, float alignment) {
        if (lines == null || lines.isEmpty()) return;

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

        int rowIndex = 0;
        for (TextLine line : lines) {
            if (line == null || line.text == null || line.text.isEmpty()) continue;
            // Split explicit newlines inside a line so they stack instead of rendering as tofu.
            String[] parts = line.text.split("\n", -1);
            for (String part : parts) {
                if (part.isEmpty()) {
                    rowIndex++;
                    continue;
                }
                float rowWidth = font.width(part) * scale;
                float x = baseX - rowWidth * alignFraction(alignment);
                float y = baseY + rowIndex * scaledLineHeight;
                drawScaledOutlinedText(graphics, font, part, x, y, line.color, scale);
                rowIndex++;
            }
        }

        graphics.pose().popPose();
    }

    /**
     * Renders inline segments at a local anchor. Unlike
     * {@link #renderSingleLineAligned}, the coordinates are not relative to the
     * screen centre, so this can be used from a HUD element that is already
     * translated into place.
     */
    public static void renderSegmentsAt(GuiGraphics graphics, List<TextLine> segments, float x, float y, float scale, float alignment, boolean shadow) {
        if (segments == null || segments.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int lineHeight = font.lineHeight + 1;
        float scaledLineHeight = lineHeight * scale;

        List<List<TextLine>> rows = splitOnNewlines(segments);
        for (int i = 0; i < rows.size(); i++) {
            List<TextLine> row = rows.get(i);
            if (row == null || row.isEmpty()) continue;

            float rowWidth = 0;
            for (TextLine seg : row) {
                if (seg != null && seg.text != null && !seg.text.isEmpty()) {
                    rowWidth += font.width(seg.text) * scale;
                }
            }
            if (rowWidth <= 0) continue;

            float drawX = x - rowWidth * alignFraction(alignment);
            float drawY = y + i * scaledLineHeight;
            for (TextLine seg : row) {
                if (seg == null || seg.text == null || seg.text.isEmpty()) continue;
                drawScaledText(graphics, font, seg.text, drawX, drawY, seg.color, scale, shadow);
                drawX += font.width(seg.text) * scale;
            }
        }
    }

    /**
     * Measures inline segments as if they were rendered by
     * {@link #renderSegmentsAt}.
     *
     * @return {minX, minY, maxX, maxY} relative to the same local origin
     */
    public static float[] measureSegmentsRelative(List<TextLine> segments, float x, float y, float scale, float alignment) {
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        if (segments == null || segments.isEmpty()) {
            return new float[]{x, y, x, y};
        }

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int lineHeight = font.lineHeight + 1;
        float scaledLineHeight = lineHeight * scale;

        List<List<TextLine>> rows = splitOnNewlines(segments);
        for (int i = 0; i < rows.size(); i++) {
            List<TextLine> row = rows.get(i);
            if (row == null || row.isEmpty()) continue;
            float rowWidth = 0;
            for (TextLine seg : row) {
                if (seg != null && seg.text != null && !seg.text.isEmpty()) {
                    rowWidth += font.width(seg.text) * scale;
                }
            }
            if (rowWidth <= 0) continue;

            float startX = x - rowWidth * alignFraction(alignment);
            float rowY = y + i * scaledLineHeight;
            minX = Math.min(minX, startX);
            maxX = Math.max(maxX, startX + rowWidth);
            minY = Math.min(minY, rowY);
            maxY = Math.max(maxY, rowY + font.lineHeight * scale);
        }

        if (minX == Float.MAX_VALUE) {
            return new float[]{x, y, x, y};
        }
        return new float[]{minX, minY, maxX, maxY};
    }

    /**
     * Renders lines inside a local box, aligning the text block horizontally and
     * vertically using 0..1 alignment values.
     */
    public static void renderLinesInBox(GuiGraphics graphics, List<TextLine> lines, float boxX, float boxY,
                                        float boxWidth, float boxHeight, float scale, float alignX, float alignY,
                                        boolean shadow) {
        if (lines == null || lines.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int lineHeight = font.lineHeight + 1;
        float scaledLineHeight = lineHeight * scale;

        List<TextLine> rows = new ArrayList<>();
        for (TextLine line : lines) {
            if (line == null || line.text == null) continue;
            String[] parts = line.text.split("\n", -1);
            for (String part : parts) {
                rows.add(new TextLine(part, line.color));
            }
        }
        if (rows.isEmpty()) return;

        float ax = Math.max(0f, Math.min(1f, alignX));
        float ay = Math.max(0f, Math.min(1f, alignY));
        float totalHeight = rows.size() * scaledLineHeight;
        float startY = boxY + (boxHeight - totalHeight) * ay;

        for (TextLine row : rows) {
            if (!row.text.isEmpty()) {
                float rowWidth = font.width(row.text) * scale;
                float drawX = boxX + (boxWidth - rowWidth) * ax;
                drawScaledText(graphics, font, row.text, drawX, startY, row.color, scale, shadow);
            }
            startY += scaledLineHeight;
        }
    }

    /**
     * Maps a -1 (left) to 1 (right) alignment value to the fraction of the row
     * width subtracted from the anchor: -1 -> 0 (left edge at x), 0 -> 0.5
     * (centered), 1 -> 1 (right edge at x). Values outside [-1, 1] are clamped.
     */
    private static float alignFraction(float alignment) {
        float clamped = Math.max(-1f, Math.min(1f, alignment));
        return (clamped + 1f) / 2f;
    }

    private static List<List<TextLine>> splitOnNewlines(List<TextLine> segments) {
        List<List<TextLine>> rows = new ArrayList<>();
        List<TextLine> current = new ArrayList<>();
        for (TextLine seg : segments) {
            if (seg == null || seg.text == null || seg.text.isEmpty()) continue;
            String text = seg.text;
            int start = 0;
            for (int i = 0; i < text.length(); i++) {
                if (text.charAt(i) == '\n') {
                    if (i > start) {
                        current.add(new TextLine(text.substring(start, i), seg.color));
                    }
                    rows.add(current);
                    current = new ArrayList<>();
                    start = i + 1;
                }
            }
            if (start < text.length()) {
                current.add(new TextLine(text.substring(start), seg.color));
            } else if (start == text.length() && text.endsWith("\n")) {
                // trailing newline already pushed a row; keep current empty for next row
            }
        }
        if (!current.isEmpty()) {
            rows.add(current);
        }
        if (rows.isEmpty() && !segments.isEmpty()) {
            rows.add(new ArrayList<>(segments));
        }
        return rows;
    }

    public static void drawScaledText(GuiGraphics graphics, Font font, String text, float x, float y, int color, float scale, boolean shadow) {
        if (shadow) {
            drawScaledOutlinedText(graphics, font, text, x, y, color, scale);
            return;
        }

        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        if (scale != 1.0f) {
            graphics.pose().scale(scale, scale, 1.0f);
        }
        graphics.drawString(font, text, 0, 0, color, false);
        graphics.pose().popPose();
    }

    public static void drawScaledOutlinedText(GuiGraphics graphics, Font font, String text, float x, float y, int color, float scale) {
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

    private static boolean usesCharIcons(ModConfig.Crosshair.IconCharStyle iconStyle, String filledCharCustom, String emptyCharCustom) {
        return iconStyle != ModConfig.Crosshair.IconCharStyle.BLOCKS
                || (filledCharCustom != null && !filledCharCustom.isEmpty())
                || (emptyCharCustom != null && !emptyCharCustom.isEmpty());
    }

    public static int getAmmoIconCellWidth(float scale, ModConfig.Crosshair.IconCharStyle iconStyle, String filledCharCustom, String emptyCharCustom) {
        if (!usesCharIcons(iconStyle, filledCharCustom, emptyCharCustom)) {
            return Math.max(1, (int) (8 * scale));
        }
        Font font = Minecraft.getInstance().font;
        String filled = filledCharCustom != null && !filledCharCustom.isEmpty() ? filledCharCustom : getDefaultFilledChar(iconStyle);
        String empty = emptyCharCustom != null && !emptyCharCustom.isEmpty() ? emptyCharCustom : getDefaultEmptyChar(iconStyle);
        return Math.max(1, (int) Math.ceil(Math.max(font.width(filled), font.width(empty)) * scale));
    }

    public static int getAmmoIconCellHeight(float scale, ModConfig.Crosshair.IconCharStyle iconStyle, String filledCharCustom, String emptyCharCustom) {
        if (!usesCharIcons(iconStyle, filledCharCustom, emptyCharCustom)) {
            return Math.max(1, (int) (8 * scale));
        }
        return Math.max(1, (int) Math.ceil(Minecraft.getInstance().font.lineHeight * scale));
    }

    public static int getAmmoIconGap(float scale, ModConfig.Crosshair.IconCharStyle iconStyle, String filledCharCustom, String emptyCharCustom) {
        if (!usesCharIcons(iconStyle, filledCharCustom, emptyCharCustom)) {
            return Math.max(0, (int) (2 * scale));
        }
        return 0;
    }

    public static int getVisibleAmmoCount(int current, int max, boolean hideEmptySlots) {
        return hideEmptySlots ? Math.max(0, Math.min(current, max)) : Math.max(0, max);
    }

    public static int getAmmoIconsWidth(int visibleCount, float scale, boolean vertical, ModConfig.Crosshair.IconCharStyle iconStyle, String filledCharCustom, String emptyCharCustom) {
        if (visibleCount <= 0) return 0;
        int cell = getAmmoIconCellWidth(scale, iconStyle, filledCharCustom, emptyCharCustom);
        int gap = getAmmoIconGap(scale, iconStyle, filledCharCustom, emptyCharCustom);
        return vertical ? cell : visibleCount * cell + Math.max(0, visibleCount - 1) * gap;
    }

    public static int getAmmoIconsHeight(int visibleCount, float scale, boolean vertical, ModConfig.Crosshair.IconCharStyle iconStyle, String filledCharCustom, String emptyCharCustom) {
        if (visibleCount <= 0) return 0;
        int cell = getAmmoIconCellHeight(scale, iconStyle, filledCharCustom, emptyCharCustom);
        int gap = getAmmoIconGap(scale, iconStyle, filledCharCustom, emptyCharCustom);
        return vertical ? visibleCount * cell + Math.max(0, visibleCount - 1) * gap : cell;
    }

    /**
     * Draws ammo icons inside a local box. Coordinates are relative to the HUD
     * element, and the icon block is aligned within the box using 0..1 values.
     */
    public static void renderAmmoIconsInBox(GuiGraphics graphics, int current, int max, float boxX, float boxY,
                                            float boxWidth, float boxHeight, float scale, int fullColor, int midColor,
                                            int emptyColor, int emptySlotColor, ModConfig.Crosshair.IconCharStyle iconStyle,
                                            String filledCharCustom, String emptyCharCustom, boolean vertical,
                                            boolean hideEmptySlots, ModConfig.Crosshair.ShadowType shadowType,
                                            float alignX, float alignY) {
        int visibleCount = getVisibleAmmoCount(current, max, hideEmptySlots);
        if (visibleCount <= 0) return;

        int cellW = getAmmoIconCellWidth(scale, iconStyle, filledCharCustom, emptyCharCustom);
        int cellH = getAmmoIconCellHeight(scale, iconStyle, filledCharCustom, emptyCharCustom);
        int gap = getAmmoIconGap(scale, iconStyle, filledCharCustom, emptyCharCustom);
        int totalW = getAmmoIconsWidth(visibleCount, scale, vertical, iconStyle, filledCharCustom, emptyCharCustom);
        int totalH = getAmmoIconsHeight(visibleCount, scale, vertical, iconStyle, filledCharCustom, emptyCharCustom);

        float ax = Math.max(0f, Math.min(1f, alignX));
        float ay = Math.max(0f, Math.min(1f, alignY));
        float baseX = boxX + (boxWidth - totalW) * ax;
        float baseY = boxY + (boxHeight - totalH) * ay;

        int filledColorValue = MultiloadDisplay.getAmmoColor(fullColor, midColor, emptyColor, current, max);
        filledColorValue |= 0xFF000000;
        int slotColor = emptySlotColor | 0xFF000000;

        boolean charIcons = usesCharIcons(iconStyle, filledCharCustom, emptyCharCustom);
        Font font = Minecraft.getInstance().font;
        String filledChar = filledCharCustom != null && !filledCharCustom.isEmpty() ? filledCharCustom : getDefaultFilledChar(iconStyle);
        String emptyChar = emptyCharCustom != null && !emptyCharCustom.isEmpty() ? emptyCharCustom : getDefaultEmptyChar(iconStyle);

        graphics.pose().pushPose();
        for (int i = 0; i < visibleCount; i++) {
            float x = vertical ? baseX : baseX + i * (cellW + gap);
            float y = vertical ? baseY + i * (cellH + gap) : baseY;
            boolean filled = i < current;

            if (charIcons) {
                String ch = filled ? filledChar : emptyChar;
                int color = filled ? filledColorValue : slotColor;
                graphics.pose().pushPose();
                graphics.pose().translate(x, y, 0);
                if (scale != 1.0f) {
                    graphics.pose().scale(scale, scale, 1.0f);
                }
                drawCharWithShadow(graphics, font, ch, 0, 0, color, shadowType);
                graphics.pose().popPose();
            } else if (filled) {
                graphics.fill((int) x, (int) y, (int) x + cellW, (int) y + cellH, filledColorValue);
                graphics.fill((int) x - 1, (int) y - 1, (int) x + cellW + 1, (int) y, 0xFF000000);
                graphics.fill((int) x - 1, (int) y + cellH, (int) x + cellW + 1, (int) y + cellH + 1, 0xFF000000);
                graphics.fill((int) x - 1, (int) y, (int) x, (int) y + cellH, 0xFF000000);
                graphics.fill((int) x + cellW, (int) y, (int) x + cellW + 1, (int) y + cellH, 0xFF000000);
            } else {
                graphics.fill((int) x, (int) y, (int) x + cellW, (int) y + 1, slotColor);
                graphics.fill((int) x, (int) y + cellH - 1, (int) x + cellW, (int) y + cellH, slotColor);
                graphics.fill((int) x, (int) y, (int) x + 1, (int) y + cellH, slotColor);
                graphics.fill((int) x + cellW - 1, (int) y, (int) x + cellW, (int) y + cellH, slotColor);
            }
        }
        graphics.pose().popPose();
    }

    private static float alignmentFraction(ModConfig.Crosshair.Alignment alignment) {
        if (alignment == null) return 0.5f;
        return switch (alignment) {
            case LEFT -> 0f;
            case RIGHT -> 1f;
            default -> 0.5f;
        };
    }

    public static void renderAmmoIcons(GuiGraphics graphics, int current, int max, float xOffset, float yOffset, int zOffset, float scale, int fullColor, int midColor, int emptyColor, int emptySlotColor, ModConfig.Crosshair.IconCharStyle iconStyle, String filledCharCustom, String emptyCharCustom, boolean vertical, boolean hideEmptySlots, ModConfig.Crosshair.Alignment alignment, ModConfig.Crosshair.ShadowType shadowType) {
        if (max <= 0) return;

        Minecraft mc = Minecraft.getInstance();
        int centerX = mc.getWindow().getGuiScaledWidth() / 2;
        int centerY = mc.getWindow().getGuiScaledHeight() / 2;

        int visibleCount = getVisibleAmmoCount(current, max, hideEmptySlots);
        int totalW = getAmmoIconsWidth(visibleCount, scale, vertical, iconStyle, filledCharCustom, emptyCharCustom);
        int totalH = getAmmoIconsHeight(visibleCount, scale, vertical, iconStyle, filledCharCustom, emptyCharCustom);

        float ax = 0.5f;
        float ay = 0.5f;
        if (hideEmptySlots) {
            if (vertical) {
                ay = alignmentFraction(alignment);
            } else {
                ax = alignmentFraction(alignment);
            }
        }

        float boxX = centerX + xOffset - totalW * ax;
        float boxY = centerY + yOffset - totalH * ay;

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, zOffset);
        renderAmmoIconsInBox(graphics, current, max, boxX, boxY, totalW, totalH, scale,
                fullColor, midColor, emptyColor, emptySlotColor, iconStyle, filledCharCustom,
                emptyCharCustom, vertical, hideEmptySlots, shadowType, 0f, 0f);
        graphics.pose().popPose();
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
