package net.deepacat.deepamonu.configUtils;

import me.shedaniel.autoconfig.gui.registry.api.GuiProvider;
import me.shedaniel.autoconfig.gui.registry.api.GuiRegistryAccess;
import me.shedaniel.autoconfig.util.Utils;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CompactAbilityProvider implements GuiProvider {

    @Override
    public List<AbstractConfigListEntry> get(String i18nKey, Field field, Object config, Object defaults, GuiRegistryAccess registry) {
        @SuppressWarnings("unchecked")
        List<CompactAbilityEntry> currentList = Utils.getUnsafely(field, config);
        @SuppressWarnings("unchecked")
        List<CompactAbilityEntry> defaultList = Utils.getUnsafely(field, defaults);

        List<CompactAbilityEntry> entries = new ArrayList<>(currentList != null ? currentList : Collections.emptyList());
        List<CompactAbilityEntry> defaultEntries = new ArrayList<>(defaultList != null ? defaultList : Collections.emptyList());

        CompactAbilityListEntry listEntry = new CompactAbilityListEntry(
                Component.translatable(i18nKey),
                entries,
                null,
                (newList) -> Utils.setUnsafely(field, config, newList),
                () -> defaultEntries,
                Component.translatable("config.deepamonu.reset")
        );

        return Collections.singletonList(listEntry);
    }
}
