package net.deepacat.deepamonu.hud;

import net.deepacat.deepamonu.DMMClient;
import net.deepacat.deepamonu.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class MultiloadDisplay {

    private static final Logger LOGGER = LoggerFactory.getLogger("CrosshairHud");

    private static final String MONUMENTA_TAG = "Monumenta";
    private static final String STOCK_TAG = "Stock";
    private static final String ENCHANTMENTS_TAG = "Enchantments";
    private static final String MULTILOAD_ENCHANTMENT = "Multi-Load";
    private static final String LEVEL_TAG = "Level";
    private static final String PLAYER_MODIFIED_TAG = "PlayerModified";
    private static final String AMMO_TAG = "RepeaterAmmo";

    private static int currentAmmo = -1;
    private static int maxAmmo = -1;
    private static long lastShotTime = 0;
    private static int lastObservedAmmo = -1;
    private static int lastPredictedAmmo = -1;
    private static boolean localShotPending = false;
    private static boolean suppressRepeatUse = false;
    private static int lastSlot = -1;
    private static Item lastItem = null;
    private static int lastLoggedAmmo = -2;
    private static int lastLoggedMax = -2;

    /**
     * Reads mult-load state straight from the held item's Monumenta data.
     * RepeaterAmmo is the number of shots left; the Multi-Load enchantment
     * level determines the maximum (level + 1).
     */
    public static void tick() {
        ModConfig config = DMMClient.config();
        if (config == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            reset();
            return;
        }

        ItemStack held = mc.player.getMainHandItem();
        int slot = mc.player.getInventory().selected;
        Item item = held.isEmpty() ? null : held.getItem();

        if (suppressRepeatUse && !mc.options.keyUse.isDown()) {
            suppressRepeatUse = false;
        }

        if (slot != lastSlot || item != lastItem) {
            lastSlot = slot;
            lastItem = item;
            lastObservedAmmo = -1;
            lastPredictedAmmo = -1;
            localShotPending = false;
            suppressRepeatUse = false;
            lastShotTime = 0;
        }

        if (hasMultiLoad(held)) {
            int current = getAmmoFromNbt(held);
            if (current < 0) current = 0;
            int max = getMaxAmmoFromNbt(held);
            if (max <= 0) max = Math.max(current, 1);

            // If prediction is off, or another update path changes the item,
            // still catch ammo decreases to drive the fire timer.
            if (lastObservedAmmo >= 0 && current < lastObservedAmmo && current != lastPredictedAmmo) {
                if (localShotPending) {
                    // This is the server confirming a shot we already timed locally.
                    localShotPending = false;
                } else {
                    lastShotTime = System.currentTimeMillis();
                }
            }
            if (current == lastPredictedAmmo) {
                lastPredictedAmmo = -1;
                localShotPending = false;
            }

            lastObservedAmmo = current;
            currentAmmo = current;
            maxAmmo = max;
        } else {
            currentAmmo = -1;
            maxAmmo = -1;
            lastObservedAmmo = -1;
            lastPredictedAmmo = -1;
            localShotPending = false;
            suppressRepeatUse = false;
        }

        if (DMMClient.features().enableDebug && (currentAmmo != lastLoggedAmmo || maxAmmo != lastLoggedMax)) {
            lastLoggedAmmo = currentAmmo;
            lastLoggedMax = maxAmmo;
            String raw = ActionBarState.getLastActionBarText();
            long actionBarAge = raw.isEmpty() ? -1 : System.currentTimeMillis() - ActionBarState.getLastActionBarTime();
            LOGGER.debug("[Multiload] Item data: {}/{}; last action bar ({}ms ago): '{}'",
                    currentAmmo, maxAmmo, actionBarAge, raw);
        }
    }

    /**
     * Called from the client-side crossbow hook immediately after a shot.
     * When prediction is enabled, decrements the local ammo count and keeps
     * the crossbow charged so the client does not start reloading while the
     * server's shot update is still in flight.
     */
    public static boolean onLocalShot(ItemStack stack, ListTag chargedProjectiles) {
        ModConfig config = DMMClient.config();
        if (config == null) return false;

        ModConfig.Crosshair.Multiload cfg = config.features.crosshair.multiload;
        if (!cfg.enabled || !hasMultiLoad(stack)) return false;

        suppressRepeatUse = false;
        lastShotTime = System.currentTimeMillis();
        localShotPending = true;

        if (!cfg.prediction.enabled) return false;

        int current = getAmmoFromNbt(stack);
        if (current <= 0) return false;

        int next = current - 1;
        setAmmoInNbt(stack, next);

        int max = getMaxAmmoFromNbt(stack);
        if (max > 0) maxAmmo = max;
        currentAmmo = next;
        lastObservedAmmo = next;
        lastPredictedAmmo = next;

        if (next > 0) {
            // Vanilla clears both during the shot. Put them back so the client
            // remains loaded until the server's mult-load update arrives.
            stack.getOrCreateTag().put("ChargedProjectiles", chargedProjectiles.copy());
            CrossbowItem.setCharged(stack, true);
            // Keep the trigger held from immediately firing the restored shot.
            suppressRepeatUse = true;
            return true;
        }
        return false;
    }

    public static boolean shouldSuppressRepeatUse() {
        return suppressRepeatUse;
    }

    public static void render(GuiGraphics graphics) {
        ModConfig config = DMMClient.config();
        if (config == null) return;
        ModConfig.Crosshair.Multiload cfg = config.features.crosshair.multiload;
        if (!cfg.enabled) return;

        if (maxAmmo <= 0) return;

        int current = Math.max(0, Math.min(currentAmmo, maxAmmo));
        int max = maxAmmo;

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

        if (cfg.fireTimer.enabled && lastShotTime > 0) {
            long elapsed = System.currentTimeMillis() - lastShotTime;
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

    private static void reset() {
        currentAmmo = -1;
        maxAmmo = -1;
        lastShotTime = 0;
        lastObservedAmmo = -1;
        lastPredictedAmmo = -1;
        localShotPending = false;
        suppressRepeatUse = false;
        lastSlot = -1;
        lastItem = null;
    }

    private static boolean hasMultiLoad(ItemStack stack) {
        if (stack.isEmpty()) return false;
        CompoundTag root = stack.getTag();
        if (root == null) return false;
        CompoundTag monumenta = root.getCompound(MONUMENTA_TAG);
        if (monumenta.isEmpty()) return false;
        return getMultiloadLevel(monumenta) > 0 || getAmmoFromNbt(monumenta) >= 0;
    }

    private static int getMaxAmmoFromNbt(ItemStack stack) {
        CompoundTag root = stack.getTag();
        if (root == null) return -1;
        CompoundTag monumenta = root.getCompound(MONUMENTA_TAG);
        if (monumenta.isEmpty()) return -1;
        int level = getMultiloadLevel(monumenta);
        return level > 0 ? level + 1 : -1;
    }

    private static int getMultiloadLevel(CompoundTag monumenta) {
        CompoundTag stock = monumenta.getCompound(STOCK_TAG);
        if (stock.isEmpty()) return 0;
        CompoundTag enchantments = stock.getCompound(ENCHANTMENTS_TAG);
        if (enchantments.isEmpty()) return 0;
        return enchantments.getCompound(MULTILOAD_ENCHANTMENT).getInt(LEVEL_TAG);
    }

    private static int getAmmoFromNbt(ItemStack stack) {
        CompoundTag root = stack.getTag();
        if (root == null) return -1;
        CompoundTag monumenta = root.getCompound(MONUMENTA_TAG);
        if (monumenta.isEmpty()) return -1;
        return getAmmoFromNbt(monumenta);
    }

    private static int getAmmoFromNbt(CompoundTag monumenta) {
        CompoundTag playerModified = monumenta.getCompound(PLAYER_MODIFIED_TAG);
        if (playerModified.isEmpty() || !playerModified.contains(AMMO_TAG)) return -1;
        return playerModified.getInt(AMMO_TAG);
    }

    private static void setAmmoInNbt(ItemStack stack, int amount) {
        CompoundTag root = stack.getTag();
        if (root == null) return;
        CompoundTag monumenta = root.getCompound(MONUMENTA_TAG);
        if (monumenta.isEmpty()) return;
        CompoundTag playerModified = monumenta.getCompound(PLAYER_MODIFIED_TAG);
        if (playerModified.isEmpty()) return;
        playerModified.putInt(AMMO_TAG, amount);
    }

    static int getAmmoColor(int fullColor, int midColor, int emptyColor, int current, int max) {
        if (current == max || max <= 0) return fullColor;
        float fraction = (float) current / max;
        if (fraction > 0.66f) return fullColor;
        if (fraction > 0.33f) return midColor;
        return emptyColor;
    }
}
