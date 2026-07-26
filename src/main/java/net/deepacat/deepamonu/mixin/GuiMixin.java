package net.deepacat.deepamonu.mixin;

import net.deepacat.deepamonu.DMMClient;
import net.deepacat.deepamonu.config.ModConfig;
import net.deepacat.deepamonu.hud.ActionBarState;
import net.deepacat.deepamonu.hud.CrosshairHud;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {

    private static final Logger LOGGER = LoggerFactory.getLogger("CrosshairHud");

    @Inject(method = "setOverlayMessage", at = @At("HEAD"), cancellable = true)
    private void onSetOverlayMessage(Component component, boolean animateColor, CallbackInfo ci) {
        if (component == null) return;

        String plainText = component.getString();
        if (plainText == null || plainText.isEmpty()) return;

        LOGGER.debug("[ActionBar] (animateColor={}): {}", animateColor, plainText);

        ActionBarState.handleActionBar(plainText);

        ModConfig config = DMMClient.config();
        if (config != null && config.features.crosshair.multiload.enabled && config.features.crosshair.multiload.display.hideVanillaAmmo) {
            if (ActionBarState.isAmmoMessage(plainText)) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderTail(GuiGraphics graphics, float tickDelta, CallbackInfo ci) {
        CrosshairHud.onRenderOverUI(graphics, tickDelta);
    }
}
