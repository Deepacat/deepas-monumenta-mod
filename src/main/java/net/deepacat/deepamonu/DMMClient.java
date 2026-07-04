package net.deepacat.deepamonu;

import java.util.Objects;

import com.dayssky.mma.events.ClientReceiveSystemChatEvent;
import com.dayssky.mma.events.EventResult;
import net.deepacat.deepamonu.config.ModConfig;
import net.deepacat.deepamonu.features.Commands;
import net.deepacat.deepamonu.features.Keybinds;
import net.deepacat.deepamonu.features.SoundReward;
import net.deepacat.deepamonu.utils.SafeExceptionLogger;
import net.deepacat.deepamonu.utils.TickScheduler;
import net.minecraft.client.resources.sounds.Sound;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;

import me.shedaniel.autoconfig.ConfigHolder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.ClientStarted;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
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

    public static Player player() {
        return Objects.requireNonNull(Minecraft.getInstance().player);
    }

    public static ClientLevel level() {
        return (ClientLevel) player().level();
    }

    public static String playerName() {
        return player().getScoreboardName();
    }

    public static void reload() {
        ModConfig config = (ModConfig) CONFIG.get();
    }

    public static ModConfig config() {
        return (ModConfig) CONFIG.get();
    }

    public static ModConfig.Appearance appearance() {
        return CONFIG.get().appearance;
    }

    public static ModConfig.ModToggles features() {
        return CONFIG.get().mod;
    }

    public void onInitializeClient() {
        CONFIG = ModConfig.register();
        Keybinds.init();
        SoundReward.init();
        ClientLifecycleEvents.CLIENT_STARTED.register(minecraft -> GLOBAL_SAFE_EH.runSafely(this::initializeAfterMC));
        ClientTickEvents.END_CLIENT_TICK.register(mc -> GLOBAL_SAFE_EH.runSafely(() -> {
            // DMMClient tick functions
            Keybinds.tick();
        }));
        Commands.init();
        VERSION_CHECK = new VersionChecker((ModConfig) CONFIG.get());
        VERSION_CHECK.init();
    }

    private void initializeAfterMC() {
        reload();
    }
}
