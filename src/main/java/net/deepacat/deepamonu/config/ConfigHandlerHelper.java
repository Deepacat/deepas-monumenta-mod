package net.deepacat.deepamonu.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;

public class ConfigHandlerHelper {
    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static <T, U> Stream<AbstractConfigListEntry> buildClassifying(
            Stream<T> entries,
            Function<T, U> classifier,
            Function<U, Component> nameGetter,
            Function<Stream<T>, Stream<AbstractConfigListEntry>> b,
            ConfigEntryBuilder builder
    ) {
        return entries.collect(Collectors.groupingBy(classifier))
                .entrySet()
                .stream()
                .map(x -> builder.startSubCategory(nameGetter.apply(x.getKey()), b.apply((Stream<T>) x.getValue().stream()).toList()).build());
    }

    public static void register() {
    }
}
