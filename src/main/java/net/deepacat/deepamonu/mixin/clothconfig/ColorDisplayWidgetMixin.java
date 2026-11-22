package net.deepacat.deepamonu.mixin.clothconfig;

import me.shedaniel.clothconfig2.gui.widget.ColorDisplayWidget;
import net.deepacat.deepamonu.compat.clothconfig.ColorPickerScreen;
import net.deepacat.deepamonu.compat.clothconfig.DMMColorDisplay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ColorDisplayWidget.class)
public abstract class ColorDisplayWidgetMixin implements DMMColorDisplay {
    @Shadow(remap = false)
    protected EditBox textFieldWidget;

    @Shadow(remap = false)
    protected int color;

    @Unique
    private boolean dmm$alphaAllowed = true;

    @Inject(method = "onClick", at = @At("HEAD"))
    private void dmm$openColorPicker(double mouseX, double mouseY, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        Screen currentScreen = minecraft.screen;
        if (currentScreen instanceof ColorPickerScreen) return;

        boolean alpha = dmm$alphaAllowed;
        ColorDisplayWidget widget = (ColorDisplayWidget) (Object) this;
        ColorPickerScreen.open(currentScreen, color, alpha, newColor -> {
            String hex = alpha
                    ? String.format("#%08X", newColor)
                    : String.format("#%06X", newColor & 0xFFFFFF);
            textFieldWidget.setValue(hex);
        }, widget.getX(), widget.getY());
    }

    @Override
    public void dmm$setAlphaAllowed(boolean alphaAllowed) {
        this.dmm$alphaAllowed = alphaAllowed;
    }

    @Override
    public boolean dmm$isAlphaAllowed() {
        return dmm$alphaAllowed;
    }
}
