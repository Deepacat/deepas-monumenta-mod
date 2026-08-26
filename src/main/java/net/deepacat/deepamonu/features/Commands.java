package net.deepacat.deepamonu.features;

import net.deepacat.deepamonu.DMMClient;
import net.deepacat.deepamonu.config.ModConfig;
import net.deepacat.deepamonu.features.commands.debug.DpsTestTracker;
import net.deepacat.deepamonu.features.commands.debug.DumpAbilities;
import net.deepacat.deepamonu.features.commands.debug.DumpChatMessage;
import net.deepacat.deepamonu.utils.ChatUtil;
import net.deepacat.deepamonu.utils.CommandUtil;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;

import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class Commands {
    public static void init() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            LiteralCommandNode<FabricClientCommandSource> deepamonu = dispatcher.register(
                    CommandUtil.lit("deepamonu",
                            CommandUtil.lit("debug",
                                    CommandUtil.lit("abilities", ignored -> {
                                        DumpAbilities.run();
                                        ChatUtil.send("Dumped ability data to log");
                                        return 0;
                                    }),
                            CommandUtil.lit("chat", ignored -> {
                                DumpChatMessage.dumpLast(1);
                                ChatUtil.send("Dumped last chat message to log");
                                return 0;
                            },
                                    CommandUtil.arg("count", IntegerArgumentType.integer(1, 50), (context) -> {
                                        int count = IntegerArgumentType.getInteger(context, "count");
                                        DumpChatMessage.dumpLast(count);
                                        ChatUtil.send("Dumped last " + count + " chat messages to log");
                                        return 0;
                                    })
                            )
                            ),
                            CommandUtil.lit("dpstest",
                                    CommandUtil.lit("timer", ignored -> {
                                        DpsTestTracker.start(10);
                                        ChatUtil.send("DPS test started for 10 seconds");
                                        return 0;
                                    },
                                            CommandUtil.arg("seconds", IntegerArgumentType.integer(1, 300), (context) -> {
                                                int secs = IntegerArgumentType.getInteger(context, "seconds");
                                                DpsTestTracker.start(secs);
                                                ChatUtil.send("DPS test started for " + secs + " seconds");
                                                return 0;
                                            })
                                    ),
                                    CommandUtil.lit("total", ignored -> {
                                        DpsTestTracker.startTotal(10000);
                                        ChatUtil.send("DPS test started, target 10000 total damage");
                                        return 0;
                                    },
                                            CommandUtil.arg("damage", IntegerArgumentType.integer(1, Integer.MAX_VALUE), (context) -> {
                                                int target = IntegerArgumentType.getInteger(context, "damage");
                                                DpsTestTracker.startTotal(target);
                                                ChatUtil.send("DPS test started, target " + target + " total damage");
                                                return 0;
                                            })
                                    ),
                                    CommandUtil.lit("start", ignored -> {
                                        DpsTestTracker.startInfinite();
                                        ChatUtil.send("DPS test started. Use /deepamonu dpstest stop to end.");
                                        return 0;
                                    }),
                                    CommandUtil.lit("stop", ignored -> {
                                        DpsTestTracker.stop();
                                        return 0;
                                    })
                            ),
                            CommandUtil.lit("help", ignored -> {
                                ChatUtil.send(Component.literal("Command Help").withStyle(ChatFormatting.BOLD));
                                ChatUtil.send("/deepamonu debug abilities - dumps current UMM ability data to log");
                                ChatUtil.send("/deepamonu debug chat [count] - dumps last N chat messages to log");
                                ChatUtil.send("/deepamonu dpstest timer [seconds] - starts a timed DPS test");
                                ChatUtil.send("/deepamonu dpstest total [damage] - starts a damage-target DPS test");
                                ChatUtil.send("/deepamonu config - opens the config");
                                ChatUtil.send("/deepamonu help - prints this message");
                                ChatUtil.send("/deepamonu version - displays version info");
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
