package net.deepacat.deepamonu.features.commands.debug;

import net.deepacat.deepamonu.DMMClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class DumpContainerItems {

    public static void run() {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof AbstractContainerScreen<?> containerScreen)) {
            DMMClient.LOGGER.info("[Debug] No container screen open.");
            return;
        }
        DMMClient.LOGGER.info("[Debug] === Dumping items in container: {} ===", containerScreen.getTitle().getString());
        int idx = 0;
        for (Slot slot : containerScreen.getMenu().slots) {
            if (slot.container == mc.player.getInventory()) continue;
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) continue;

            String name = stack.getHoverName().getString();
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.contains("display")) {
                CompoundTag display = tag.getCompound("display");
                if (display.contains("Lore")) {
                    net.minecraft.nbt.ListTag lore = display.getList("Lore", 8);
                    if (!lore.isEmpty()) {
                        DMMClient.LOGGER.info("[Debug] Slot {}: '{}'", idx, name);
                        for (int i = 0; i < lore.size(); i++) {
                            String line = Component.Serializer.fromJson(lore.getString(i)).getString();
                            DMMClient.LOGGER.info("[Debug]   Lore[{}]: '{}'", i, line);
                        }
                    }
                }
            }
            idx++;
        }
        DMMClient.LOGGER.info("[Debug] === End of container dump ===");
    }
}
