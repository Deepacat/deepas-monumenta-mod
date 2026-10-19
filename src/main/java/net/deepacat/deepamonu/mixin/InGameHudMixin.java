package net.deepacat.deepamonu.mixin;

import net.deepacat.deepamonu.hud.elements.GuardSituText;
import net.deepacat.deepamonu.hud.elements.ShieldBreakText;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class InGameHudMixin {
    @Inject(
            method = {"render"}, at = {@At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Gui;renderEffects(Lnet/minecraft/client/gui/GuiGraphics;)V",
            shift = At.Shift.BEFORE
    )})
    void renderBeforeEffects(GuiGraphics drawContext, float tickDelta, CallbackInfo ci) {
        ShieldBreakText.INSTANCE.renderAbsolute(drawContext, tickDelta);
        GuardSituText.INSTANCE.renderAbsolute(drawContext, tickDelta);
    }
}
