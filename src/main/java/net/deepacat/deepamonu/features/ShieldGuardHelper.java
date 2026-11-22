package net.deepacat.deepamonu.features;

import net.deepacat.deepamonu.DMMClient;
import net.deepacat.deepamonu.utils.NBTUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.Items;

public class ShieldGuardHelper {
    private static final String GUARD_ENCHANTMENT = "Guard";

    /**
     * Vanilla shield-disabled cooldown is 5 seconds, used as a fallback.
     */
    private static final int DEFAULT_SHIELD_BREAK_TICKS = 100;

    public static int guardHitTicks = 0;
    public static int guardBlockTicks = 0;
    public static int shieldBrokenTicks = 0;

    public static int guardTicks() {
        return Math.max(guardBlockTicks, guardHitTicks);
    }

    public static boolean hasGuardEnchant(Player player) {
        if (player == null) return false;
        return NBTUtils.getAllEnchants(player).getOrDefault(GUARD_ENCHANTMENT, 0) > 0;
    }

    // Updated from block event in client. Guard activates on any shield block.
    public static void updateGuardBlockTimer(Player player) {
        DMMClient.LOGGER.info("[DEBUG] Shield was hit, checking guard timer");
        DMMClient.LOGGER.info("[DEBUG] enchants: {}", NBTUtils.getAllEnchants(player));
        DMMClient.LOGGER.info("[DEBUG] guard: {}", NBTUtils.getAllEnchants(player).getOrDefault("Guard", 0));
        DMMClient.LOGGER.info("[DEBUG] Does player have guard: {}", hasGuardEnchant(player));
        if (!hasGuardEnchant(player)) return;
        DMMClient.LOGGER.info("[DEBUG] Updating guard block timer");
        boolean mainShield = player.getMainHandItem().is(Items.SHIELD);
        boolean offShield = player.getOffhandItem().is(Items.SHIELD);
        if (mainShield) {
            guardBlockTicks = 120; // 6 seconds when main-hand shielded
        } else if (offShield && guardBlockTicks < 80) {
            guardBlockTicks = 80; // 4 seconds when off-hand shielded
        }
    }

    // Guard triggers for 2 seconds when taking over 35% of hp in damage at once
    public static void updateGuardHitTimer(Player player, Float damage) {
        if (!hasGuardEnchant(player)) return;
        float ratio = damage / player.getMaxHealth();
        if (ratio >= 0.35) {
            guardHitTicks = 40;
        }
    }

    /**
     * Called when the server sends the shield-break entity event. The event can
     * arrive before the client cooldown entry exists, so use a vanilla-length
     * fallback that is corrected by {@link #onTick(Minecraft)} once it appears.
     */
    public static void updateShieldBreakTimer(Player player) {
        ItemCooldowns cooldowns = player.getCooldowns();
        ItemCooldowns.CooldownInstance instance = cooldowns.cooldowns.get(Items.SHIELD);
        if (instance != null) {
            shieldBrokenTicks = Math.max(instance.endTime - cooldowns.tickCount - 1, 0);
        }
        if (shieldBrokenTicks <= 0) {
            shieldBrokenTicks = DEFAULT_SHIELD_BREAK_TICKS;
        }
    }

    public static void onTick(Minecraft mc) {
        DMMClient.GLOBAL_SAFE_EH.runSafely(() -> {
            if (mc.player == null) {
                guardHitTicks = 0;
                guardBlockTicks = 0;
                shieldBrokenTicks = 0;
            } else {
                if (guardHitTicks > 0) guardHitTicks--;
                if (guardBlockTicks > 0) guardBlockTicks--;

                ItemCooldowns cooldowns = mc.player.getCooldowns();
                ItemCooldowns.CooldownInstance instance = cooldowns.cooldowns.get(Items.SHIELD);
                if (instance != null) {
                    shieldBrokenTicks = Math.max(instance.endTime - cooldowns.tickCount - 1, 0);
                } else if (shieldBrokenTicks > 0) {
                    shieldBrokenTicks--;
                }
            }
        });
    }
}
