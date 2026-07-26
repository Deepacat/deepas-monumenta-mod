package net.deepacat.deepamonu.hud;

import net.deepacat.deepamonu.DMMClient;
import net.deepacat.deepamonu.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiloadDisplay {

    private static final Logger LOGGER = LoggerFactory.getLogger("CrosshairHud");

    private static final Map<Integer, AmmoCache> slotCache = new HashMap<>();
    private static int lastHotbarSlot = -1;
    private static Item lastHeldItem = null;
    private static boolean cachedIsCrossbow = false;

    private static class AmmoCache {
        final int current;
        final int max;
        AmmoCache(int c, int m) { current = c; max = m; }
    }

    private static boolean hasMultiLoad(ItemStack stack) {
        CompoundTag root = stack.getTag();
        if (root == null) return false;
        CompoundTag monumenta = root.getCompound("Monumenta");
        if (monumenta.isEmpty()) return false;
        CompoundTag stock = monumenta.getCompound("Stock");
        if (!stock.isEmpty()) {
            if (stock.getCompound("Enchantments").contains("Multi-Load")) return true;
        }
        return monumenta.getCompound("PlayerModified").contains("RepeaterAmmo");
    }

    private static int getMaxAmmoFromNbt(ItemStack stack) {
        CompoundTag root = stack.getTag();
        if (root == null) return -1;
        CompoundTag monumenta = root.getCompound("Monumenta");
        if (monumenta.isEmpty()) return -1;
        CompoundTag playerMod = monumenta.getCompound("PlayerModified");
        if (playerMod.isEmpty()) return -1;
        return playerMod.contains("RepeaterAmmo") ? playerMod.getInt("RepeaterAmmo") : -1;
    }

    private static boolean checkCrossbowLike(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (hasMultiLoad(stack)) return true;
        Item item = stack.getItem();
        String key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getPath();
        return key.contains("crossbow") || key.contains("arbalest");
    }

    public static void tick() {
        ModConfig config = DMMClient.config();
        if (config == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            ItemStack held = mc.player.getMainHandItem();
            int currentSlot = mc.player.getInventory().selected;
            Item currentItem = held.isEmpty() ? null : held.getItem();

            if (currentSlot != lastHotbarSlot || currentItem != lastHeldItem) {
                if (lastHotbarSlot >= 0 && ActionBarState.getMultiloadMax() > 0) {
                    slotCache.put(lastHotbarSlot, new AmmoCache(
                            ActionBarState.getMultiloadCurrent(), ActionBarState.getMultiloadMax()));
                }
                lastHotbarSlot = currentSlot;
                lastHeldItem = currentItem;

                cachedIsCrossbow = checkCrossbowLike(held);

                if (cachedIsCrossbow) {
                    if (slotCache.containsKey(currentSlot)) {
                        AmmoCache cached = slotCache.get(currentSlot);
                        ActionBarState.setMultiload(cached.current, cached.max);
                    } else {
                        int maxFromNbt = getMaxAmmoFromNbt(held);
                        if (maxFromNbt > 0) {
                            ActionBarState.setMultiload(maxFromNbt, maxFromNbt);
                        } else {
                            ActionBarState.clearMultiload();
                        }
                    }
                } else {
                    slotCache.remove(currentSlot);
                    ActionBarState.clearMultiload();
                }
            }
        }

        ModConfig.Crosshair.Multiload cfg = config.features.crosshair.multiload;
        if (!cfg.enabled) return;

        if (DMMClient.features().enableDebug) {
            long now = System.currentTimeMillis();
            String raw = ActionBarState.getLastActionBarText();
            if (!raw.isEmpty()) {
                LOGGER.debug("[Multiload] Last action bar ({}ms ago): '{}'",
                        now - ActionBarState.getLastActionBarTime(), raw);
            }
        }
    }

    public static void render(GuiGraphics graphics) {
        ModConfig config = DMMClient.config();
        if (config == null) return;
        ModConfig.Crosshair.Multiload cfg = config.features.crosshair.multiload;
        if (!cfg.enabled) return;

        if (!cachedIsCrossbow) return;

        int current = ActionBarState.getMultiloadCurrent();
        int max = ActionBarState.getMultiloadMax();
        if (max <= 0) return;

        int textColor = getAmmoColor(cfg.colors.ammoFullColor, cfg.colors.ammoMidColor, cfg.colors.ammoEmptyColor, current, max);
        textColor |= 0xFF000000;

        if (cfg.display.ammoDisplayMode == ModConfig.Crosshair.DisplayMode.ICONS) {
            CrosshairTextRenderer.renderAmmoIcons(
                    graphics, current, max,
                    cfg.layout.xOffset, cfg.layout.yOffset,
                    0, cfg.layout.textScale,
                    cfg.colors.ammoFullColor, cfg.colors.ammoMidColor, cfg.colors.ammoEmptyColor, cfg.colors.ammoEmptySlotColor,
                    cfg.icons.iconCharStyle,
                    cfg.icons.filledIconChar, cfg.icons.emptyIconChar,
                    cfg.icons.vertical,
                    cfg.icons.hideEmptySlots, cfg.icons.alignment,
                    cfg.icons.iconShadow
            );
        } else {
            String displayText = cfg.display.showTotal
                    ? String.format("%d/%d", current, max)
                    : String.valueOf(current);
            List<CrosshairTextRenderer.TextLine> lines = new ArrayList<>();
            lines.add(new CrosshairTextRenderer.TextLine(displayText, textColor));
            CrosshairTextRenderer.renderLines(graphics, lines, cfg.layout.xOffset, cfg.layout.yOffset, 0, cfg.layout.textScale);
        }

        if (cfg.fireTimer.enabled) {
            long decreaseTime = ActionBarState.getLastAmmoDecreaseTime();
            if (decreaseTime > 0) {
                long elapsed = System.currentTimeMillis() - decreaseTime;
                long durationMs = (long) (cfg.fireTimer.duration * 1000f);
                CrosshairTextRenderer.renderTimerBar(
                        graphics, elapsed, durationMs,
                        cfg.fireTimer.bar.xOffset, cfg.fireTimer.bar.yOffset,
                        cfg.fireTimer.bar.width, cfg.fireTimer.bar.height,
                        cfg.fireTimer.barColors.barColor, cfg.fireTimer.barColors.bgColor,
                        cfg.fireTimer.vertical
                );
            }
        }
    }

    static int getAmmoColor(int fullColor, int midColor, int emptyColor, int current, int max) {
        if (current == max || max <= 0) return fullColor;
        float fraction = (float) current / max;
        if (fraction > 0.66f) return fullColor;
        if (fraction > 0.33f) return midColor;
        return emptyColor;
    }
}
