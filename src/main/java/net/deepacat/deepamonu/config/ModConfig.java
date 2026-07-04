package net.deepacat.deepamonu.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Category;
import me.shedaniel.autoconfig.annotation.ConfigEntry.ColorPicker;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.TransitiveObject;
import me.shedaniel.autoconfig.gui.registry.GuiRegistry;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.deepacat.deepamonu.DMMClient;
import net.minecraft.SharedConstants;
import net.minecraft.world.InteractionResult;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

@Config(name = "deepamonu")
public class ModConfig implements ConfigData {
    @Category("features")
    @TransitiveObject
    public Features features = new Features();

    public static class Features {
        @ConfigEntry.Gui.CollapsibleObject
        public SoundReward soundReward = new SoundReward();


        public static class SoundReward {
            public boolean enable = true;
            public float volume = 1.0f;
            public float pitch = 1.0f;
            public boolean deepasPreset = true;
        }

        @ConfigEntry.Gui.CollapsibleObject
        public MobGlowColorOverrides mobGlowColorOverrides = new MobGlowColorOverrides();

        public static class MobGlowColorOverrides {
            public boolean enable = true;

            @ConfigEntry.Gui.CollapsibleObject
            public MobColorsDropdown mobColorsDropdown = new MobColorsDropdown();

            public static class MobColorsDropdown {
                public Map<String, Integer> mobColorMap = new LinkedHashMap<>(Map.of("Gravity Bomb", 0xFF0000));
            }
        }

//        @ConfigEntry.Gui.CollapsibleObject
//        public InventoryOverlayToggles inventoryOverlay = new InventoryOverlayToggles();
//
//        public static class InventoryOverlayToggles {
//            public boolean enable = true;
//            public boolean enableRarity = false;
//            public boolean enableCZCharmRarity = false;
//            public boolean enableCooldown = true;
//            public boolean enableCZCharmPower = false;
//            public boolean enablePICount = true;
//            public boolean enableLoomFirmCount = false;
//            @ConfigEntry.BoundedDiscrete(min = 0L, max = 20L)
//            public int updateDelayTicks = 5;
//        }
    }

    @Category("modtweaks")
    @TransitiveObject
    public ModTweaks modtweaks = new ModTweaks();

    public static class ModTweaks {
        @ConfigEntry.Gui.CollapsibleObject
        public TSlatEntityStatus tslatentitystatus = new TSlatEntityStatus();

        public static class TSlatEntityStatus {
            @ConfigEntry.Gui.CollapsibleObject
            public Particles particles = new Particles();

            public static class Particles {
                public boolean enableThresholds = true;
                public float healThreshold = 1.0f;
                public float damageThreshold = 1.0f;
            }
        }
    }


    @Category("mod")
    @TransitiveObject
    public ModToggles mod = new ModToggles();

    public static class ModToggles {
        public boolean enableDebug = SharedConstants.IS_RUNNING_IN_IDE;
        public boolean suppressDebugWarning = !SharedConstants.IS_RUNNING_IN_IDE;
        public boolean versionCheck = false;
        public boolean versionCheckIncludeBeta = false;
    }

    @Category("appearance")
    @TransitiveObject
    public Appearance appearance = new Appearance();

    public static class Appearance {
        @ColorPicker
        public int bracketColor = 12041720;
        @ColorPicker
        public int tagColor = 13017334;
        public String tagText = "DMM";
        @ColorPicker
        public int textColor = 16047062;
        @ColorPicker
        public int numericColor = 15961000;
        @ColorPicker
        public int detailColor = 7106437;
        @ColorPicker
        public int playerNameColor = 15703926;
        @ColorPicker
        public int altTextColor = 11845374;
        @ColorPicker
        public int errorColor = 15091027;
        @ColorPicker
        public int warningColor = 14650909;
    }

    public static ConfigHolder<ModConfig> register() {
        ConfigHolder<ModConfig> holder = AutoConfig.register(
                ModConfig.class, (config, clazz) -> new GsonConfigSerializer(config, clazz, ConfigHandlerHelper.GSON)
        );

        GuiRegistry registry = AutoConfig.getGuiRegistry(ModConfig.class);
        Predicate<Field> predicate = field -> {
            if (field.getType() != Map.class) return false;
            if (!(field.getGenericType() instanceof ParameterizedType pt)) return false;
            java.lang.reflect.Type[] args = pt.getActualTypeArguments();
            return args.length == 2 && args[0] == String.class && args[1] == Integer.class;
        };
        registry.registerPredicateProvider(new MobGlowColorProvider(), predicate);

        holder.registerSaveListener((configHolder, config) -> {
            config.validatePostLoad();
            DMMClient.reload();
            return InteractionResult.PASS;
        });
        ConfigHandlerHelper.register();
        return holder;
    }

    public @interface Hidden {
    }

    public void validatePostLoad() {

    }
}
