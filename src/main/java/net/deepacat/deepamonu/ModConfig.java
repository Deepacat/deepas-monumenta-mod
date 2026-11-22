package net.deepacat.deepamonu;

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
import net.deepacat.deepamonu.compat.umm.ResetTriggersProvider;
import net.deepacat.deepamonu.compat.umm.TextAlignmentTooltip;
import net.deepacat.deepamonu.configUtils.*;
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
            @Tooltip()
            public boolean enable = true;
            public Map<String, Integer> mobColorMap = new LinkedHashMap<>(Map.of("Gravity Bomb", 0xFF0000));
        }

        @ConfigEntry.Gui.CollapsibleObject
        public BlockEntityHider blockEntityHider = new BlockEntityHider();

        public static class BlockEntityHider {
            @Tooltip()
            public boolean enabled = true;
            @Tooltip()
            public boolean hideInAllDimensions = false;
            @Tooltip()
            public boolean hideInDimensionList = false;
            @Tooltip()
            public boolean hideAllBlocks = false;
            @Tooltip()
            public int distance = 6;
            @Tooltip()
            public List<String> blockEntityIds = new ArrayList<>(List.of("minecraft:chest", "/minecraft.*sign/", "/minecraft.*banner/"));
            @Tooltip()
            public List<String> dimensionIds = new ArrayList<>(List.of("monumenta:plots"));
        }

        @ConfigEntry.Gui.CollapsibleObject
        public ShieldHelpers shieldHelpers = new ShieldHelpers();

        public static class ShieldHelpers {
            @Tooltip()
            public boolean enableShieldStunTimer;

            @Tooltip()
            public boolean shieldStunAlwaysShow = false;

            @ConfigEntry.Gui.CollapsibleObject
            public ShieldStunTimerDisplaySettings shieldStunTimerDisplaySettings = new ShieldStunTimerDisplaySettings();

            public static class ShieldStunTimerDisplaySettings {
                @Tooltip()
                public String displayText = "Break: %breaktime%";
                @ConfigEntry.Gui.CollapsibleObject
                public HudElementConfig hud = HudElementConfig.builder()
                        .alignment(1.0F, 0.0F)
                        .textColor(0xFF55FF)
                        .bgColor(0x80000000)
                        .build();
            }

            @Tooltip()
            public boolean enableGuardTimer;

            @Tooltip()
            public boolean guardAlwaysShow = false;

            @ConfigEntry.Gui.CollapsibleObject
            public GuardTimerDisplaySettings guardTimerDisplaySettings = new GuardTimerDisplaySettings();

            public static class GuardTimerDisplaySettings {
                @Tooltip()
                public String displayText = "Guard: %guardtime%";
                @ConfigEntry.Gui.CollapsibleObject
                public HudElementConfig hud = HudElementConfig.builder()
                        .offset(0, 20)
                        .alignment(1.0F, 0.0F)
                        .textColor(0xFF55FF)
                        .bgColor(0x80000000)
                        .build();
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
                @Tooltip()
                public boolean enableThresholds = false;
                @Tooltip()
                public float healThreshold = 1.0f;
                @Tooltip()
                public float damageThreshold = 1.0f;
            }
        }

        @ConfigEntry.Gui.CollapsibleObject
        public UMM umm = new UMM();

        public static class UMM {
            @ConfigEntry.Gui.CollapsibleObject
            public TriggerOverlay triggerOverlay = new TriggerOverlay();

            public static class TriggerOverlay {
                @Tooltip()
                public boolean enabled = false;

                @ConfigEntry.Gui.CollapsibleObject
                public TriggerLayout layout = new TriggerLayout();

                @ConfigEntry.Gui.CollapsibleObject
                public TriggerModifiers modifiers = new TriggerModifiers();

                @ConfigEntry.Gui.CollapsibleObject
                public TriggerBackground background = new TriggerBackground();

                @Tooltip()
                public boolean useIcons = false;

                @ConfigEntry.Gui.Excluded
                public transient boolean resetAutoTriggers = false;
            }

            public static class TriggerLayout {
                @Tooltip()
                public int xOffset = 0;
                @Tooltip()
                public int yOffset = 0;
            }

            public static class TriggerModifiers {
                @Tooltip()
                public boolean modifiersEnabled = false;
                @Tooltip()
                public int modifierXOffset = 0;
                @Tooltip()
                public int modifierYOffset = 0;
                @Tooltip()
                public boolean modifiersBelowKeyLine = false;
            }

            public static class TriggerBackground {
                @Tooltip()
                public boolean backgroundEnabled = true;
                @Tooltip()
                public int backgroundXOffset = 0;
                @Tooltip()
                public int backgroundYOffset = 0;
                @Tooltip()
                public int backgroundWidth = 0;
                @Tooltip()
                public int backgroundHeight = 0;
                @Tooltip()
                public float backgroundCornerRadius = 1.0f;
                @Tooltip()
                @ColorPicker(allowAlpha = true)
                public int backgroundColor = 0x80000000;
            }
        }

        @ConfigEntry.Gui.CollapsibleObject
        public XaeroMap xaeromap = new XaeroMap();

        public static class XaeroMap {
            @ConfigEntry.Gui.CollapsibleObject
            public Patches patches = new Patches();

            public static class Patches {
                @Tooltip()
                public boolean enableWorldMapTransferCrashFix = true;
            }
        }
    }

    @Category("mod")
    @TransitiveObject
    public ModToggles mod = new ModToggles();

    public static class ModToggles {
        @Tooltip()
        public boolean enableDebug = SharedConstants.IS_RUNNING_IN_IDE;
        @Tooltip()
        public boolean suppressDebugWarning = !SharedConstants.IS_RUNNING_IN_IDE;
        @Tooltip()
        public boolean versionCheck = false;
        @Tooltip()
        public boolean versionCheckIncludeBeta = false;
    }

    @Category("appearance")
    @TransitiveObject
    public Appearance appearance = new Appearance();

    public static class Appearance {
        @Tooltip()
        @ColorPicker
        public int bracketColor = 12041720;
        @Tooltip()
        @ColorPicker
        public int tagColor = 13017334;
        @Tooltip()
        public String tagText = "DMM";
        @Tooltip()
        @ColorPicker
        public int textColor = 16047062;
        @Tooltip()
        @ColorPicker
        public int numericColor = 15961000;
        @Tooltip()
        @ColorPicker
        public int detailColor = 7106437;
        @Tooltip()
        @ColorPicker
        public int playerNameColor = 15703926;
        @Tooltip()
        @ColorPicker
        public int altTextColor = 11845374;
        @Tooltip()
        @ColorPicker
        public int errorColor = 15091027;
        @Tooltip()
        @ColorPicker
        public int warningColor = 14650909;
    }

    // ===== Crosshair HUD sub-configs (inside Features) =====

    public static class Crosshair {
        public enum DisplayMode {TEXT, ICONS}

        public enum IconCharStyle {BLOCKS, CIRCLES, SQUARES}

        public enum Alignment {LEFT, CENTER, RIGHT}

        public enum ShadowType {OUTLINE, VANILLA}

        @ConfigEntry.Gui.CollapsibleObject
        public CompactAbilities compactAbilities = new CompactAbilities();

        public static class CompactAbilities {
            @Tooltip()
            public boolean enabled = false;
            @ConfigEntry.Gui.CollapsibleObject
            public HudElementConfig hud = HudElementConfig.builder().build();
            @Tooltip()
            public List<CompactAbilityEntry> trackedAbilities = new ArrayList<>();
            @Tooltip()
            public boolean advancedMode = false;
            @Tooltip()
            public String advancedJsonPath = "configUtils/deepamonu/compact_abilities.json";
        }

        @ConfigEntry.Gui.CollapsibleObject
        public Multiload multiload = new Multiload();

        public static class Multiload {
            @Tooltip()
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

            @ConfigEntry.Gui.CollapsibleObject
            public Prediction prediction = new Prediction();
        }

        public static class Prediction {
            @Tooltip()
            public boolean enabled = true;
        }

        public static class DisplaySettings {
            @Tooltip()
            public boolean hideVanillaAmmo = false;
            @Tooltip()
            public boolean showTotal = false;
            @Tooltip()
            public DisplayMode ammoDisplayMode = DisplayMode.TEXT;
        }

        public static class IconSettings {
            @Tooltip()
            public IconCharStyle iconCharStyle = IconCharStyle.BLOCKS;
            @Tooltip()
            public String filledIconChar = "";
            @Tooltip()
            public String emptyIconChar = "\u25CB";
            @Tooltip()
            public ShadowType iconShadow = ShadowType.OUTLINE;
            @Tooltip()
            public boolean vertical = false;
            @Tooltip()
            public boolean hideEmptySlots = false;
        }

        public static class FireTimer {
            @Tooltip()
            public boolean enabled = false;
            @Tooltip()
            public boolean vertical = false;
            @Tooltip()
            public float duration = 0.5f;

            @ConfigEntry.Gui.CollapsibleObject
            public BarLayout bar = new BarLayout();

            @ConfigEntry.Gui.CollapsibleObject
            public BarColors barColors = new BarColors();
        }

        public static class BarLayout {
            @Tooltip()
            public int xOffset = 0;
            @Tooltip()
            public int yOffset = 33;
            @Tooltip()
            public int width = 32;
            @Tooltip()
            public int height = 4;
        }

        public static class BarColors {
            @Tooltip()
            @ColorPicker
            public int barColor = 0xFFFFFF;
            @Tooltip()
            @ColorPicker
            public int bgColor = 0x000000;
        }

        public static class AmmoColors {
            @Tooltip()
            @ColorPicker
            public int ammoFullColor = 0x55FF55;
            @Tooltip()
            @ColorPicker
            public int ammoMidColor = 0xFFAA00;
            @Tooltip()
            @ColorPicker
            public int ammoEmptyColor = 0xFF5555;
            @Tooltip()
            @ColorPicker
            public int ammoEmptySlotColor = 0x444444;
        }

        public static class Layout {
            @ConfigEntry.Gui.CollapsibleObject
            public HudElementConfig hud = HudElementConfig.builder().build();
            @Tooltip()
            public float textScale = 1.0f;
            @Tooltip()
            public boolean renderOverUI = false;

            public Layout(int defaultYOffset) {
                hud.positioning.yOffset = defaultYOffset;
            }
        }
    }

    // ===== Config registration =====

    public static ConfigHolder<ModConfig> register() {
        ConfigHolder<ModConfig> holder = AutoConfig.register(
                ModConfig.class, (config, clazz) -> new GsonConfigSerializer<>(config, clazz, ConfigHandlerHelper.GSON)
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
                field -> {
                    if (field.getType() != List.class) return false;
                    if (!(field.getGenericType() instanceof ParameterizedType pt)) return false;
                    java.lang.reflect.Type[] args = pt.getActualTypeArguments();
                    return args.length == 1 && args[0] == CompactAbilityEntry.class;
                }
        );

        registry.registerPredicateTransformer(new TextAlignmentTooltip(),
                field -> field.getName().equals("textAlignment")
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
