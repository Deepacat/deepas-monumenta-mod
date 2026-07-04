package net.deepacat.deepamonu.config;

import me.shedaniel.autoconfig.gui.registry.api.GuiProvider;
import me.shedaniel.autoconfig.gui.registry.api.GuiRegistryAccess;
import me.shedaniel.autoconfig.util.Utils;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.util.*;

public class MobGlowColorProvider implements GuiProvider {

    @Override
    public List<AbstractConfigListEntry> get(String i18nKey, Field field, Object config, Object defaults, GuiRegistryAccess registry) {
        Map<String, Integer> currentMap = Utils.getUnsafely(field, config);
        Map<String, Integer> defaultMap = Utils.getUnsafely(field, defaults);

        List<MapEntry> entries = new ArrayList<>();
        currentMap.forEach((k, v) -> entries.add(new MapEntry(k, v)));
        if (entries.isEmpty()) entries.add(new MapEntry("Gravity Bomb", 0xFF0000));

        List<MapEntry> defaultEntries = new ArrayList<>();
        defaultMap.forEach((k, v) -> defaultEntries.add(new MapEntry(k, v)));
        if (defaultEntries.isEmpty()) defaultEntries.add(new MapEntry("Gravity Bomb", 0xFF0000));

        MapListEntry listEntry = new MapListEntry(
                Component.translatable(i18nKey),
                entries,
                null,
                (newList) -> {
                    Map<String, Integer> newMap = new LinkedHashMap<>();
                    for (MapEntry e : newList) {
                        String key = e.key.trim();
                        if (!key.isEmpty()) {
                            newMap.put(key, e.color);
                        }
                    }
                    Utils.setUnsafely(field, config, newMap);
                },
                () -> defaultEntries,
                Component.literal("Reset")
        );

        return Collections.singletonList(listEntry);
    }
}