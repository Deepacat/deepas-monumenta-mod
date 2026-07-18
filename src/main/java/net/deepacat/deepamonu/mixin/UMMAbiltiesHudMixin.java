package net.deepacat.deepamonu.mixin;

import ch.njol.minecraft.uiframework.hud.HudElement;
import ch.njol.unofficialmonumentamod.AbilityHandler;
import ch.njol.unofficialmonumentamod.hud.AbilitiesHud;
import ch.njol.unofficialmonumentamod.options.Options;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.deepacat.deepamonu.DMMClient;
import net.deepacat.deepamonu.compat.TriggerCache;
import net.deepacat.deepamonu.compat.IndicatorLine;
import net.deepacat.deepamonu.compat.TextSegment;
import net.deepacat.deepamonu.config.ModConfig;
import net.deepacat.deepamonu.utils.GuiTextures;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(AbilitiesHud.class)
public abstract class UMMAbiltiesHudMixin {

    @Unique private static final int VANILLA_GREEN = 0xFF55FF55;
    @Unique private static final int VANILLA_RED   = 0xFFFF5555;
    @Unique private static final int PADDING = 2;
    @Unique private static final int FIXED_Y_OFFSET = -16;
    @Unique private static final int FIXED_MODIFIER_Y_OFFSET = -10;
    @Unique private static final int FIXED_BG_Y_OFFSET = 8;
    @Unique private static final int FIXED_BG_EXTRA = 1;

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blit(IIIIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V",
                    ordinal = 0
            )
    )
    private void addKeybindText(
            GuiGraphics drawContext,
            float tickDelta,
            CallbackInfo ci,
            @Local(ordinal = 0) Options options,
            @Local(ordinal = 0) int iconSize,
            @Local(index = 14) int iconX,
            @Local(index = 15) int iconY,
            @Local(ordinal = 0) AbilityHandler.AbilityInfo abilityInfo
    ) {
        ModConfig config = DMMClient.config();
        if (config == null || !config.modtweaks.umm.triggerOverlay.enabled) {
            restoreRenderState();
            return;
        }
        var cfg = config.modtweaks.umm.triggerOverlay;

        HudElement hudElement = (HudElement) (Object) this;
        String currentClass = abilityInfo.className;
        if (currentClass == null) {
            restoreRenderState();
            return;
        }

        TriggerCache.ClassTriggers triggers = TriggerCache.loadClassTriggers(currentClass);
        if (triggers == null) {
            restoreRenderState();
            return;
        }

        // Hardcoded first, then auto – both lookups are now case‑insensitive
        TriggerCache.TriggerEntry trigger = TriggerCache.getCaseInsensitive(triggers.hardcoded, abilityInfo.name);
        if (trigger == null) {
            trigger = TriggerCache.getCaseInsensitive(triggers.triggers, abilityInfo.name);
        }
        if (trigger == null || (trigger.keyBindingName == null && trigger.displayString == null)) {
            restoreRenderState();
            return;
        }
        if (trigger.suppressDisplay) {
            restoreRenderState();
            return;
        }

        // --- Key text / icon preparation ---
        String keyText = "";
        List<TextSegment> formattedSegments = null;
        int totalKeyWidth = 0;
        boolean useIcon = cfg.useIcons && (trigger.displayString == null || trigger.displayString.isEmpty());

        if (trigger.displayString != null && !trigger.displayString.isEmpty()) {
            int defaultColor = trigger.displayColor != 0 ? trigger.displayColor : 0xFFFFFFFF;
            String resolved = expandKeyPlaceholders(trigger.displayString);
            formattedSegments = parseFormattingSegments(resolved, defaultColor);
            for (TextSegment seg : formattedSegments) {
                totalKeyWidth += Minecraft.getInstance().font.width(seg.text);
            }
        } else if (useIcon && "key.attack".equals(trigger.keyBindingName)) {
            totalKeyWidth = 16;
        } else if (useIcon && "key.use".equals(trigger.keyBindingName)) {
            totalKeyWidth = 16;
        } else {
            if (trigger.rawTrigger != null && trigger.rawTrigger.equalsIgnoreCase("Shoot Projectile")) {
                keyText = "🏹";
            } else {
                KeyMapping mapping = null;
                for (KeyMapping km : Minecraft.getInstance().options.keyMappings) {
                    if (km.getName().equals(trigger.keyBindingName)) {
                        mapping = km;
                        break;
                    }
                }
                if (mapping != null && mapping.key.getType() == com.mojang.blaze3d.platform.InputConstants.Type.MOUSE) {
                    int mouseButton = mapping.key.getValue();
                    keyText = switch (mouseButton) {
                        case GLFW.GLFW_MOUSE_BUTTON_1 -> "M1";
                        case GLFW.GLFW_MOUSE_BUTTON_2 -> "M2";
                        case GLFW.GLFW_MOUSE_BUTTON_3 -> "M3";
                        case GLFW.GLFW_MOUSE_BUTTON_4 -> "M4";
                        case GLFW.GLFW_MOUSE_BUTTON_5 -> "M5";
                        default -> mapping.getTranslatedKeyMessage().getString();
                    };
                } else if (mapping != null) {
                    keyText = mapping.getTranslatedKeyMessage().getString();
                } else {
                    keyText = "?";
                }
            }
            if (trigger.doubleClick) keyText += "²";
            totalKeyWidth = Minecraft.getInstance().font.width(keyText);
        }

        // Arrow prefix (no space)
        String arrow = "";
        int arrowColor = 0xFFFFFFFF;
        if (trigger.requireSneaking) {
            arrow = "↓";
            arrowColor = VANILLA_GREEN;
        } else if (trigger.requireNotSneaking) {
            arrow = "↑";
            arrowColor = VANILLA_RED;
        }

        int arrowWidth = !arrow.isEmpty() ? Minecraft.getInstance().font.width(arrow) : 0;
        int totalWidth = arrowWidth + totalKeyWidth;
        int startX = iconX + (iconSize - totalWidth) / 2 + cfg.xOffset;
        int baseY = iconY + cfg.yOffset + FIXED_Y_OFFSET;
        int keyBaseY = baseY;
        if (trigger.displayOffsetY != 0 && trigger.displayString != null && !trigger.displayString.isEmpty()) {
            keyBaseY += trigger.displayOffsetY;
        }

        // --- Modifier indicators ---
        List<IndicatorLine> indicators = new ArrayList<>();
        if (cfg.modifiersEnabled && trigger.lookDirection != null) {
            indicators.add(new IndicatorLine(lookDirToArrow(trigger.lookDirection), 0xFF55FFFF));
        }
        if (cfg.modifiersEnabled && trigger.hasSprintCondition) {
            indicators.add(new IndicatorLine("»", trigger.requireSprinting ? VANILLA_GREEN : VANILLA_RED));
        }
        if (cfg.modifiersEnabled && trigger.hasGroundCondition) {
            indicators.add(new IndicatorLine("⚓", trigger.requireOnGround ? VANILLA_GREEN : VANILLA_RED));
        }

        int fontHeight = Minecraft.getInstance().font.lineHeight;
        int indicatorLineWidth = 0;
        if (!indicators.isEmpty()) {
            int spaceWidth = Minecraft.getInstance().font.width(" ");
            for (IndicatorLine ind : indicators) {
                indicatorLineWidth += Minecraft.getInstance().font.width(ind.text);
            }
            indicatorLineWidth += (indicators.size() - 1) * spaceWidth;
        }

        int blockWidth = Math.max(totalWidth, indicatorLineWidth);
        int modifierLineY;
        int blockTop, blockBottom;

        if (cfg.modifiersBelowKeyLine) {
            modifierLineY = baseY + fontHeight + cfg.modifierYOffset + FIXED_MODIFIER_Y_OFFSET;
        } else {
            modifierLineY = baseY + cfg.modifierYOffset + FIXED_MODIFIER_Y_OFFSET;
        }

        int keyTop = keyBaseY - fontHeight;
        int keyBottom = keyBaseY;

        if (!indicators.isEmpty()) {
            int modifierTop = modifierLineY - fontHeight;
            blockTop = Math.min(keyTop, modifierTop);
            blockBottom = Math.max(keyBottom, modifierLineY);
        } else {
            blockTop = keyTop;
            blockBottom = keyBottom;
        }
        int blockHeight = blockBottom - blockTop;

        // Background
        if (cfg.backgroundEnabled) {
            int autoW = blockWidth + 2 * PADDING;
            int autoH = blockHeight + 2 * PADDING;
            int bgW = autoW + cfg.backgroundWidth + FIXED_BG_EXTRA;
            int bgH = autoH + cfg.backgroundHeight + FIXED_BG_EXTRA;
            int bgX = iconX + (iconSize - bgW) / 2 + cfg.backgroundXOffset;
            int bgY = blockTop - PADDING + cfg.backgroundYOffset + FIXED_BG_Y_OFFSET + trigger.backgroundOffsetY;
            drawRoundedRect(drawContext, bgX, bgY, bgW, bgH, cfg.backgroundColor, cfg.backgroundCornerRadius);
        }

        // Draw modifiers
        if (!indicators.isEmpty()) {
            int spaceWidth = Minecraft.getInstance().font.width(" ");
            int currentX = iconX + (iconSize - indicatorLineWidth) / 2 + cfg.modifierXOffset;
            for (int i = 0; i < indicators.size(); i++) {
                IndicatorLine ind = indicators.get(i);
                hudElement.drawOutlinedText(drawContext, ind.text, currentX, modifierLineY, ind.color);
                currentX += Minecraft.getInstance().font.width(ind.text);
                if (i < indicators.size() - 1) currentX += spaceWidth;
            }
        }

        // Draw arrow + key
        int keyLineStartX = startX + arrowWidth;
        if (trigger.displayOffsetX != 0 && trigger.displayString != null && !trigger.displayString.isEmpty()) {
            keyLineStartX += trigger.displayOffsetX;
        }

        if (!arrow.isEmpty()) {
            hudElement.drawOutlinedText(drawContext, arrow, startX, keyBaseY, arrowColor);
        }
        if (useIcon && formattedSegments == null && "key.attack".equals(trigger.keyBindingName)) {
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            GuiTextures.LEFT_CLICK.render(drawContext, keyLineStartX, keyBaseY - 8);
        } else if (useIcon && formattedSegments == null && "key.use".equals(trigger.keyBindingName)) {
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            GuiTextures.RIGHT_CLICK.render(drawContext, keyLineStartX, keyBaseY - 8);
        } else if (formattedSegments != null) {
            int cx = keyLineStartX;
            for (TextSegment seg : formattedSegments) {
                hudElement.drawOutlinedText(drawContext, seg.text, cx, keyBaseY, seg.color);
                cx += Minecraft.getInstance().font.width(seg.text);
            }
        } else {
            hudElement.drawOutlinedText(drawContext, keyText, keyLineStartX, keyBaseY, 0xFFFFFFFF);
        }

        restoreRenderState();
    }

    @Unique
    private void restoreRenderState() {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    @Unique
    private String lookDirToArrow(String dir) {
        return switch (dir) {
            case "DOWN" -> "↓";
            case "LEVEL" -> "→";
            case "UP" -> "↑";
            case "DOWN_OR_LEVEL" -> "↘";
            case "DOWN_OR_UP" -> "⇵";
            case "LEVEL_OR_UP" -> "↗";
            default -> "?";
        };
    }

    @Unique
    private List<TextSegment> parseFormattingSegments(String text, int defaultColor) {
        List<TextSegment> segments = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int currentColor = defaultColor;
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                if (!current.isEmpty()) {
                    segments.add(new TextSegment(current.toString(), currentColor));
                    current.setLength(0);
                }
                char code = text.charAt(i + 1);
                net.minecraft.ChatFormatting formatting = net.minecraft.ChatFormatting.getByCode(code);
                if (formatting != null) {
                    if (formatting == net.minecraft.ChatFormatting.RESET) {
                        currentColor = defaultColor;
                    } else if (formatting.getColor() != null) {
                        currentColor = 0xFF000000 | formatting.getColor();
                    }
                }
                i += 2;
            } else {
                current.append(c);
                i++;
            }
        }
        if (!current.isEmpty()) {
            segments.add(new TextSegment(current.toString(), currentColor));
        }
        return segments;
    }

    @Unique
    private String expandKeyPlaceholders(String text) {
        if (text == null || text.isEmpty()) return text;
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '{' && i + 1 < text.length()) {
                int end = text.indexOf('}', i + 1);
                if (end != -1) {
                    String token = text.substring(i + 1, end);
                    if (token.startsWith("key.")) {
                        String keyName = token.substring(4);
                        KeyMapping mapping = null;
                        for (KeyMapping km : Minecraft.getInstance().options.keyMappings) {
                            if (km.getName().equals("key." + keyName)) {
                                mapping = km;
                                break;
                            }
                        }
                        String replacement = mapping != null ? mapping.getTranslatedKeyMessage().getString() : "?";
                        result.append(replacement);
                        i = end + 1;
                        continue;
                    }
                }
            }
            result.append(c);
            i++;
        }
        return result.toString();
    }

    @Unique
    private void drawRoundedRect(GuiGraphics graphics, int x, int y, int width, int height, int color, float radius) {
        if (radius <= 0.0f || width <= 0 || height <= 0) {
            graphics.fill(x, y, x + width, y + height, color);
            return;
        }
        float maxR = Math.min(width / 2.0f, height / 2.0f);
        radius = Math.min(radius, maxR);

        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Matrix4f matrix = graphics.pose().last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        int innerX = (int)(x + radius);
        int innerY = (int)(y + radius);
        int innerW = (int)(width - 2 * radius);
        int innerH = (int)(height - 2 * radius);
        if (innerW > 0 && innerH > 0) {
            graphics.fill(innerX, innerY, innerX + innerW, innerY + innerH, color);
        }
        graphics.fill(innerX, y, innerX + innerW, innerY, color);
        graphics.fill(innerX, innerY + innerH, innerX + innerW, y + height, color);
        graphics.fill(x, innerY, innerX, innerY + innerH, color);
        graphics.fill(innerX + innerW, innerY, x + width, innerY + innerH, color);

        int segments = 12;
        float angleStep = (float) (Math.PI / 2.0 / segments);
        drawCorner(buffer, matrix, x + radius, y + radius, radius, a, r, g, b, segments, angleStep, 1);
        drawCorner(buffer, matrix, x + width - radius, y + radius, radius, a, r, g, b, segments, angleStep, 2);
        drawCorner(buffer, matrix, x + width - radius, y + height - radius, radius, a, r, g, b, segments, angleStep, 3);
        drawCorner(buffer, matrix, x + radius, y + height - radius, radius, a, r, g, b, segments, angleStep, 4);

        RenderSystem.disableBlend();
    }

    @Unique
    private void drawCorner(BufferBuilder buffer, Matrix4f matrix, float cx, float cy, float radius,
                            int a, int r, int g, int b, int segments, float angleStep, int quadrant) {
        float startAngle = switch (quadrant) {
            case 1 -> (float) Math.PI;
            case 2 -> (float) (Math.PI * 1.5);
            case 3 -> 0.0f;
            case 4 -> (float) (Math.PI * 0.5);
            default -> 0.0f;
        };
        buffer.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix, cx, cy, 0).color(r / 255f, g / 255f, b / 255f, a / 255f).endVertex();
        for (int i = 0; i <= segments; i++) {
            float angle = startAngle + i * angleStep;
            float px = cx + radius * (float) Math.cos(angle);
            float py = cy + radius * (float) Math.sin(angle);
            buffer.vertex(matrix, px, py, 0).color(r / 255f, g / 255f, b / 255f, a / 255f).endVertex();
        }
        BufferUploader.drawWithShader(buffer.end());
    }
}