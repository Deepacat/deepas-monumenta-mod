package net.deepacat.deepamonu.features;

import net.deepacat.deepamonu.DMMClient;
import net.deepacat.deepamonu.config.ModConfig;
import net.deepacat.deepamonu.utils.ChatUtil;
import net.deepacat.deepamonu.utils.CommandUtil;
import com.mojang.brigadier.tree.LiteralCommandNode;

import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class Commands {
    public static void init() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            // ---------- Main /deepamod command ----------
            LiteralCommandNode<FabricClientCommandSource> deepamod = dispatcher.register(
                    CommandUtil.lit("deepamod",
                            // Debug subcommands (only when debug enabled)
                            CommandUtil.<FabricClientCommandSource>litPred(
                                    "debug",
                                    ignored -> DMMClient.config().mod.enableDebug,
                                    CommandUtil.lit("test", ignored -> {
                                        ChatUtil.send(":3");
                                        return 0;
                                    })
                            ),
                            // General info commands
                            CommandUtil.lit("help", ignored -> {
                                ChatUtil.send(Component.literal("Command Help").withStyle(ChatFormatting.BOLD));
                                ChatUtil.send("/deepamod debug - dumps internal state, don't use this unless something breaks");
                                ChatUtil.send("/deepamod config - opens the config");
                                ChatUtil.send("/deepamod help - prints this message");
                                ChatUtil.send("/deepamod version - displays version info");
                                return 0;
                            }),
                            CommandUtil.lit("version", ignored -> {
                                ChatUtil.send(DMMClient.MOD.getMetadata().getVersion().getFriendlyString());
                                return 0;
                            }),
                            CommandUtil.lit("config", context -> {
                                DMMClient.SCHEDULER.schedule(0, minecraft ->
                                        minecraft.setScreen(AutoConfig.getConfigScreen(ModConfig.class, minecraft.screen).get())
                                );
                                return 0;
                            })
                    )
            );
        });
    }
}