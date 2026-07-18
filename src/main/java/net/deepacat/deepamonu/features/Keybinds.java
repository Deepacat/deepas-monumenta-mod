package net.deepacat.deepamonu.features;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import com.mojang.blaze3d.platform.InputConstants.Type;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Keybinds {
    private static final Logger LOGGER = LoggerFactory.getLogger("DeepamonuKeybinds");

    private static final KeyMapping debugDumpItems = new KeyMapping(
            "key.deepamonu.debugDumpItems",
            Type.KEYSYM,
            GLFW.GLFW_KEY_F12,
            "category.deepamonu"
    );
    private static final KeyMapping debugDumpAbilities = new KeyMapping(
            "key.deepamonu.debugDumpAbilities",
            Type.KEYSYM,
            GLFW.GLFW_KEY_F11,
            "category.deepamonu"
    );

    public static void init() {
        KeyBindingHelper.registerKeyBinding(debugDumpItems);
        KeyBindingHelper.registerKeyBinding(debugDumpAbilities);

        ScreenEvents.BEFORE_INIT.register(Keybinds::onBeforeInit);
    }

    private static void onBeforeInit(Minecraft client, Screen screen, int scaledWidth, int scaledHeight) {
        ScreenKeyboardEvents.beforeKeyPress(screen).register(Keybinds::onKeyPress);
    }

    private static boolean onKeyPress(Screen screen, int key, int scancode, int modifiers) {
        if (key == debugDumpItems.key.getValue()) {
            dumpContainerItems();
            return true;
        }
        if (key == debugDumpAbilities.key.getValue()) {
            dumpAbilityData();
            return true;
        }
        return false;
    }

    public static void tick() { }

    private static void dumpContainerItems() {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof AbstractContainerScreen<?> containerScreen)) {
            LOGGER.info("No container screen open.");
            return;
        }
        LOGGER.info("=== Dumping items in container: {} ===", containerScreen.getTitle().getString());
        int idx = 0;
        for (Slot slot : containerScreen.getMenu().slots) {
            if (slot.container == mc.player.getInventory()) continue;
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) continue;

            String name = stack.getHoverName().getString();
            // Only log if the item has any lore
            CompoundTag tag = stack.getTag();
            boolean hasLore = false;
            if (tag != null && tag.contains("display")) {
                CompoundTag display = tag.getCompound("display");
                if (display.contains("Lore")) {
                    net.minecraft.nbt.ListTag lore = display.getList("Lore", 8);
                    if (!lore.isEmpty()) {
                        hasLore = true;
                        LOGGER.info("Slot {}: '{}'", idx, name);
                        for (int i = 0; i < lore.size(); i++) {
                            String line = Component.Serializer.fromJson(lore.getString(i)).getString();
                            LOGGER.info("  Lore[{}]: '{}'", i, line);
                        }
                    }
                }
            }
            // If there's no lore, we can still log the name if it's a non‑empty slot, but the request was to skip empty lore.
            // To keep it clean, we log only items with lore as before. Uncomment the following to log even items with no lore:
            // else { LOGGER.info("Slot {}: '{}' (no lore)", idx, name); }
            idx++;
        }
        LOGGER.info("=== End of container dump ===");
    }

    private static void dumpAbilityData() {
        var handler = UnofficialMonumentaModClient.abilityHandler;
        var list = handler.abilityData;
        if (list.isEmpty()) {
            LOGGER.info("No ability data available.");
            return;
        }
        LOGGER.info("=== Dumping ability data ===");
        for (var info : list) {
            LOGGER.info("Name: {}, Class: {}, Cooldown: {}, Charges: {}/{}",
                    info.name, info.className,
                    info.remainingCooldown, info.charges, info.maxCharges);
        }
        LOGGER.info("=== End of ability data dump ===");
    }
}