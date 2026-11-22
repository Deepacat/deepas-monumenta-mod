package net.deepacat.deepamonu.configUtils;

import me.shedaniel.autoconfig.gui.registry.api.GuiProvider;
import me.shedaniel.autoconfig.gui.registry.api.GuiRegistryAccess;
import me.shedaniel.autoconfig.util.Utils;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MobGlowColorProvider implements GuiProvider {

    @Override
    public List<AbstractConfigListEntry> get(String i18nKey, Field field, Object config, Object defaults, GuiRegistryAccess registry) {
        Map<String, Integer> currentMap = Utils.getUnsafely(field, config);
        Map<String, Integer> defaultMap = Utils.getUnsafely(field, defaults);

        List<MapEntry> entries = new ArrayList<>();
        currentMap.forEach((key, color) -> entries.add(new MapEntry(key, color)));
        if (entries.isEmpty()) entries.add(new MapEntry("Gravity Bomb", 0xFF0000));

        List<MapEntry> defaultEntries = new ArrayList<>();
        defaultMap.forEach((key, color) -> defaultEntries.add(new MapEntry(key, color)));
        if (defaultEntries.isEmpty()) defaultEntries.add(new MapEntry("Gravity Bomb", 0xFF0000));

        MobGlowListEntry listEntry = new MobGlowListEntry(
                Component.translatable(i18nKey),
                entries,
                false,
                null,
                (newList) -> {
                    Map<String, Integer> newMap = new LinkedHashMap<>();
                    for (MapEntry entry : newList) {
                        String key = entry.key.trim();
                        if (!key.isEmpty()) {
                            newMap.put(key, entry.color);
                        }
                    }
                    Utils.setUnsafely(field, config, newMap);
                },
                () -> defaultEntries,
                Component.translatable("config.deepamonu.mobGlowColorOverrides.reset")
        );
        listEntry.setAddTooltip(Component.translatable("config.deepamonu.mobGlowColorOverrides.addTooltip"));
        listEntry.setRemoveTooltip(Component.translatable("config.deepamonu.mobGlowColorOverrides.removeTooltip"));

        return Collections.singletonList(listEntry);
    }
}
