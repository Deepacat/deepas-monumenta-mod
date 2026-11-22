package net.deepacat.deepamonu.configUtils;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfig;
import net.deepacat.deepamonu.ModConfig;
import net.minecraft.client.gui.screens.Screen;

public class ModMenuCompat implements ModMenuApi {
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> (Screen) AutoConfig.getConfigScreen(ModConfig.class, parent).get();
    }
}
