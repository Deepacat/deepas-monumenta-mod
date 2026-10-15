package net.deepacat.deepamonu.features;

import com.dayssky.mma.MMAClient;
import com.dayssky.mma.util.SafeExceptionLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.Items;

public class ShieldBlockHelper {
    private static final SafeExceptionLogger EXCEPTION_LOGGER = new SafeExceptionLogger("ShieldBlockHelper");

    public static int guardHitTicks = 0;
    public static int guardBlockTicks = 0;
    public static int shieldBrokenTicks = 0;

    // Updated from block event in client
    public static void updateGuardBlockTimer(Player player) {
        if (player.getMainHandItem().getItem().equals(Items.SHIELD)) {
            guardBlockTicks = 120; // 6 seconds when main-hand shielded
        } else if ((player.getOffhandItem().getItem().equals(Items.SHIELD)) && (guardBlockTicks < 80)) {
            guardBlockTicks = 80; // 4 seconds when off-hand shielded
        }
    }

    // Guard triggers for 2 seconds when taking over 35% of hp in damage at once
    public static void updateGuardHitTimer(Player player, Float damage) {
        if (damage / player.getMaxHealth() >= 0.35) {
            guardHitTicks = 40;
        }
    }

    public static void onTick(Minecraft mc) {
        EXCEPTION_LOGGER.runSafely(() -> {
            if (mc.player == null) {
                guardHitTicks = 0;
                guardBlockTicks = 0;
                shieldBrokenTicks = 0;
            } else {
                if (guardHitTicks > 0) guardHitTicks--;
                if (guardBlockTicks > 0) guardBlockTicks--;

                ItemCooldowns cd = mc.player.getCooldowns();
                if (cd.cooldowns.get(Items.SHIELD) != null) {
                    shieldBrokenTicks = Math.max((cd.cooldowns.get(Items.SHIELD).endTime - cd.tickCount - 1), 0);
                }
            }
        });
    }
}
