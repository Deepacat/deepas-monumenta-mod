package net.deepacat.deepamonu.features;

import com.mojang.blaze3d.platform.InputConstants.Type;
import net.deepacat.deepamonu.features.commands.debug.DumpAbilities;
import net.deepacat.deepamonu.features.commands.debug.DumpChatMessage;
import net.deepacat.deepamonu.features.commands.debug.DumpContainerItems;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class Keybinds {

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
    private static final KeyMapping debugDumpChat = new KeyMapping(
            "key.deepamonu.debugDumpChat",
            Type.KEYSYM,
            GLFW.GLFW_KEY_F10,
            "category.deepamonu"
    );

    private static boolean wasDumpItemsDown = false;
    private static boolean wasDumpAbilitiesDown = false;
    private static boolean wasDumpChatDown = false;

    public static void init() {
        KeyBindingHelper.registerKeyBinding(debugDumpItems);
        KeyBindingHelper.registerKeyBinding(debugDumpAbilities);
        KeyBindingHelper.registerKeyBinding(debugDumpChat);
    }

    public static void tick() {
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
    }
}
