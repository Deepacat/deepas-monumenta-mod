package net.deepacat.deepamonu.features;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.deepacat.deepamonu.DMMClient;
import net.deepacat.deepamonu.ModConfig;
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
                                        ChatUtil.send(Component.translatable("text.deepamonu.command.debugDumpAbilities"));
                                        return 0;
                                    }),
                                    CommandUtil.lit("chat", ignored -> {
                                                DumpChatMessage.dumpLast(1);
                                                ChatUtil.send(Component.translatable("text.deepamonu.command.debugDumpChat"));
                                                return 0;
                                            },
                                            CommandUtil.arg("count", IntegerArgumentType.integer(1, 50), (context) -> {
                                                int count = IntegerArgumentType.getInteger(context, "count");
                                                DumpChatMessage.dumpLast(count);
                                                ChatUtil.send(Component.translatable("text.deepamonu.command.debugDumpChatCount", count));
                                                return 0;
                                            })
                                    )
                            ),
                            CommandUtil.lit("dpstest",
                                    CommandUtil.lit("timer", ignored -> {
                                                DpsTestTracker.start(10);
                                                ChatUtil.send(Component.translatable("text.deepamonu.command.dpstestTimer", 10));
                                                return 0;
                                            },
                                            CommandUtil.arg("seconds", IntegerArgumentType.integer(1, 300), (context) -> {
                                                int secs = IntegerArgumentType.getInteger(context, "seconds");
                                                DpsTestTracker.start(secs);
                                                ChatUtil.send(Component.translatable("text.deepamonu.command.dpstestTimer", secs));
                                                return 0;
                                            })
                                    ),
                                    CommandUtil.lit("total", ignored -> {
                                                DpsTestTracker.startTotal(10000);
                                                ChatUtil.send(Component.translatable("text.deepamonu.command.dpstestTotal", 10000));
                                                return 0;
                                            },
                                            CommandUtil.arg("damage", IntegerArgumentType.integer(1, Integer.MAX_VALUE), (context) -> {
                                                int target = IntegerArgumentType.getInteger(context, "damage");
                                                DpsTestTracker.startTotal(target);
                                                ChatUtil.send(Component.translatable("text.deepamonu.command.dpstestTotal", target));
                                                return 0;
                                            })
                                    ),
                                    CommandUtil.lit("start", ignored -> {
                                        DpsTestTracker.startInfinite();
                                        ChatUtil.send(Component.translatable("text.deepamonu.command.dpstestStart"));
                                        return 0;
                                    }),
                                    CommandUtil.lit("stop", ignored -> {
                                        DpsTestTracker.stop();
                                        return 0;
                                    })
                            ),
                            CommandUtil.lit("help", ignored -> {
                                ChatUtil.send(Component.translatable("text.deepamonu.command.helpTitle").withStyle(ChatFormatting.BOLD));
                                ChatUtil.send(Component.translatable("text.deepamonu.command.helpNote"));
                                ChatUtil.send(Component.translatable("text.deepamonu.command.helpDebugAbilities"));
                                ChatUtil.send(Component.translatable("text.deepamonu.command.helpDebugChat"));
                                ChatUtil.send(Component.translatable("text.deepamonu.command.helpDpstestTimer"));
                                ChatUtil.send(Component.translatable("text.deepamonu.command.helpDpstestTotal"));
                                ChatUtil.send(Component.translatable("text.deepamonu.command.helpConfig"));
                                ChatUtil.send(Component.translatable("text.deepamonu.command.helpHelp"));
                                ChatUtil.send(Component.translatable("text.deepamonu.command.helpVersion"));
                                return 0;
                            }),
                            CommandUtil.lit("version", ignored -> {
                                ChatUtil.send(DMMClient.MOD.getMetadata().getVersion().getFriendlyString());
                                return 0;
                            }),
                            CommandUtil.lit("configUtils", context -> {
                                DMMClient.SCHEDULER.schedule(0, minecraft ->
                                        minecraft.setScreen(AutoConfig.getConfigScreen(ModConfig.class, minecraft.screen).get())
                                );
                                return 0;
                            })
                    )
            );
            // dmm alias for deepamonu command
            LiteralArgumentBuilder<FabricClientCommandSource> dmm =
                    LiteralArgumentBuilder.<FabricClientCommandSource>literal("dmm").redirect(deepamonu);
            dispatcher.register(dmm);
        });
    }
}
