package net.deepacat.deepamonu.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Category;
import me.shedaniel.autoconfig.annotation.ConfigEntry.ColorPicker;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.TransitiveObject;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.Tooltip;
import me.shedaniel.autoconfig.gui.registry.GuiRegistry;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.deepacat.deepamonu.DMMClient;
import net.minecraft.SharedConstants;
import net.minecraft.world.InteractionResult;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@Config(name = "deepamonu")
public class ModConfig implements ConfigData {
    @Category("features")
    @TransitiveObject
    public Features features = new Features();

    public static class Features {
        @ConfigEntry.Gui.CollapsibleObject
        public MobGlowColorOverrides mobGlowColorOverrides = new MobGlowColorOverrides();

        public static class MobGlowColorOverrides {
            @Tooltip(count = 1)
            public boolean enable = true;

            @ConfigEntry.Gui.CollapsibleObject
            public MobColorsDropdown mobColorsDropdown = new MobColorsDropdown();

            public static class MobColorsDropdown {
                public Map<String, Integer> mobColorMap = new LinkedHashMap<>(Map.of("Gravity Bomb", 0xFF0000));
            }
        }

        @ConfigEntry.Gui.CollapsibleObject
        public Crosshair crosshair = new Crosshair();
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
                @Tooltip(count = 1)
                public boolean enableThresholds = true;
                @Tooltip(count = 1)
                public float healThreshold = 1.0f;
                @Tooltip(count = 1)
                public float damageThreshold = 1.0f;
            }
        }

        @ConfigEntry.Gui.CollapsibleObject
        public UMM umm = new UMM();

        public static class UMM {
            @ConfigEntry.Gui.CollapsibleObject
            public TriggerOverlay triggerOverlay = new TriggerOverlay();

            public static class TriggerOverlay {
                @Tooltip(count = 1)
                public boolean enabled = true;

                @ConfigEntry.Gui.CollapsibleObject
                public TriggerLayout layout = new TriggerLayout();

                @ConfigEntry.Gui.CollapsibleObject
                public TriggerModifiers modifiers = new TriggerModifiers();

                @ConfigEntry.Gui.CollapsibleObject
                public TriggerBackground background = new TriggerBackground();

                @Tooltip(count = 1)
                public boolean useIcons = false;

                @ConfigEntry.Gui.Excluded
                public transient boolean resetAutoTriggers = false;
            }

            public static class TriggerLayout {
                @Tooltip(count = 1)
                public int xOffset = 0;
                @Tooltip(count = 1)
                public int yOffset = 0;
            }

            public static class TriggerModifiers {
                @Tooltip(count = 1)
                public boolean modifiersEnabled = true;
                @Tooltip(count = 1)
                public int modifierXOffset = 0;
                @Tooltip(count = 1)
                public int modifierYOffset = 0;
                @Tooltip(count = 1)
                public boolean modifiersBelowKeyLine = false;
            }

            public static class TriggerBackground {
                @Tooltip(count = 1)
                public boolean backgroundEnabled = true;
                @Tooltip(count = 1)
                public int backgroundXOffset = 0;
                @Tooltip(count = 1)
                public int backgroundYOffset = 0;
                @Tooltip(count = 1)
                public int backgroundWidth = 0;
                @Tooltip(count = 1)
                public int backgroundHeight = 0;
                @Tooltip(count = 1)
                public float backgroundCornerRadius = 0.0f;
                @Tooltip(count = 1) @ColorPicker(allowAlpha = true)
                public int backgroundColor = 0x80000000;
            }
        }
    }

    @Category("mod")
    @TransitiveObject
    public ModToggles mod = new ModToggles();

    public static class ModToggles {
        @Tooltip(count = 1)
        public boolean enableDebug = SharedConstants.IS_RUNNING_IN_IDE;
        @Tooltip(count = 1)
        public boolean suppressDebugWarning = !SharedConstants.IS_RUNNING_IN_IDE;
        @Tooltip(count = 1)
        public boolean versionCheck = false;
        @Tooltip(count = 1)
        public boolean versionCheckIncludeBeta = false;
    }

    @Category("appearance")
    @TransitiveObject
    public Appearance appearance = new Appearance();

    public static class Appearance {
        @Tooltip(count = 1) @ColorPicker
        public int bracketColor = 12041720;
        @Tooltip(count = 1) @ColorPicker
        public int tagColor = 13017334;
        @Tooltip(count = 1)
        public String tagText = "DMM";
        @Tooltip(count = 1) @ColorPicker
        public int textColor = 16047062;
        @Tooltip(count = 1) @ColorPicker
        public int numericColor = 15961000;
        @Tooltip(count = 1) @ColorPicker
        public int detailColor = 7106437;
        @Tooltip(count = 1) @ColorPicker
        public int playerNameColor = 15703926;
        @Tooltip(count = 1) @ColorPicker
        public int altTextColor = 11845374;
        @Tooltip(count = 1) @ColorPicker
        public int errorColor = 15091027;
        @Tooltip(count = 1) @ColorPicker
        public int warningColor = 14650909;
    }

    // ===== Crosshair HUD sub-configs (inside Features) =====

    public static class Crosshair {
        public enum DisplayMode { TEXT, ICONS }
        public enum IconCharStyle { BLOCKS, CIRCLES, SQUARES }
        public enum Alignment { LEFT, CENTER, RIGHT }
        public enum ShadowType { OUTLINE, VANILLA }

        @ConfigEntry.Gui.CollapsibleObject
        public CompactAbilities compactAbilities = new CompactAbilities();

        public static class CompactAbilities {
            @Tooltip(count = 1)
            public boolean enabled = false;
            @Tooltip(count = 1)
            public List<CompactAbilityEntry> trackedAbilities = new ArrayList<>();
            @Tooltip(count = 1)
            public boolean advancedMode = false;
            @Tooltip(count = 1)
            public String advancedJsonPath = "config/deepamonu/compact_abilities.json";
        }

        @ConfigEntry.Gui.CollapsibleObject
        public AlchPotions alchPotions = new AlchPotions();

        public static class AlchPotions {
            @Tooltip(count = 1)
            public boolean enabled = false;

            @ConfigEntry.Gui.CollapsibleObject
            public Layout layout = new Layout(60);

            @ConfigEntry.Gui.CollapsibleObject
            public PotionDisplaySettings display = new PotionDisplaySettings();

            @ConfigEntry.Gui.CollapsibleObject
            public PotionIconSettings icons = new PotionIconSettings();

            @ConfigEntry.Gui.CollapsibleObject
            public PotionColors colors = new PotionColors();
        }

        public static class PotionDisplaySettings {
            @Tooltip(count = 1)
            public DisplayMode potionDisplayMode = DisplayMode.TEXT;
        }

        public static class PotionIconSettings {
            @Tooltip(count = 1)
            public IconCharStyle iconCharStyle = IconCharStyle.BLOCKS;
            @Tooltip(count = 1)
            public String filledIconChar = "";
            @Tooltip(count = 1)
            public String emptyIconChar = "\u25CB";
            @Tooltip(count = 1)
            public ShadowType iconShadow = ShadowType.OUTLINE;
            @Tooltip(count = 1)
            public boolean vertical = false;
        }

        public static class PotionColors {
            @Tooltip(count = 1) @ColorPicker
            public int potionFullColor = 0x55FF55;
            @Tooltip(count = 1) @ColorPicker
            public int potionMidColor = 0xFFAA00;
            @Tooltip(count = 1) @ColorPicker
            public int potionEmptyColor = 0xFF5555;
            @Tooltip(count = 1) @ColorPicker
            public int potionEmptySlotColor = 0x444444;
        }

        @ConfigEntry.Gui.CollapsibleObject
        public Multiload multiload = new Multiload();

        public static class Multiload {
            @Tooltip(count = 1)
            public boolean enabled = false;

            @ConfigEntry.Gui.CollapsibleObject
            public Layout layout = new Layout(75);

            @ConfigEntry.Gui.CollapsibleObject
            public DisplaySettings display = new DisplaySettings();

            @ConfigEntry.Gui.CollapsibleObject
            public IconSettings icons = new IconSettings();

            @ConfigEntry.Gui.CollapsibleObject
            public AmmoColors colors = new AmmoColors();

            @ConfigEntry.Gui.CollapsibleObject
            public FireTimer fireTimer = new FireTimer();
        }

        public static class DisplaySettings {
            @Tooltip(count = 1)
            public boolean hideVanillaAmmo = true;
            @Tooltip(count = 1)
            public boolean showTotal = true;
            @Tooltip(count = 1)
            public DisplayMode ammoDisplayMode = DisplayMode.TEXT;
        }

        public static class IconSettings {
            @Tooltip(count = 1)
            public IconCharStyle iconCharStyle = IconCharStyle.BLOCKS;
            @Tooltip(count = 1)
            public String filledIconChar = "";
            @Tooltip(count = 1)
            public String emptyIconChar = "\u25CB";
            @Tooltip(count = 1)
            public ShadowType iconShadow = ShadowType.OUTLINE;
            @Tooltip(count = 1)
            public boolean vertical = false;
            @Tooltip(count = 1)
            public boolean hideEmptySlots = false;
            @Tooltip(count = 1)
            public Alignment alignment = Alignment.CENTER;
        }

        public static class FireTimer {
            @Tooltip(count = 1)
            public boolean enabled = false;
            @Tooltip(count = 1)
            public boolean vertical = false;
            @Tooltip(count = 1)
            public float duration = 0.5f;

            @ConfigEntry.Gui.CollapsibleObject
            public BarLayout bar = new BarLayout();

            @ConfigEntry.Gui.CollapsibleObject
            public BarColors barColors = new BarColors();
        }

        public static class BarLayout {
            @Tooltip(count = 1)
            public int xOffset = 0;
            @Tooltip(count = 1)
            public int yOffset = 33;
            @Tooltip(count = 1)
            public int width = 32;
            @Tooltip(count = 1)
            public int height = 4;
        }

        public static class BarColors {
            @Tooltip(count = 1) @ColorPicker
            public int barColor = 0xFFFFFF;
            @Tooltip(count = 1) @ColorPicker
            public int bgColor = 0x000000;
        }

        public static class AmmoColors {
            @Tooltip(count = 1) @ColorPicker
            public int ammoFullColor = 0x55FF55;
            @Tooltip(count = 1) @ColorPicker
            public int ammoMidColor = 0xFFAA00;
            @Tooltip(count = 1) @ColorPicker
            public int ammoEmptyColor = 0xFF5555;
            @Tooltip(count = 1) @ColorPicker
            public int ammoEmptySlotColor = 0x444444;
        }

        public static class Layout {
            @Tooltip(count = 1)
            public int xOffset;
            @Tooltip(count = 1)
            public int yOffset;
            @Tooltip(count = 1)
            public float textScale = 1.0f;
            @Tooltip(count = 1)
            public boolean renderOverUI = false;

            public Layout(int defaultYOffset) {
                this.xOffset = 0;
                this.yOffset = defaultYOffset;
            }
        }
    }

    // ===== Config registration =====

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

        registry.registerPredicateProvider(new CompactAbilityProvider(),
                field -> field.getType() == List.class
        );

        registry.registerPredicateProvider(
                new ResetTriggersProvider(),
                field -> field.getName().equals("resetAutoTriggers")
        );

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
