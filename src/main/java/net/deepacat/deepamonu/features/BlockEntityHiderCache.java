package net.deepacat.deepamonu.features;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Caches the result of the block entity hider's pattern matching.
 * <p>
 * The renderer previously ran {@link String#matches} (which compiles a fresh
 * {@link Pattern}) for every pattern, for every block entity, every frame, and
 * also did a linear registry lookup plus {@code toString()} allocations. This
 * class compiles the patterns once per configUtils change and memoizes the answer per
 * {@link BlockEntityType} / dimension.
 */
public final class BlockEntityHiderCache {

    private static final Logger LOGGER = LoggerFactory.getLogger("BlockEntityHider");

    private static final List<String> EMPTY_IDS = Collections.emptyList();
    private static final List<Entry> NO_ENTRIES = Collections.emptyList();

    private static boolean debug = false;

    private static List<String> blockIdConfig = EMPTY_IDS;
    private static List<String> dimensionConfig = EMPTY_IDS;
    private static List<Entry> blockMatchers = NO_ENTRIES;
    private static List<Entry> dimensionMatchers = NO_ENTRIES;

    private static final Map<BlockEntityType<?>, TypeInfo> TYPE_CACHE = new HashMap<>();
    private static final Map<ResourceLocation, Boolean> DIMENSION_CACHE = new HashMap<>();

    // Diagnostics only, so a given block entity type is reported once instead of every frame.
    private static final Set<BlockEntityType<?>> LOGGED_HIDES = new HashSet<>();
    private static final Set<BlockEntityType<?>> LOGGED_UNREGISTERED = new HashSet<>();
    private static int lastGateState = -1;
    private static int lastLoggedDistance = -1;

    private BlockEntityHiderCache() {
    }

    /** Diagnostics switch, mirrored from the mod's debug setting by the renderer. */
    public static void setDebug(boolean value) {
        debug = value;
    }

    /**
     * Recompiles the patterns only when the configured id lists actually change.
     * The comparison is effectively free in the steady state because the entries
     * are the same {@link String} instances; it is safe to call for every
     * rendered block entity.
     */
    public static void sync(List<String> blockIds, List<String> dimensionIds) {
        List<String> safeBlockIds = blockIds == null ? EMPTY_IDS : blockIds;
        List<String> safeDimensionIds = dimensionIds == null ? EMPTY_IDS : dimensionIds;

        if (Objects.equals(safeBlockIds, blockIdConfig) && Objects.equals(safeDimensionIds, dimensionConfig)) {
            return;
        }

        // Owned copies, so later in-place edits of the configUtils lists are detected.
        blockIdConfig = new ArrayList<>(safeBlockIds);
        dimensionConfig = new ArrayList<>(safeDimensionIds);
        blockMatchers = compile(blockIdConfig);
        dimensionMatchers = compile(dimensionConfig);
        TYPE_CACHE.clear();
        DIMENSION_CACHE.clear();
        LOGGED_HIDES.clear();
        LOGGED_UNREGISTERED.clear();

        if (debug) {
            LOGGER.debug("[BlockEntityHider] rebuilt filters: block ids={}, dimension ids={}",
                    blockIdConfig, dimensionConfig);
        }
    }

    public static boolean isRegistered(BlockEntityType<?> type) {
        return info(type).registered;
    }

    /** Whether the type's id matches the configured block entity id patterns. */
    public static boolean matches(BlockEntityType<?> type) {
        return info(type).matches;
    }

    public static boolean matchesDimension(ResourceLocation dimension) {
        Boolean cached = DIMENSION_CACHE.get(dimension);
        if (cached != null) return cached;

        boolean result = anyMatch(dimensionMatchers, dimension.toString());
        DIMENSION_CACHE.put(dimension, result);
        return result;
    }

    /** Logs the active hide switches whenever they change, making silent no-ops visible. */
    public static void logGateState(boolean enabled, boolean hideInAllDimensions, boolean hideInDimensionList, boolean hideAllBlocks, int distance) {
        int state = (enabled ? 1 : 0)
                | (hideInAllDimensions ? 2 : 0)
                | (hideInDimensionList ? 4 : 0)
                | (hideAllBlocks ? 8 : 0);
        if (state == lastGateState && distance == lastLoggedDistance) return;
        lastGateState = state;
        lastLoggedDistance = distance;

        // Logged at info because a switch change is a deliberate user action and
        // this is the main way to see why the hider may be doing nothing.
        LOGGER.info("[BlockEntityHider] enabled={}, hideInAllDimensions={}, hideInDimensionList={}, hideAllBlocks={}, distance={}",
                enabled, hideInAllDimensions, hideInDimensionList, hideAllBlocks, distance);

        if (enabled && !hideInAllDimensions && !hideInDimensionList) {
            LOGGER.warn("[BlockEntityHider] enabled, but neither hideInAllDimensions nor hideInDimensionList is on, so nothing will be hidden");
        }
        if (enabled && !hideAllBlocks && blockMatchers.isEmpty()) {
            LOGGER.warn("[BlockEntityHider] enabled, but hideAllBlocks is off and the block id list is empty, so nothing will be hidden");
        }
    }

    /** Reports the first hide of each type (debug only). */
    public static void logHide(BlockEntityType<?> type, BlockPos pos, int distanceSquared) {
        if (!debug) return;
        if (!LOGGED_HIDES.add(type)) return;

        TypeInfo info = info(type);
        LOGGER.debug("[BlockEntityHider] hiding {} at {} (dist^2={})", info.id, pos, distanceSquared);
    }

    /** Reports the first unregistered type, which can never be hidden. */
    public static void logUnregistered(BlockEntityType<?> type) {
        if (!debug) return;
        if (!LOGGED_UNREGISTERED.add(type)) return;

        LOGGER.debug("[BlockEntityHider] skipping {} because it has no registry id", type);
    }

    private static TypeInfo info(BlockEntityType<?> type) {
        TypeInfo cached = TYPE_CACHE.get(type);
        if (cached != null) return cached;

        // This reverse lookup is a linear registry scan, so it must not run per frame.
        ResourceLocation id = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type);
        boolean matches = id != null && anyMatch(blockMatchers, id.toString());
        TypeInfo info = new TypeInfo(id, id != null, matches);
        TYPE_CACHE.put(type, info);
        return info;
    }

    private static boolean anyMatch(List<Entry> entries, String value) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).matches(value)) return true;
        }
        return false;
    }

    private static List<Entry> compile(List<String> patterns) {
        if (patterns.isEmpty()) return NO_ENTRIES;

        List<Entry> entries = new ArrayList<>(patterns.size());
        for (String pattern : patterns) {
            if (pattern == null) continue;
            // /regex/ syntax; an invalid pattern keeps the old "never matches" behaviour.
            if (pattern.length() >= 2 && pattern.startsWith("/") && pattern.endsWith("/")) {
                try {
                    entries.add(new Entry(null, Pattern.compile(pattern.substring(1, pattern.length() - 1))));
                } catch (PatternSyntaxException e) {
                    if (debug) {
                        LOGGER.warn("[BlockEntityHider] ignoring invalid regex '{}'", pattern, e);
                    }
                }
            } else {
                entries.add(new Entry(pattern, null));
            }
        }
        return entries.isEmpty() ? NO_ENTRIES : entries;
    }

    private record TypeInfo(ResourceLocation id, boolean registered, boolean matches) {
    }

    private record Entry(String literal, Pattern pattern) {
        boolean matches(String value) {
            return pattern != null ? pattern.matcher(value).matches() : value.equals(literal);
        }
    }
}
