package net.deepacat.deepamonu.features;

import com.mojang.blaze3d.platform.InputConstants;
import net.deepacat.deepamonu.DMMClient;
import net.deepacat.deepamonu.config.ModConfig;
import net.deepacat.deepamonu.features.commands.debug.DumpAbilities;
import net.deepacat.deepamonu.features.commands.debug.DumpChatMessage;
import net.deepacat.deepamonu.features.commands.debug.DumpContainerItems;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class Keybinds {
    private static final KeyMapping debugDumpItems = new KeyMapping("key.deepamonu.debugDumpItems",
            InputConstants.Type.KEYSYM, -1, "category.deepamonu");
    private static final KeyMapping debugDumpAbilities = new KeyMapping("key.deepamonu.debugDumpAbilities",
            InputConstants.Type.KEYSYM, -1, "category.deepamonu");
    private static final KeyMapping debugDumpChat = new KeyMapping("key.deepamonu.debugDumpChat",
            InputConstants.Type.KEYSYM, -1, "category.deepamonu");
    private static final KeyMapping toggleHideAllBlocks = new KeyMapping("key.deepamonu.toggleHideAllBlocks",
            InputConstants.Type.KEYSYM, -1, "category.deepamonu");
    private static final KeyMapping toggleHideBlocksDimensionList = new KeyMapping("key.deepamonu.toggleHideBlocksDimensionList",
            InputConstants.Type.KEYSYM, -1, "category.deepamonu");
    private static final KeyMapping toggleHideBlocksAllDimensions = new KeyMapping("key.deepamonu.toggleHideBlocksAllDimensions",
            InputConstants.Type.KEYSYM, -1, "category.deepamonu");

    private static boolean wasDumpItemsDown = false;
    private static boolean wasDumpAbilitiesDown = false;
    private static boolean wasDumpChatDown = false;
    private static boolean wasToggleHideAllBlocksDown = false;
    private static boolean wasToggleHideBlocksDimensionListDown = false;
    private static boolean wasToggleHideBlocksAllDimensionsDown = false;

    public static void init() {
        KeyBindingHelper.registerKeyBinding(debugDumpItems);
        KeyBindingHelper.registerKeyBinding(debugDumpAbilities);
        KeyBindingHelper.registerKeyBinding(debugDumpChat);
        KeyBindingHelper.registerKeyBinding(toggleHideAllBlocks);
        KeyBindingHelper.registerKeyBinding(toggleHideBlocksDimensionList);
        KeyBindingHelper.registerKeyBinding(toggleHideBlocksAllDimensions);
    }

    public static void tick() {
        ModConfig config = DMMClient.config();

        boolean dumpItemsDown = debugDumpItems.isDown();
        if (dumpItemsDown && !wasDumpItemsDown) {
            DumpContainerItems.run();
        }
        wasDumpItemsDown = dumpItemsDown;

        boolean dumpAbilitiesDown = debugDumpAbilities.isDown();
        if (dumpAbilitiesDown && !wasDumpAbilitiesDown) {
            DumpAbilities.run();
        }
        wasDumpAbilitiesDown = dumpAbilitiesDown;

        boolean dumpChatDown = debugDumpChat.isDown();
        if (dumpChatDown && !wasDumpChatDown) {
            DumpChatMessage.dumpHovered();
        }
        wasDumpChatDown = dumpChatDown;

        boolean toggleHideAllBlocksDown = toggleHideAllBlocks.isDown();
        if (toggleHideAllBlocksDown && !wasToggleHideAllBlocksDown) {
            config.features.blockEntityHider.hideAllBlocks = !config.features.blockEntityHider.hideAllBlocks;
            DMMClient.CONFIG.save();
        }
        wasToggleHideAllBlocksDown = toggleHideAllBlocksDown;

        boolean toggleHideBlocksDimensionListDown = toggleHideBlocksDimensionList.isDown();
        if (toggleHideBlocksDimensionListDown && !wasToggleHideBlocksDimensionListDown) {
            config.features.blockEntityHider.hideInDimensionList = !config.features.blockEntityHider.hideInDimensionList;
            DMMClient.CONFIG.save();
        }
        wasToggleHideBlocksDimensionListDown = toggleHideBlocksDimensionListDown;

        boolean toggleHideBlocksAllDimensionsDown = toggleHideBlocksAllDimensions.isDown();
        if (toggleHideBlocksAllDimensionsDown && !wasToggleHideBlocksAllDimensionsDown) {
            config.features.blockEntityHider.hideInAllDimensions = !config.features.blockEntityHider.hideInAllDimensions;
            DMMClient.CONFIG.save();
        }
        wasToggleHideBlocksAllDimensionsDown = toggleHideBlocksAllDimensionsDown;
    }
}
