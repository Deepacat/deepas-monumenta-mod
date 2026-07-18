package net.deepacat.deepamonu.utils;

import com.mojang.blaze3d.systems.RenderSystem;

import net.deepacat.deepamonu.DMMClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public enum GuiTextures implements ScreenElement {
    // JEI
    JEI_SLOT("widgets", 18, 18),
    JEI_CHANCE_SLOT("widgets", 20, 156, 18, 18),
    JEI_CATALYST_SLOT("widgets", 0, 156, 18, 18),
    JEI_ARROW("widgets", 19, 10, 42, 10),
    JEI_LONG_ARROW("widgets", 19, 0, 71, 10),
    JEI_DOWN_ARROW("widgets", 0, 21, 18, 14),
    JEI_LIGHT("widgets", 0, 42, 52, 11),
    JEI_QUESTION_MARK("widgets", 0, 178, 12, 16),
    JEI_SHADOW("widgets", 0, 56, 52, 11),
    BLOCKZAPPER_UPGRADE_RECIPE("widgets", 0, 75, 144, 66),
    INFO("widgets", 240, 0, 16, 16),
    LEFT_CLICK("widgets", 192, 0, 16, 16),
    RIGHT_CLICK("widgets", 224, 0, 16, 16);

    public final ResourceLocation location;
    public int width, height;
    public int startX, startY;

    GuiTextures(String location, int width, int height) {
        this(location, 0, 0, width, height);
    }

    GuiTextures(ResourceLocation location, int width, int height) {
        this(location, 0, 0, width, height);
    }

    GuiTextures(int startX, int startY) {
        this("icons", startX * 16, startY * 16, 16, 16);
    }

    GuiTextures(String location, int startX, int startY, int width, int height) {
        this(DMMClient.MOD_ID, location, startX, startY, width, height);
    }

    GuiTextures(String namespace, String location, int startX, int startY, int width, int height) {
        this.location = new ResourceLocation(namespace, "textures/gui/" + location + ".png");
        this.width = width;
        this.height = height;
        this.startX = startX;
        this.startY = startY;
    }

    GuiTextures(ResourceLocation location, int startX, int startY, int width, int height) {
        this.location = location;
        this.width = width;
        this.height = height;
        this.startX = startX;
        this.startY = startY;
    }

    public void bind() {
        RenderSystem.setShaderTexture(0, location);
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y) {
        graphics.blit(location, x, y, startX, startY, width, height);
    }
}
