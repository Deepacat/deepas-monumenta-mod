package net.deepacat.deepamonu.compat.umm;

import me.shedaniel.autoconfig.gui.registry.api.GuiRegistryAccess;
import me.shedaniel.autoconfig.gui.registry.api.GuiTransformer;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

/**
 * Applies the shared alignment tooltip to every {@code textAlignment} field.
 * AutoConfig derives {@code @Tooltip} keys from the field's configUtils path, so two
 * layouts can never share one key through annotations alone. These fields
 * therefore carry no {@code @Tooltip} annotation (which also keeps the default
 * tooltip transformer from overwriting this) and get the shared key here instead.
 */
public class TextAlignmentTooltip implements GuiTransformer {

    public static final String TOOLTIP_KEY = "configUtils.deepamonu.textAlignment.tooltip";

    public static Component tooltip() {
        return Component.translatable(TOOLTIP_KEY);
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public List<AbstractConfigListEntry> transform(List<AbstractConfigListEntry> entries, String baseI18n, Field field, Object config, Object defaults, GuiRegistryAccess registry) {
        for (AbstractConfigListEntry entry : entries) {
            if (entry instanceof TooltipListEntry tooltipEntry) {
                tooltipEntry.setTooltipSupplier(() -> Optional.of(new Component[]{tooltip()}));
            }
        }
        return entries;
    }
}
