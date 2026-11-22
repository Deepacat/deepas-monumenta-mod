package net.deepacat.deepamonu;

import java.util.Objects;

import net.deepacat.deepamonu.compat.TriggerSaving;
import net.deepacat.deepamonu.events.ClientPlayerHurtEvent;
import net.deepacat.deepamonu.events.LocalShieldBreakEvent;
import net.deepacat.deepamonu.events.LocalShieldHitEvent;
import net.deepacat.deepamonu.features.Commands;
import net.deepacat.deepamonu.features.Keybinds;
import net.deepacat.deepamonu.features.ShieldGuardHelper;
import net.deepacat.deepamonu.features.commands.DpsTestTracker;
import net.deepacat.deepamonu.hud.CrosshairHud;
import net.deepacat.deepamonu.hud.DMMHud;
import net.deepacat.deepamonu.utils.SafeExceptionLogger;
import net.deepacat.deepamonu.utils.TickScheduler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;

import me.shedaniel.autoconfig.ConfigHolder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;

public class DMMClient implements ClientModInitializer {
    public static final Gson GSON = new Gson();
    public static final Logger LOGGER = LogManager.getLogger();
    public static final TickScheduler SCHEDULER = new TickScheduler();
    public static final ModContainer MOD = (ModContainer) FabricLoader.getInstance().getModContainer("deepamonu").orElseThrow();
    public static final SafeExceptionLogger GLOBAL_SAFE_EH = new SafeExceptionLogger("GlobalExceptionHandler");
    public static ConfigHolder<ModConfig> CONFIG;
    public static VersionChecker VERSION_CHECK;
    public static String MOD_ID = "deepamonu";

    public static Player player() {
        return Objects.requireNonNull(Minecraft.getInstance().player);
    }

    public static Minecraft instance() {
        return Objects.requireNonNull(Minecraft.getInstance());
    }

    public static ClientLevel level() {
        return (ClientLevel) player().level();
    }

    public static String playerName() {
        return player().getScoreboardName();
    }

    public static void reload() {
        ModConfig config = CONFIG.get();
    }

    public static ModConfig config() {
        return CONFIG.get();
    }

    public static ModConfig.Appearance appearance() {
        return CONFIG.get().appearance;
    }

    public static ModConfig.ModToggles features() {
        return CONFIG.get().mod;
    }

    public void onInitializeClient() {
        CONFIG = ModConfig.register();
        ClientLifecycleEvents.CLIENT_STARTED.register(minecraft -> GLOBAL_SAFE_EH.runSafely(this::initializeAfterMC));

        Keybinds.init();
        DMMHud.init();
        Commands.init();

        ClientPlayerHurtEvent.EVENT.register(damage -> {
//            DMMClient.LOGGER.info("Local player hurt with dmg {}", damage);
            ShieldGuardHelper.updateGuardHitTimer(player(), damage);
        });

        LocalShieldHitEvent.EVENT.register(player -> GLOBAL_SAFE_EH.runSafely(() -> ShieldGuardHelper.updateGuardBlockTimer(player)));
        LocalShieldBreakEvent.EVENT.register(player -> GLOBAL_SAFE_EH.runSafely(() -> ShieldGuardHelper.updateShieldBreakTimer(player)));

        ClientTickEvents.END_WORLD_TICK.register(clientLevel -> GLOBAL_SAFE_EH.runSafely(() -> {
            ShieldGuardHelper.onTick(instance());
        }));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> GLOBAL_SAFE_EH.runSafely(() -> {
            // DMMClient tick functions
            Keybinds.tick();
            TriggerSaving.tick();
            CrosshairHud.tick();
            DpsTestTracker.tick();
        }));

        VERSION_CHECK = new VersionChecker(CONFIG.get());
        VERSION_CHECK.init();
    }

    private void initializeAfterMC() {
        reload();
    }
}
