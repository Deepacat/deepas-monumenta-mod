package net.deepacat.deepamonu.features;

import net.deepacat.deepamonu.ClientInit;
import net.deepacat.deepamonu.config.ModConfig;
import net.deepacat.deepamonu.utils.ChatUtil;
import net.deepacat.deepamonu.utils.CommandUtil;
import com.mojang.brigadier.tree.LiteralCommandNode;

import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class Commands {
    public static void init() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            // ---------- MISC COMMANDS (Seperated) ----------

//            // /omw
//            dispatcher.register(CommandUtil.lit("omw", context -> {
//                ChatUtil.sendCommand("lfg omw");
//                return 0;
//            }, CommandUtil.arg("text", StringArgumentType.greedyString(), context -> {
//                String arg = StringArgumentType.getString(context, "text");
//                ChatUtil.sendCommand(String.format("lfg omw %s", arg));
//                return 0;
//            })));
//            // /compass
//            dispatcher.register(CommandUtil.lit("compass", context -> {
//                BlockPos pos = ClientInit.player().level().getSharedSpawnPos();
//                ChatUtil.send("Position: %s, %s, %s".formatted(pos.getX(), pos.getY(), pos.getZ()));
//                return 0;
//            }));
//            // /timer
//            dispatcher.register(CommandUtil.lit("timer", context -> {
//                if (timerMs == -1L) {
//                    timerMs = Util.now();
//                    ChatUtil.send(Component.translatable("text.deepamod.timer_start"));
//                } else {
//                    long delta = Util.now() - timerMs;
//                    ChatUtil.send(Component.translatable("text.deepamod.timer_end", FormatUtil.timestamp(delta)));
//                    timerMs = -1L;
//                }
//                return 0;
//            }));
            // /lb (leaderboard)
//            dispatcher.register(CommandUtil.lit("lb",
//                    CommandUtil.arg(
//                            "lb_name",
//                            StringArgumentType.word(),
//                            context -> {
//                                String lbName = LeaderboardUtils.resolve(StringArgumentType.getString(context, "lb_name"));
//                                ChatUtil.sendCommand(String.format("leaderboard @s %s true 1", lbName));
//                                return 0;
//                            },
//                            (context, builder) -> SharedSuggestionProvider.suggest(LeaderboardUtils.getKeys(), builder),
//                            CommandUtil.arg(
//                                    "arg",
//                                    StringArgumentType.word(),
//                                    context -> {
//                                        String lbName = LeaderboardUtils.resolve(StringArgumentType.getString(context, "lb_name"));
//                                        String arg = StringArgumentType.getString(context, "arg");
//                                        ChatUtil.sendCommand(String.format("leaderboard @s %s true %s", lbName, arg));
//                                        return 0;
//                                    }
//                            )
//                    )
//            ));

            // ---------- Main /deepamod command ----------
            LiteralCommandNode<FabricClientCommandSource> deepamod = dispatcher.register(
                    CommandUtil.lit("deepamod",
                            // Debug subcommands (only when debug enabled)
                            CommandUtil.<FabricClientCommandSource>litPred(
                                    "debug",
                                    ignored -> ClientInit.config().mod.enableDebug,
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
                                ChatUtil.send(ClientInit.MOD.getMetadata().getVersion().getFriendlyString());
                                return 0;
                            }),
                            CommandUtil.lit("config", context -> {
                                ClientInit.SCHEDULER.schedule(0, minecraft ->
                                        minecraft.setScreen((Screen) AutoConfig.getConfigScreen(ModConfig.class, minecraft.screen).get())
                                );
                                return 0;
                            })
                    )
            );
        });
    }
}